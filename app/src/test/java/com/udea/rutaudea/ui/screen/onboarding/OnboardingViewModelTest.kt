package com.udea.rutaudea.ui.screen.onboarding

import com.udea.rutaudea.data.repository.AuthRepository
import com.udea.rutaudea.domain.model.DatosPerfil
import com.udea.rutaudea.domain.model.User
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `precarga los datos del usuario autenticado`() = runTest {
        val user = User(
            uid = "u1",
            nombre = "Ana",
            apellido = "Pérez",
            tipoInstitucion = DatosPerfil.TIPO_UNIVERSIDAD,
            institucion = "UdeA",
            gradoSemestre = "5°"
        )
        val viewModel = crearViewModel(authRepository(user))

        assertFalse(viewModel.uiState.value.cargando)
        assertEquals("Ana", viewModel.uiState.value.datos.nombre)
        assertEquals("Pérez", viewModel.uiState.value.datos.apellido)
        assertEquals("UdeA", viewModel.uiState.value.datos.institucion)
        assertEquals("5°", viewModel.uiState.value.datos.gradoSemestre)
    }

    @Test
    fun `elegir Otra limpia el grado o semestre`() = runTest {
        val user = User(
            uid = "u1",
            nombre = "Ana",
            apellido = "Pérez",
            tipoInstitucion = DatosPerfil.TIPO_UNIVERSIDAD,
            institucion = "UdeA",
            gradoSemestre = "5°"
        )
        val viewModel = crearViewModel(authRepository(user))

        viewModel.onTipoInstitucionChange(DatosPerfil.TIPO_OTRA)

        assertEquals(DatosPerfil.TIPO_OTRA, viewModel.uiState.value.datos.tipoInstitucion)
        assertEquals("", viewModel.uiState.value.datos.gradoSemestre)
    }

    @Test
    fun `no guarda si faltan campos obligatorios`() = runTest {
        val auth = authRepository()
        val viewModel = crearViewModel(auth)

        viewModel.guardar()

        assertTrue(viewModel.uiState.value.errores.isNotEmpty())
        assertFalse(viewModel.uiState.value.guardando)
        coVerify(exactly = 0) { auth.completeProfile(any()) }
    }

    @Test
    fun `guarda un perfil valido y emite el evento`() = runTest {
        val auth = authRepository(user())
        coEvery { auth.completeProfile(any()) } returns Result.success(
            user(onboardingCompletado = true)
        )
        val viewModel = crearViewModel(auth)

        viewModel.onTipoInstitucionChange(DatosPerfil.TIPO_COLEGIO)
        viewModel.onInstitucionChange("I.E. San Juan")
        viewModel.onGradoSemestreChange("11°")

        val evento = esperarEvento(viewModel)
        viewModel.guardar()
        advanceUntilIdle()

        assertEquals(OnboardingViewModel.Evento.Guardado, evento.await())
        coVerify(exactly = 1) {
            auth.completeProfile(
                match {
                    it.nombre == "Ana" &&
                        it.apellido == "Pérez" &&
                        it.tipoInstitucion == DatosPerfil.TIPO_COLEGIO &&
                        it.institucion == "I.E. San Juan" &&
                        it.gradoSemestre == "11°"
                }
            )
        }
    }

    @Test
    fun `un error del repositorio se muestra sin emitir exito`() = runTest {
        val auth = authRepository(user())
        coEvery { auth.completeProfile(any()) } returns
            Result.failure(Exception("Sin conexión"))
        val viewModel = crearViewModel(auth)

        viewModel.onTipoInstitucionChange(DatosPerfil.TIPO_OTRA)
        viewModel.onInstitucionChange("Academia")
        viewModel.guardar()

        assertEquals("Sin conexión", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.guardando)
    }

    @Test
    fun `cerrar sesion emite el evento y limpia la sesion`() = runTest {
        val auth = authRepository(user())
        val viewModel = crearViewModel(auth)

        val evento = esperarEvento(viewModel)
        viewModel.cerrarSesion()

        assertEquals(OnboardingViewModel.Evento.SesionCerrada, evento.await())
        coVerify(exactly = 1) { auth.logout() }
    }

    // ----- helpers -----

    private fun TestScope.esperarEvento(viewModel: OnboardingViewModel) =
        async(UnconfinedTestDispatcher(testScheduler)) { viewModel.evento.first() }

    private fun crearViewModel(auth: AuthRepository): OnboardingViewModel =
        OnboardingViewModel(auth)

    private fun authRepository(user: User? = null): AuthRepository = mockk {
        coEvery { restoreSession() } returns user
        every { logout() } just runs
    }

    private fun user(onboardingCompletado: Boolean = false) = User(
        uid = "u1",
        nombre = "Ana",
        apellido = "Pérez",
        correo = "ana@udea.edu.co",
        onboardingCompletado = onboardingCompletado
    )
}
