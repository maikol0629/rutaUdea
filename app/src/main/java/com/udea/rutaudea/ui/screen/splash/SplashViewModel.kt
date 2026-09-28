package com.udea.rutaudea.ui.screen.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udea.rutaudea.data.repository.AuthRepository
import com.udea.rutaudea.data.repository.QuestionRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Espera a que el banco de preguntas esté sembrado en Room y verifica
 * si existe una sesión activa (gate de autenticación).
 */
class SplashViewModel(
    private val repository: QuestionRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    /**
     * Espera el seed del banco (≤3s) y devuelve `true` si el usuario
     * ya tiene una sesión activa de Firebase Auth.
     *
     * El gate de autenticación usa este resultado para decidir el
     * destino inicial: Home (con sesión) o la pantalla de login.
     */
    suspend fun initialize(): Boolean {
        loadQuestions()
        return authRepository.isLoggedIn()
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

    /** Inicializa en segundo plano y reporta el resultado por [onReady]. */
    fun initializeAsync(onReady: (sesionActiva: Boolean) -> Unit) {
        viewModelScope.launch {
            val sesionActiva = try {
                initialize()
            } catch (e: IllegalArgumentException) {
                // Seed fallido (timeout): no bloquea el gate de autenticación.
                authRepository.isLoggedIn()
            }
            onReady(sesionActiva)
        }
    }
}
