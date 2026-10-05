package com.udea.rutaudea.ui.screen.progreso

import com.udea.rutaudea.data.repository.ProgressRepository
import com.udea.rutaudea.domain.model.PracticaResumen
import com.udea.rutaudea.domain.model.SimulacroResumen
import com.udea.rutaudea.domain.model.SubtemaEstadistica
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests del ViewModel de Progreso: lee de la cache local, dispara la
 * sincronización con Firestore, y calcula analítica (evolución,
 * fortalezas, recomendaciones e historial).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProgresoViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun simulacro(id: String, fechaMs: Long, score: Int = 60, total: Int = 80) =
        SimulacroResumen(
            id = id, fechaMs = fechaMs, score = score, total = total,
            tiempoUsedMs = 1000L, respondidas = total, omitidas = 0
        )

    private fun practica(id: String, fechaMs: Long, score: Int = 8, total: Int = 10) =
        PracticaResumen(
            id = id, fechaMs = fechaMs, area = "razonamiento_logico",
            subtema = "RL07 Sucesiones", score = score, total = total, tiempoUsedMs = 500L
        )

    @Test
    fun `carga el historial local y calcula analitica`() = runTest {
        val simulacros = listOf(simulacro("s1", 1000L, 40, 80), simulacro("s2", 2000L, 60, 80))
        val practicas = listOf(practica("p1", 1500L))
        val stats = listOf(
            SubtemaEstadistica("RL07 Sucesiones", respondidas = 5, correctas = 2),
            SubtemaEstadistica("RL02 Porcentajes", respondidas = 2, correctas = 2)
        )
        val repository = mockk<ProgressRepository>(relaxed = true) {
            coEvery { cargarSimulacros(any()) } returns simulacros
            coEvery { cargarPracticas(any()) } returns practicas
            coEvery { cargarEstadisticasPorSubtema(any(), any()) } returns stats
            coEvery { sincronizar() } returns true
        }

        val vm = ProgresoViewModel(repository)

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.sinDatos)
        assertEquals(2, state.simulacros.size)
        assertEquals(1, state.practicas.size)
        assertEquals(3, state.historial.size)
        assertEquals(listOf(50, 75), state.evolucion)
        assertEquals(62, state.promedioGlobal)
        assertEquals(75, state.mejorPuntaje)
        assertEquals(1, state.areasDeMejora.size)
        assertEquals("RL07 Sucesiones", state.areasDeMejora.first().subtema)
        assertEquals(1, state.recomendaciones.size)
        assertTrue(state.recomendacionesTexto.first().contains("RL07 Sucesiones"))
    }

    @Test
    fun `sin actividad local el estado queda vacio`() = runTest {
        val repository = mockk<ProgressRepository>(relaxed = true) {
            coEvery { cargarSimulacros(any()) } returns emptyList()
            coEvery { cargarPracticas(any()) } returns emptyList()
            coEvery { cargarEstadisticasPorSubtema(any(), any()) } returns emptyList()
            coEvery { sincronizar() } returns true
        }

        val vm = ProgresoViewModel(repository)

        assertTrue(vm.uiState.value.sinDatos)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `refrescar relanza la sincronizacion con la nube`() = runTest {
        val repository = mockk<ProgressRepository>(relaxed = true) {
            coEvery { cargarSimulacros(any()) } returns emptyList()
            coEvery { cargarPracticas(any()) } returns emptyList()
            coEvery { cargarEstadisticasPorSubtema(any(), any()) } returns emptyList()
            coEvery { sincronizar() } returns true
        }

        val vm = ProgresoViewModel(repository)
        vm.refrescar()

        // init (1) + refrescar (1)
        coVerify(exactly = 2) { repository.sincronizar() }
        assertFalse(vm.uiState.value.isRefreshing)
    }
}
