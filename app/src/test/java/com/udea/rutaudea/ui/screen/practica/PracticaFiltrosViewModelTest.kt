package com.udea.rutaudea.ui.screen.practica

import androidx.lifecycle.SavedStateHandle
import com.udea.rutaudea.data.repository.QuestionRepository
import com.udea.rutaudea.domain.model.Question
import com.udea.rutaudea.domain.services.PracticeSelector
import io.mockk.coEvery
import io.mockk.mockk
import kotlin.random.Random
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests del ViewModel de filtros de práctica: carga de subtemas por
 * área, conteo de disponibles y arranque de la sesión (selección
 * estratificada escrita en el SavedStateHandle).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PracticaFiltrosViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun pregunta(id: String, dificultad: String = "baja") = Question(
        id = id,
        area = "razonamiento_logico",
        subtema = "Series",
        dificultad = dificultad,
        pregunta = "¿$id?",
        opciones = listOf("a", "b", "c", "d"),
        respuestaCorrecta = "A"
    )

    @Test
    fun `carga subtemas y disponibles del area por defecto`() = runTest {
        val repository = mockk<QuestionRepository> {
            coEvery { getDistinctSubtemas("razonamiento_logico") } returns listOf("Series", "Porcentajes")
            coEvery { countByFilters("razonamiento_logico", null) } returns 25
        }
        val vm = PracticaFiltrosViewModel(repository, PracticeSelector(Random(42)), SavedStateHandle())

        val state = vm.uiState.value
        assertEquals(listOf("Series", "Porcentajes"), state.subtemas)
        assertEquals(25, state.disponibles)
        assertTrue(state.puedeIniciar)
    }

    @Test
    fun `cambiar de area recarga subtemas y limpia el subtema`() = runTest {
        val repository = mockk<QuestionRepository> {
            coEvery { getDistinctSubtemas("razonamiento_logico") } returns listOf("Series")
            coEvery { getDistinctSubtemas("competencia_lectora") } returns listOf("CL02 Inferencia")
            coEvery { countByFilters(any(), any()) } returns 10
        }
        val vm = PracticaFiltrosViewModel(repository, PracticeSelector(Random(42)), SavedStateHandle())

        vm.onSubtemaChange("Series")
        assertEquals("Series", vm.uiState.value.subtema)

        vm.onAreaChange("competencia_lectora")

        val state = vm.uiState.value
        assertEquals("competencia_lectora", state.area)
        assertEquals(listOf("CL02 Inferencia"), state.subtemas)
        assertEquals(null, state.subtema)
    }

    @Test
    fun `sin preguntas disponibles no permite iniciar`() = runTest {
        val repository = mockk<QuestionRepository> {
            coEvery { getDistinctSubtemas(any()) } returns emptyList()
            coEvery { countByFilters(any(), any()) } returns 0
        }
        val vm = PracticaFiltrosViewModel(repository, PracticeSelector(Random(42)), SavedStateHandle())

        assertFalse(vm.uiState.value.puedeIniciar)
    }

    @Test
    fun `iniciar escribe la seleccion estratificada en el SavedStateHandle`() = runTest {
        val pool = (1..30).map { pregunta("q$it", if (it % 2 == 0) "media" else "baja") }
        val repository = mockk<QuestionRepository> {
            coEvery { getDistinctSubtemas(any()) } returns listOf("Series")
            coEvery { countByFilters(any(), any()) } returns 30
            coEvery { getQuestions(any(), any(), any(), any()) } returns pool
        }
        val sah = SavedStateHandle()
        val vm = PracticaFiltrosViewModel(repository, PracticeSelector(Random(42)), sah)

        var lista = false
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.evento.collect {
                lista = it is PracticaFiltrosViewModel.FiltrosEvento.PracticaLista
            }
        }

        vm.onCantidadChange(10)
        vm.iniciarPractica()

        assertTrue(lista)
        val preguntasJson = sah.get<String>(PracticaFiltrosViewModel.PREGUNTAS_KEY)
        assertNotNull(preguntasJson)
        assertTrue(preguntasJson!!.contains("\"q1\""))
        assertEquals("razonamiento_logico", sah.get<String>(PracticaFiltrosViewModel.AREA_KEY))
        assertFalse(vm.uiState.value.isStarting)
    }

    @Test
    fun `pool vacio al iniciar muestra error y no navega`() = runTest {
        val repository = mockk<QuestionRepository> {
            coEvery { getDistinctSubtemas(any()) } returns listOf("Series")
            coEvery { countByFilters(any(), any()) } returns 30
            coEvery { getQuestions(any(), any(), any(), any()) } returns emptyList()
        }
        val vm = PracticaFiltrosViewModel(repository, PracticeSelector(Random(42)), SavedStateHandle())

        vm.iniciarPractica()

        val state = vm.uiState.value
        assertNotNull(state.error)
        assertFalse(state.isStarting)
    }
}
