package com.udea.rutaudea.ui.screen.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udea.rutaudea.data.repository.AuthRepository
import com.udea.rutaudea.domain.model.User
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PerfilViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    /** Eventos de navegación derivados de la autenticación. */
    sealed interface PerfilEvento {
        /** Login o registro exitosos. */
        data object Autenticado : PerfilEvento

        /** El usuario cerró sesión (el gate de login debe volver a ser raíz). */
        data object SesionCerrada : PerfilEvento
    }

    data class UiState(
        val isLoading: Boolean = true,
        val user: User? = null,
        val esRegistro: Boolean = false,
        val nombre: String = "",
        val correo: String = "",
        val password: String = "",
        val error: String? = null,
        val isSubmitting: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _evento = MutableSharedFlow<PerfilEvento>(extraBufferCapacity = 1)
    val evento: SharedFlow<PerfilEvento> = _evento.asSharedFlow()

    init {
        restoreSession()
    }

    private fun restoreSession() {
        viewModelScope.launch {
            val user = authRepository.restoreSession()
            _uiState.update { it.copy(isLoading = false, user = user) }
        }
    }

    fun onNombreChange(nombre: String) =
        _uiState.update { it.copy(nombre = nombre, error = null) }

    fun onCorreoChange(correo: String) =
        _uiState.update { it.copy(correo = correo, error = null) }

    fun onPasswordChange(password: String) =
        _uiState.update { it.copy(password = password, error = null) }

    fun toggleModo() =
        _uiState.update { it.copy(esRegistro = !it.esRegistro, error = null) }

    fun submit() {
        val state = _uiState.value
        if (state.isSubmitting) return

        val correo = state.correo.trim()
        val password = state.password

        when {
            !android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches() -> {
                _uiState.update { it.copy(error = "Ingresa un correo válido") }
                return
            }
            password.length < 6 -> {
                _uiState.update { it.copy(error = "La contraseña debe tener al menos 6 caracteres") }
                return
            }
            state.esRegistro && state.nombre.trim().isEmpty() -> {
                _uiState.update { it.copy(error = "Ingresa tu nombre") }
                return
            }
        }

        _uiState.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            val result = if (state.esRegistro) {
                authRepository.register(state.nombre, correo, password)
            } else {
                authRepository.login(correo, password)
            }
            result.fold(
                onSuccess = { user ->
                    _uiState.update {
                        it.copy(isSubmitting = false, user = user, password = "")
                    }
                    _evento.tryEmit(PerfilEvento.Autenticado)
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isSubmitting = false, error = e.message) }
                }
            )
        }
    }

    fun logout() {
        authRepository.logout()
        _uiState.update {
            UiState(isLoading = false, esRegistro = false)
        }
        _evento.tryEmit(PerfilEvento.SesionCerrada)
    }
}
