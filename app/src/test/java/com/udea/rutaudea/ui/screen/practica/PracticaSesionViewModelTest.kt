package com.udea.rutaudea.ui.screen.practica

import androidx.lifecycle.SavedStateHandle
import com.udea.rutaudea.domain.model.Question
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests de la sesión de práctica: feedback inmediato al responder,
 * bloqueo de la respuesta revelada, navegación y finalización
 * (los resultados quedan en el SavedStateHandle para la pantalla
 * de resultado).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PracticaSesionViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun pregunta(id: String, respuestaCorrecta: String = "A") = Question(
        id = id,
        area = "razonamiento_logico",
        subtema = "Series",
        dificultad = "baja",
        pregunta = "¿$id?",
        opciones = listOf("op A", "op B", "op C", "op D"),
        respuestaCorrecta = respuestaCorrecta,
        explicacion = "explicación $id"
    )

    private fun viewModel(
        preguntas: List<Question>,
        savedStateHandle: SavedStateHandle = SavedStateHandle()
    ) = PracticaSesionViewModel(
        preguntas, "razonamiento_logico", "Series", savedStateHandle,
        relojActivo = false // el ticker es un bucle infinito: desactivado en tests
    )

    @Test
    fun `responder revela el feedback inmediato`() = runTest {
        val vm = viewModel(listOf(pregunta("q1", respuestaCorrecta = "B")))

        assertFalse(vm.uiState.value.isRevealed)
        vm.onOptionSelected("B")

        assertTrue(vm.uiState.value.isRevealed)
        assertEquals("B", vm.uiState.value.currentAnswer)
        assertTrue(vm.uiState.value.isCorrect)
    }

    @Test
    fun `respuesta incorrecta marca feedback negativo`() = runTest {
        val vm = viewModel(listOf(pregunta("q1", respuestaCorrecta = "B")))

        vm.onOptionSelected("A")

        assertTrue(vm.uiState.value.isRevealed)
        assertFalse(vm.uiState.value.isCorrect)
    }

    @Test
    fun `la respuesta revelada queda bloqueada`() = runTest {
        val vm = viewModel(listOf(pregunta("q1", respuestaCorrecta = "B")))

        vm.onOptionSelected("B")
        vm.onOptionSelected("D") // intento de cambio

        assertEquals("B", vm.uiState.value.currentAnswer)
        assertEquals(1, vm.uiState.value.revealed.size)
    }

    @Test
    fun `navegacion siguiente y anterior`() = runTest {
        val vm = viewModel(listOf(pregunta("q1"), pregunta("q2"), pregunta("q3")))

        vm.onOptionSelected("A")
        vm.onNext()
        assertEquals(1, vm.uiState.value.currentIndex)

        vm.onPrevious()
        assertEquals(0, vm.uiState.value.currentIndex)

        vm.onPrevious() // ya está en la primera: no baja de 0
        assertEquals(0, vm.uiState.value.currentIndex)
    }

    @Test
    fun `finalizar escribe los resultados en el SavedStateHandle`() = runTest {
        val preguntas = listOf(
            pregunta("q1", respuestaCorrecta = "A"),
            pregunta("q2", respuestaCorrecta = "B")
        )
        val sah = SavedStateHandle()
        val vm = viewModel(preguntas, sah)

        var terminada = false
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.evento.collect { terminada = it is PracticaSesionViewModel.SesionEvento.PracticaTerminada }
        }

        vm.onOptionSelected("A") // correcta
        vm.onNext()
        vm.onOptionSelected("A") // incorrecta (correcta = B)
        vm.onNext() // última pregunta → finaliza

        assertTrue(terminada)
        assertEquals(1, sah.get<Int>(PracticaSesionViewModel.SCORE_KEY))
        assertEquals(2, sah.get<Int>(PracticaSesionViewModel.TOTAL_KEY))
        val preguntasJson = sah.get<String>(PracticaSesionViewModel.QUESTIONS_JSON_KEY).orEmpty()
        assertTrue(preguntasJson.contains("\"q1\""))
        assertTrue(preguntasJson.contains("\"q2\""))
        val respuestasJson = sah.get<String>(PracticaSesionViewModel.ANSWERS_JSON_KEY).orEmpty()
        assertTrue(respuestasJson.contains("\"A\""))
    }

    @Test
    fun `terminar antes de responder deja omitidas`() = runTest {
        val vm = viewModel(listOf(pregunta("q1"), pregunta("q2")))
        val state = vm.uiState.value

        assertEquals(0, state.respondidas)
        assertEquals(0, state.score)
        assertEquals(2, state.questions.size)
    }

    @Test
    fun `restaura el progreso tras rotacion`() = runTest {
        val sah = SavedStateHandle(
            mapOf(
                "practica_answers" to mapOf(0 to "A"),
                "practica_revealed" to listOf(0),
                "practica_current_index" to 1
            )
        )
        val vm = viewModel(listOf(pregunta("q1"), pregunta("q2")), sah)

        val state = vm.uiState.value
        assertEquals(1, state.currentIndex)
        assertEquals("A", state.answers[0])
        assertEquals(setOf(0), state.revealed)
        // La pregunta actual (2) sigue sin revelar.
        assertFalse(state.isRevealed)
        assertNull(state.currentAnswer)
    }
}
