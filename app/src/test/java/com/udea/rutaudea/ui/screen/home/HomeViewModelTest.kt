package com.udea.rutaudea.ui.screen.home

import com.udea.rutaudea.data.repository.AuthRepository
import com.udea.rutaudea.data.repository.EncuestaRepository
import com.udea.rutaudea.data.repository.RachaRepository
import com.udea.rutaudea.domain.model.EncuestaSatisfaccion
import com.udea.rutaudea.domain.model.Racha
import com.udea.rutaudea.domain.model.User
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
 * Tests del ViewModel del Home: expone la racha vigente y el saludo con
 * el nombre del usuario, y recarga al volver a la pantalla.
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

    private fun rachaRepository(racha: Racha = Racha()): RachaRepository = mockk {
        coEvery { cargarVigente() } returns racha
    }

    private fun encuestaRepository(respondio: Boolean = false): EncuestaRepository = mockk {
        coEvery { cargar() } returns if (respondio) {
            EncuestaSatisfaccion(tipoInstitucion = "Colegio")
        } else {
            null
        }
    }

    private fun authRepository(nombre: String? = null): AuthRepository = mockk {
        coEvery { restoreSession() } returns nombre?.let { User(uid = "u1", nombre = it) }
    }

    @Test
    fun `carga la racha vigente al iniciar`() = runTest {
        val racha = Racha(actual = 4, mejor = 9, ultimoDia = 20_000L, ultimaActividadMs = 1L)
        val repository = rachaRepository(racha)

        val vm = HomeViewModel(repository, authRepository("Maikol"), encuestaRepository())

        assertFalse(vm.uiState.value.cargando)
        assertEquals(4, vm.uiState.value.racha.actual)
        assertEquals(9, vm.uiState.value.racha.mejor)
        coVerify(exactly = 1) { repository.cargarVigente() }
    }

    @Test
    fun `sin actividad la racha queda en cero`() = runTest {
        val vm = HomeViewModel(rachaRepository(), authRepository("Maikol"), encuestaRepository())

        assertEquals(0, vm.uiState.value.racha.actual)
        assertEquals(0, vm.uiState.value.racha.mejor)
    }

    @Test
    fun `el saludo incluye el primer nombre del usuario`() = runTest {
        val vm = HomeViewModel(rachaRepository(), authRepository("Maikol Henao"), encuestaRepository())

        assertEquals("Maikol", vm.uiState.value.saludo.nombre)
    }

    @Test
    fun `sin sesion el saludo no incluye ningun nombre`() = runTest {
        val vm = HomeViewModel(rachaRepository(), authRepository(null), encuestaRepository())

        val momento = vm.uiState.value.saludo.momento
        assertTrue(
            momento == "Buenos días" ||
                momento == "Buenas tardes" ||
                momento == "Buenas noches"
        )
        assertEquals("", vm.uiState.value.saludo.nombre)
    }

    @Test
    fun `cargar recarga la racha al volver a la pantalla`() = runTest {
        val repository = mockk<RachaRepository> {
            coEvery { cargarVigente() } returnsMany listOf(Racha(actual = 1, mejor = 1), Racha(actual = 2, mejor = 2))
        }

        val vm = HomeViewModel(repository, authRepository("Maikol"), encuestaRepository())
        vm.cargar()

        assertEquals(2, vm.uiState.value.racha.actual)
        coVerify(exactly = 2) { repository.cargarVigente() }
    }

    @Test
    fun `alReanudar omite la primera llamada y recarga en las siguientes`() = runTest {        val repository = mockk<RachaRepository> {
            coEvery { cargarVigente() } returnsMany listOf(
                Racha(actual = 1, mejor = 1),
                Racha(actual = 2, mejor = 2)
            )
        }

        val vm = HomeViewModel(repository, authRepository("Maikol"), encuestaRepository())
        // init ya cargó una vez; la primera reanudación (entrada inicial)
        // no debe recargar.
        vm.alReanudar()
        coVerify(exactly = 1) { repository.cargarVigente() }

        // Al volver de otra pantalla sí recarga (aquí se ve la racha nueva).
        vm.alReanudar()
        assertEquals(2, vm.uiState.value.racha.actual)
        coVerify(exactly = 2) { repository.cargarVigente() }
    }

    @Test
    fun `expone si la encuesta piloto ya fue respondida`() = runTest {
        val vm = HomeViewModel(
            rachaRepository(),
            authRepository("Maikol"),
            encuestaRepository(respondio = true)
        )

        assertTrue(vm.uiState.value.encuestaRespondida)
    }

    @Test
    fun `sin respuesta la encuesta figura como pendiente`() = runTest {
        val vm = HomeViewModel(rachaRepository(), authRepository("Maikol"), encuestaRepository())

        assertFalse(vm.uiState.value.encuestaRespondida)
    }
}
