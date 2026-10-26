package com.udea.rutaudea.ui.screen.splash

import com.udea.rutaudea.data.local.dao.QuestionDao
import com.udea.rutaudea.data.repository.AuthRepository
import com.udea.rutaudea.data.repository.QuestionRepository
import com.udea.rutaudea.domain.model.User
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests del gate de autenticación en el splash: decide entre login,
 * onboarding y Home según la sesión y el estado del perfil.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `sin sesion el destino es login`() = runTest {
        val viewModel = splashViewModel(banco = 200, user = null)
        assertEquals(SplashDestino.LOGIN, viewModel.initialize())
    }

    @Test
    fun `sesion con onboarding pendiente manda al onboarding`() = runTest {
        val viewModel = splashViewModel(
            banco = 200,
            user = User(uid = "u1", onboardingCompletado = false)
        )
        assertEquals(SplashDestino.ONBOARDING, viewModel.initialize())
    }

    @Test
    fun `sesion con onboarding completo va al home`() = runTest {
        val viewModel = splashViewModel(
            banco = 200,
            user = User(uid = "u1", onboardingCompletado = true)
        )
        assertEquals(SplashDestino.HOME, viewModel.initialize())
    }

    @Test
    fun `banco vacio lanza error de seed en initialize directo`() = runTest {
        // Contrato: initialize() falla con IllegalArgumentException si el
        // banco nunca siembra; el gate usa initializeAsync() que hace
        // fallback a la verificación de sesión (ver test siguiente).
        val viewModel = splashViewModel(banco = 0, user = null)

        var lanzo = false
        try {
            viewModel.initialize()
        } catch (e: IllegalArgumentException) {
            lanzo = true
        }
        assertTrue(lanzo)
    }

    @Test
    fun `timeout de seed no bloquea el gate - reporta el destino real`() = runTest {
        // El seed nunca completa (0 preguntas), pero el gate debe decidir
        // con la sesión real: sin onboarding pendiente se entra a Home.
        val viewModel = splashViewModel(
            banco = 0,
            user = User(uid = "u1", onboardingCompletado = true)
        )

        var destino: SplashDestino? = null
        viewModel.initializeAsync { destino = it }
        advanceUntilIdle()

        assertEquals(SplashDestino.HOME, destino)
    }

    // ----- helpers -----

    private fun splashViewModel(banco: Int, user: User?): SplashViewModel {
        val dao = mockk<QuestionDao> {
            coEvery { countQuestions() } returns banco
        }
        val questionRepository = QuestionRepository(dao)

        val authRepository = mockk<AuthRepository> {
            coEvery { restoreSession() } returns user
        }

        return SplashViewModel(questionRepository, authRepository)
    }
}
