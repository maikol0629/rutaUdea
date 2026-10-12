package com.udea.rutaudea.ui.screen.home

import com.udea.rutaudea.data.repository.RachaRepository
import com.udea.rutaudea.domain.model.Racha
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
import org.junit.Before
import org.junit.Test

/**
 * Tests del ViewModel del Home: expone la racha vigente leída de la
 * cache local y la recarga al volver a la pantalla.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `carga la racha vigente al iniciar`() = runTest {
        val racha = Racha(actual = 4, mejor = 9, ultimoDia = 20_000L, ultimaActividadMs = 1L)
        val repository = mockk<RachaRepository> {
            coEvery { cargarVigente() } returns racha
        }

        val vm = HomeViewModel(repository)

        assertFalse(vm.uiState.value.cargando)
        assertEquals(4, vm.uiState.value.racha.actual)
        assertEquals(9, vm.uiState.value.racha.mejor)
        coVerify(exactly = 1) { repository.cargarVigente() }
    }

    @Test
    fun `sin actividad la racha queda en cero`() = runTest {
        val repository = mockk<RachaRepository> {
            coEvery { cargarVigente() } returns Racha()
        }

        val vm = HomeViewModel(repository)

        assertEquals(0, vm.uiState.value.racha.actual)
        assertEquals(0, vm.uiState.value.racha.mejor)
    }

    @Test
    fun `cargar recarga la racha al volver a la pantalla`() = runTest {
        val repository = mockk<RachaRepository> {
            coEvery { cargarVigente() } returnsMany listOf(Racha(actual = 1, mejor = 1), Racha(actual = 2, mejor = 2))
        }

        val vm = HomeViewModel(repository)
        vm.cargar()

        assertEquals(2, vm.uiState.value.racha.actual)
        coVerify(exactly = 2) { repository.cargarVigente() }
    }

    @Test
    fun `alReanudar omite la primera llamada y recarga en las siguientes`() = runTest {
        val repository = mockk<RachaRepository> {
            coEvery { cargarVigente() } returnsMany listOf(
                Racha(actual = 1, mejor = 1),
                Racha(actual = 2, mejor = 2)
            )
        }

        val vm = HomeViewModel(repository)
        // init ya cargó una vez; la primera reanudación (entrada inicial)
        // no debe recargar.
        vm.alReanudar()
        coVerify(exactly = 1) { repository.cargarVigente() }

        // Al volver de otra pantalla sí recarga (aquí se ve la racha nueva).
        vm.alReanudar()
        assertEquals(2, vm.uiState.value.racha.actual)
        coVerify(exactly = 2) { repository.cargarVigente() }
    }
}
