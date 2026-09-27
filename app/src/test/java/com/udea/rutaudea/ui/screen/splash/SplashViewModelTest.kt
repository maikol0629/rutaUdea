package com.udea.rutaudea.ui.screen.splash

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.udea.rutaudea.data.local.dao.QuestionDao
import com.udea.rutaudea.data.repository.AuthRepository
import com.udea.rutaudea.data.repository.QuestionRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests del gate de autenticación en el splash:
 * la app solo entra a Home si hay sesión activa de Firebase.
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
    fun `con banco sembrado y sesion activa el gate permite entrar`() = runTest {
        val viewModel = splashViewModel(banco = 200, sesionActiva = true)
        assertTrue(viewModel.initialize())
    }

    @Test
    fun `con banco sembrado y sin sesion el gate manda a login`() = runTest {
        val viewModel = splashViewModel(banco = 200, sesionActiva = false)
        assertFalse(viewModel.initialize())
    }

    @Test
    fun `banco vacio lanza error de seed en initialize directo`() = runTest {
        // Contrato: initialize() falla con IllegalArgumentException si el
        // banco nunca siembra; el gate usa initializeAsync() que hace
        // fallback a la verificación de sesión (ver test siguiente).
        val viewModel = splashViewModel(banco = 0, sesionActiva = false)

        var lanzo = false
        try {
            viewModel.initialize()
        } catch (e: IllegalArgumentException) {
            lanzo = true
        }
        assertTrue(lanzo)
    }

    @Test
    fun `timeout de seed no bloquea el gate - reporta la sesion real`() = runTest {
        // El seed nunca completa (0 preguntas), pero el gate debe decidir
        // con la sesión real: con sesión activa se entra a Home.
        val viewModel = splashViewModel(banco = 0, sesionActiva = true)

        var resultado: Boolean? = null
        viewModel.initializeAsync { resultado = it }
        advanceUntilIdle()

        assertTrue(resultado == true)
    }

    // ----- helpers -----

    private fun splashViewModel(banco: Int, sesionActiva: Boolean): SplashViewModel {
        val dao = mockk<QuestionDao> {
            coEvery { countQuestions() } returns banco
        }
        val questionRepository = QuestionRepository(dao)

        val auth = mockk<FirebaseAuth> {
            every { currentUser } returns if (sesionActiva) mockk<FirebaseUser>() else null
        }
        val authRepository = AuthRepository(auth, mockk<FirebaseFirestore>(relaxed = true))

        return SplashViewModel(questionRepository, authRepository)
    }
}
