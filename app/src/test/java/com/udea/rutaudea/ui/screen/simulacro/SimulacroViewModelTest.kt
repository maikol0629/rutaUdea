package com.udea.rutaudea.ui.screen.simulacro

import androidx.lifecycle.SavedStateHandle
import com.google.gson.Gson
import com.udea.rutaudea.data.repository.QuestionRepository
import com.udea.rutaudea.data.repository.SimulationRepository
import com.udea.rutaudea.domain.model.Question
import com.udea.rutaudea.domain.model.SimulacroConfig
import com.udea.rutaudea.domain.services.QuestionSelector
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests de la pausa/persistencia del simulacro al cambiar de app:
 * overlay al volver, resumir, botón atrás y finalización por tiempo.
 *
 * `relojActivo = false` desactiva el bucle infinito del cronómetro.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SimulacroViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun pregunta(id: String, correcta: String = "A") = Question(
        id = id,
        area = "razonamiento_logico",
        subtema = "RL07 Sucesiones",
        dificultad = "baja",
        pregunta = "¿$id?",
        opciones = listOf("op A", "op B", "op C", "op D"),
        respuestaCorrecta = correcta,
        explicacion = "explicación $id"
    )

    private fun sahConSeleccion(ids: List<String>) =
        SavedStateHandle(mapOf("selection_ids_json" to Gson().toJson(ids)))

    private fun viewModel(
        preguntas: List<Question>,
        sah: SavedStateHandle
    ): SimulacroViewModel {
        val repository = mockk<QuestionRepository> {
            coEvery { getById(any()) } answers {
                val id = firstArg<String>()
                preguntas.firstOrNull { it.id == id }
            }
            coEvery { leerConfiguracionSimulacro() } returns SimulacroConfig()
        }
        return SimulacroViewModel(
            selector = mockk<QuestionSelector>(relaxed = true),
            repository = repository,
            simulationRepository = mockk<SimulationRepository>(relaxed = true),
            savedStateHandle = sah,
            relojActivo = false
        )
    }

    @Test
    fun `al cambiar de app y volver se muestra el overlay de pausa`() = runTest {
        val vm = viewModel(
            listOf(pregunta("q1"), pregunta("q2")),
            sahConSeleccion(listOf("q1", "q2"))
        )

        assertFalse(vm.uiState.value.showResumeOverlay)

        vm.onAppBackgrounded()
        assertFalse(vm.uiState.value.showResumeOverlay) // aún no vuelve

        vm.onAppResumed()
        assertTrue(vm.uiState.value.showResumeOverlay)
    }

    @Test
    fun `sin background previo no se muestra la pausa`() = runTest {
        val vm = viewModel(listOf(pregunta("q1")), sahConSeleccion(listOf("q1")))

        vm.onAppResumed()

        assertFalse(vm.uiState.value.showResumeOverlay)
    }

    @Test
    fun `el boton atras abre la pausa y resumir la oculta`() = runTest {
        val vm = viewModel(listOf(pregunta("q1")), sahConSeleccion(listOf("q1")))

        vm.onPauseRequested()
        assertTrue(vm.uiState.value.showResumeOverlay)

        vm.onResumeConfirmed()
        assertFalse(vm.uiState.value.showResumeOverlay)
    }

    @Test
    fun `al abandonar se oculta la pausa sin finalizar el simulacro`() = runTest {
        val vm = viewModel(
            listOf(pregunta("q1"), pregunta("q2")),
            sahConSeleccion(listOf("q1", "q2"))
        )

        vm.onAppBackgrounded()
        vm.onAppResumed()
        assertTrue(vm.uiState.value.showResumeOverlay)

        vm.onAbandonar()

        assertFalse(vm.uiState.value.showResumeOverlay)
        assertFalse(vm.uiState.value.isFinished)
    }

    @Test
    fun `un simulacro finalizado no muestra la pausa`() = runTest {
        val vm = viewModel(
            listOf(pregunta("q1"), pregunta("q2")),
            sahConSeleccion(listOf("q1", "q2"))
        )

        vm.onOptionSelected("A")
        vm.onNextOrFinish()
        vm.onOptionSelected("A")
        vm.onNextOrFinish() // última → finaliza

        assertTrue(vm.uiState.value.isFinished)

        vm.onAppBackgrounded()
        vm.onAppResumed()
        assertFalse(vm.uiState.value.showResumeOverlay)
    }

    @Test
    fun `si el tiempo se agoto durante la muerte del proceso se finaliza`() = runTest {
        val sah = SavedStateHandle(
            mapOf(
                "selection_ids_json" to Gson().toJson(listOf("q1", "q2")),
                // Arrancó hace 121 minutos (> 120 del límite).
                "start_time_ms" to (System.currentTimeMillis() - 121L * 60 * 1000)
            )
        )
        val vm = viewModel(listOf(pregunta("q1"), pregunta("q2")), sah)

        assertTrue(vm.uiState.value.isFinished)
        assertNotNull(sah.get<Int>("simulation_score"))
    }
}
