package com.udea.rutaudea.ui.screen.perfil

import com.udea.rutaudea.data.repository.AuthRepository
import com.udea.rutaudea.data.repository.EncuestaRepository
import com.udea.rutaudea.data.repository.QuestionRepository
import com.udea.rutaudea.data.sync.ResultadoSync
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PerfilViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ----- Registro manual -----

    @Test
    fun `el registro exige apellido`() = runTest {
        val auth = authRepository()
        val viewModel = crearViewModel(auth)

        viewModel.toggleModo()
        llenarRegistro(viewModel, apellido = "")
        viewModel.submit()

        assertEquals("Ingresa tu apellido", viewModel.uiState.value.error)
        coVerify(exactly = 0) { auth.register(any(), any(), any(), any()) }
    }

    @Test
    fun `el registro exige que las contrasenas coincidan`() = runTest {
        val auth = authRepository()
        val viewModel = crearViewModel(auth)

        viewModel.toggleModo()
        llenarRegistro(viewModel, confirmacion = "654321")
        viewModel.submit()

        assertEquals("Las contraseñas no coinciden", viewModel.uiState.value.error)
        coVerify(exactly = 0) { auth.register(any(), any(), any(), any()) }
    }

    @Test
    fun `el registro exige aceptar los terminos`() = runTest {
        val auth = authRepository()
        val viewModel = crearViewModel(auth)

        viewModel.toggleModo()
        llenarRegistro(viewModel, aceptaTerminos = false)
        viewModel.submit()

        assertEquals(
            "Debes aceptar los términos y la política de privacidad",
            viewModel.uiState.value.error
        )
        coVerify(exactly = 0) { auth.register(any(), any(), any(), any()) }
    }

    @Test
    fun `un registro exitoso pide onboarding`() = runTest {
        val auth = authRepository()
        coEvery {
            auth.register("Ana", "Pérez", "ana@udea.edu.co", "123456")
        } returns Result.success(user(onboardingCompletado = false))
        val viewModel = crearViewModel(auth)

        val evento = esperarEvento(viewModel)
        viewModel.toggleModo()
        llenarRegistro(viewModel)
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(
            PerfilViewModel.PerfilEvento.Autenticado(requiereOnboarding = true),
            evento.await()
        )
        assertFalse(viewModel.uiState.value.user!!.onboardingCompletado)
    }

    // ----- Login -----

    @Test
    fun `un login con onboarding pendiente pide onboarding`() = runTest {
        val auth = authRepository()
        coEvery { auth.login("ana@udea.edu.co", "123456") } returns
            Result.success(user(onboardingCompletado = false))
        val viewModel = crearViewModel(auth)

        val evento = esperarEvento(viewModel)
        viewModel.onCorreoChange("ana@udea.edu.co")
        viewModel.onPasswordChange("123456")
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(
            PerfilViewModel.PerfilEvento.Autenticado(requiereOnboarding = true),
            evento.await()
        )
    }

    @Test
    fun `un login con onboarding completo va directo al home`() = runTest {
        val auth = authRepository()
        coEvery { auth.login("ana@udea.edu.co", "123456") } returns
            Result.success(user(onboardingCompletado = true))
        val viewModel = crearViewModel(auth)

        val evento = esperarEvento(viewModel)
        viewModel.onCorreoChange("ana@udea.edu.co")
        viewModel.onPasswordChange("123456")
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(
            PerfilViewModel.PerfilEvento.Autenticado(requiereOnboarding = false),
            evento.await()
        )
    }

    @Test
    fun `un login invalido muestra el error del repositorio`() = runTest {
        val auth = authRepository()
        coEvery { auth.login(any(), any()) } returns
            Result.failure(Exception("Correo o contraseña incorrectos"))
        val viewModel = crearViewModel(auth)

        viewModel.onCorreoChange("ana@udea.edu.co")
        viewModel.onPasswordChange("123456")
        viewModel.submit()

        assertEquals("Correo o contraseña incorrectos", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    // ----- Google -----

    @Test
    fun `google exitoso autentica y decide el onboarding`() = runTest {
        val auth = authRepository()
        coEvery { auth.signInWithGoogle("id-token") } returns
            Result.success(user(onboardingCompletado = false))
        val viewModel = crearViewModel(auth)

        val evento = esperarEvento(viewModel)
        viewModel.onGoogleIniciado()
        assertTrue(viewModel.uiState.value.googleCargando)
        viewModel.onGoogleIdToken("id-token")
        advanceUntilIdle()

        assertEquals(
            PerfilViewModel.PerfilEvento.Autenticado(requiereOnboarding = true),
            evento.await()
        )
        assertFalse(viewModel.uiState.value.googleCargando)
        coVerify(exactly = 1) { auth.signInWithGoogle("id-token") }
    }

    @Test
    fun `google fallido muestra un error y oculta el progreso`() = runTest {
        val auth = authRepository()
        coEvery { auth.signInWithGoogle("id-token") } returns
            Result.failure(Exception("Sin conexión"))
        val viewModel = crearViewModel(auth)

        viewModel.onGoogleIniciado()
        viewModel.onGoogleIdToken("id-token")

        assertEquals("Sin conexión", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.googleCargando)
    }

    @Test
    fun `cancelar google no muestra error`() = runTest {
        val viewModel = crearViewModel(authRepository())

        viewModel.onGoogleIniciado()
        viewModel.onGoogleCancelado()

        assertFalse(viewModel.uiState.value.googleCargando)
        assertNull(viewModel.uiState.value.error)
    }

    // ----- Recuperación de contraseña -----

    @Test
    fun `la recuperacion valida el correo antes de enviar`() = runTest {
        val auth = authRepository()
        val viewModel = crearViewModel(auth)

        viewModel.abrirRecuperacion()
        viewModel.onCorreoRecuperacionChange("correo-invalido")
        viewModel.enviarRecuperacion()

        assertEquals("Ingresa un correo válido", viewModel.uiState.value.error)
        coVerify(exactly = 0) { auth.sendPasswordReset(any()) }
    }

    @Test
    fun `la recuperacion exitosa confirma el envio`() = runTest {
        val auth = authRepository()
        coEvery { auth.sendPasswordReset("ana@udea.edu.co") } returns Result.success(Unit)
        val viewModel = crearViewModel(auth)

        viewModel.abrirRecuperacion()
        viewModel.onCorreoRecuperacionChange("ana@udea.edu.co")
        viewModel.enviarRecuperacion()

        assertTrue(viewModel.uiState.value.recuperacionEnviada)
        assertEquals(
            "Te enviamos un correo para restablecer tu contraseña",
            viewModel.uiState.value.mensaje
        )
    }

    @Test
    fun `el logout emite el evento de sesion cerrada`() = runTest {
        val viewModel = crearViewModel(authRepository())

        val evento = esperarEvento(viewModel)
        viewModel.logout()

        assertEquals(
            PerfilViewModel.PerfilEvento.SesionCerrada,
            evento.await()
        )
        assertNull(viewModel.uiState.value.user)
    }

    // ----- Diagnóstico del banco -----

    @Test
    fun `buscar actualizaciones exitoso refresca el diagnostico`() = runTest {
        val auth = mockk<AuthRepository> {
            coEvery { restoreSession() } returns user(onboardingCompletado = false)
        }
        val repo = mockk<QuestionRepository>(relaxed = true) {
            every { infoBanco() } returns QuestionRepository.InfoBanco(
                version = "udea-2026-10-v1",
                hash = "fdfe8d9fd41fcd657dde7900",
                totalSincronizado = 396,
                ultimaSincronizacion = 1_700_000_000_000L,
                schemaVersion = 2
            )
            coEvery { countAll() } returns 396
            coEvery { sincronizarBanco() } returns
                ResultadoSync.Actualizado("udea-2026-10-v1", 396)
        }
        val viewModel = PerfilViewModel(auth, encuestaRepository(), repo)
        advanceUntilIdle()

        viewModel.buscarActualizaciones()
        advanceUntilIdle()

        val banco = viewModel.uiState.value.banco
        assertEquals("udea-2026-10-v1", banco.version)
        assertEquals(396, banco.total)
        assertNull(banco.error)
        assertTrue(banco.mensaje!!.contains("Banco actualizado"))
        assertFalse(banco.sincronizando)
    }

    @Test
    fun `buscar actualizaciones sin manifiesto muestra error`() = runTest {
        val auth = mockk<AuthRepository> {
            coEvery { restoreSession() } returns user(onboardingCompletado = false)
        }
        val repo = mockk<QuestionRepository>(relaxed = true) {
            every { infoBanco() } returns null
            coEvery { countAll() } returns 400
            coEvery { sincronizarBanco() } returns ResultadoSync.SinManifiesto
        }
        val viewModel = PerfilViewModel(auth, encuestaRepository(), repo)
        advanceUntilIdle()

        viewModel.buscarActualizaciones()
        advanceUntilIdle()

        val banco = viewModel.uiState.value.banco
        assertNull(banco.mensaje)
        assertTrue(banco.error!!.contains("banco activo"))
        assertFalse(banco.sincronizando)
    }

    // ----- helpers -----

    private fun TestScope.esperarEvento(viewModel: PerfilViewModel) =
        async(UnconfinedTestDispatcher(testScheduler)) { viewModel.evento.first() }

    private fun crearViewModel(auth: AuthRepository): PerfilViewModel =
        PerfilViewModel(auth, encuestaRepository(), questionRepository())

    private fun llenarRegistro(
        viewModel: PerfilViewModel,
        nombre: String = "Ana",
        apellido: String = "Pérez",
        confirmacion: String = "123456",
        aceptaTerminos: Boolean = true
    ) {
        viewModel.onNombreChange(nombre)
        viewModel.onApellidoChange(apellido)
        viewModel.onCorreoChange("ana@udea.edu.co")
        viewModel.onPasswordChange("123456")
        viewModel.onConfirmPasswordChange(confirmacion)
        viewModel.onAceptaTerminosChange(aceptaTerminos)
    }

    private fun authRepository(): AuthRepository = mockk {
        coEvery { restoreSession() } returns null
        every { logout() } just runs
    }

    private fun encuestaRepository(): EncuestaRepository = mockk {
        coEvery { cargar() } returns null
    }

    private fun questionRepository(): QuestionRepository = mockk(relaxed = true) {
        coEvery { countAll() } returns 0
        every { infoBanco() } returns null
    }

    private fun user(onboardingCompletado: Boolean) = User(
        uid = "u1",
        nombre = "Ana",
        apellido = "Pérez",
        correo = "ana@udea.edu.co",
        onboardingCompletado = onboardingCompletado
    )
}
