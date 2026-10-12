package com.udea.rutaudea.ui.screen.progreso

import com.udea.rutaudea.data.repository.ProgressRepository
import com.udea.rutaudea.domain.model.Racha
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Verifica que el dashboard de Progreso expone la racha vigente junto al
 * resto de métricas, leyéndola del repositorio offline-first.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProgresoRachaTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `el estado incluye la racha vigente del usuario`() = runTest {
        val racha = Racha(actual = 3, mejor = 7, ultimoDia = 20_000L, ultimaActividadMs = 1L)
        val repository = mockk<ProgressRepository>(relaxed = true) {
            coEvery { cargarSimulacros(any()) } returns emptyList()
            coEvery { cargarPracticas(any()) } returns emptyList()
            coEvery { cargarEstadisticasPorSubtema(any(), any()) } returns emptyList()
            coEvery { cargarRacha() } returns racha
            coEvery { sincronizar() } returns true
        }

        val vm = ProgresoViewModel(repository)

        assertEquals(3, vm.uiState.value.racha.actual)
        assertEquals(7, vm.uiState.value.racha.mejor)
    }

    @Test
    fun `sin actividad la racha del estado queda en cero`() = runTest {
        val repository = mockk<ProgressRepository>(relaxed = true) {
            coEvery { cargarSimulacros(any()) } returns emptyList()
            coEvery { cargarPracticas(any()) } returns emptyList()
            coEvery { cargarEstadisticasPorSubtema(any(), any()) } returns emptyList()
            coEvery { cargarRacha() } returns Racha()
            coEvery { sincronizar() } returns true
        }

        val vm = ProgresoViewModel(repository)

        assertEquals(0, vm.uiState.value.racha.actual)
    }
}
