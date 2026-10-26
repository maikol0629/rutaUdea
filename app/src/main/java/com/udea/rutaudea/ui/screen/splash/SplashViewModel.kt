package com.udea.rutaudea.ui.screen.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udea.rutaudea.data.repository.AuthRepository
import com.udea.rutaudea.data.repository.QuestionRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Destino inicial según la sesión y el estado del onboarding. */
enum class SplashDestino {
    /** Sin sesión: pantalla de login. */
    LOGIN,

    /** Sesión activa pero perfil incompleto: onboarding obligatorio. */
    ONBOARDING,

    /** Sesión activa y onboarding completo: Home. */
    HOME
}

/**
 * Espera a que el banco de preguntas esté sembrado en Room y decide el
 * destino inicial: login, onboarding o Home.
 */
class SplashViewModel(
    private val repository: QuestionRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    /**
     * Espera el seed del banco (≤3s) y devuelve el destino según la sesión
     * y el estado del onboarding.
     */
    suspend fun initialize(): SplashDestino {
        loadQuestions()
        return destinoSesion()
    }

    private suspend fun loadQuestions() {
        var count = 0
        repeat(30) {
            count = repository.countQuestions()
            if (count > 0) return
            delay(100)
        }
        require(count > 0) { "Database seeding timed out" }
    }

    private suspend fun destinoSesion(): SplashDestino {
        val user = authRepository.restoreSession() ?: return SplashDestino.LOGIN
        return if (user.onboardingCompletado) SplashDestino.HOME else SplashDestino.ONBOARDING
    }

    /** Inicializa en segundo plano y reporta el destino por [onReady]. */
    fun initializeAsync(onReady: (SplashDestino) -> Unit) {
        viewModelScope.launch {
            val destino = try {
                initialize()
            } catch (e: IllegalArgumentException) {
                // Seed fallido (timeout): no bloquea el gate de autenticación.
                destinoSesion()
            }
            onReady(destino)
        }
    }
}
