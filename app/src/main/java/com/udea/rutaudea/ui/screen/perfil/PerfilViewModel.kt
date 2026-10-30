package com.udea.rutaudea.ui.screen.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udea.rutaudea.data.repository.AuthRepository
import com.udea.rutaudea.data.repository.EncuestaRepository
import com.udea.rutaudea.data.repository.QuestionRepository
import com.udea.rutaudea.domain.model.User
import com.udea.rutaudea.domain.model.ValidadorCorreo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado de la pantalla de autenticación/perfil.
 *
 * Cubre tres modos: inicio de sesión, registro manual y perfil ya
 * autenticado. Google Sign-In se resuelve en la UI (Credential Manager) y
 * el ID token se entrega a [onGoogleIdToken].
 */
class PerfilViewModel(
    private val authRepository: AuthRepository,
    private val encuestaRepository: EncuestaRepository,
    private val questionRepository: QuestionRepository
) : ViewModel() {

    /** Eventos de navegación derivados de la autenticación. */
    sealed interface PerfilEvento {
        /**
         * Login, registro o Google exitosos. [requiereOnboarding] indica si
         * la navegación debe ir al onboarding o directo al Home.
         */
        data class Autenticado(val requiereOnboarding: Boolean) : PerfilEvento

        /** El usuario cerró sesión (el gate de login debe volver a ser raíz). */
        data object SesionCerrada : PerfilEvento
    }

    data class UiState(
        val isLoading: Boolean = true,
        val user: User? = null,
        val esRegistro: Boolean = false,
        val nombre: String = "",
        val apellido: String = "",
        val correo: String = "",
        val password: String = "",
        val confirmPassword: String = "",
        val aceptaTerminos: Boolean = false,
        val error: String? = null,
        val mensaje: String? = null,
        val isSubmitting: Boolean = false,
        val googleCargando: Boolean = false,
        /** `true` si el usuario ya respondió la encuesta piloto. */
        val encuestaRespondida: Boolean = false,
        /** Estado del banco local y de la última sincronización manual. */
        val banco: BancoUiState = BancoUiState(),
        // ----- Recuperación de contraseña -----
        val mostrarRecuperacion: Boolean = false,
        val correoRecuperacion: String = "",
        val recuperacionEnviada: Boolean = false
    )

    /** Datos visibles de la tarjeta de diagnóstico del banco. */
    data class BancoUiState(
        val version: String? = null,
        val hash: String? = null,
        val total: Int = 0,
        val ultimaSincronizacion: Long = 0L,
        val schemaVersion: Int = 0,
        val sincronizando: Boolean = false,
        val mensaje: String? = null,
        val error: String? = null
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
            val respondida = if (user != null) {
                try {
                    encuestaRepository.cargar() != null
                } catch (e: Exception) {
                    false
                }
            } else {
                false
            }
            _uiState.update {
                it.copy(isLoading = false, user = user, encuestaRespondida = respondida)
            }
            if (user != null) cargarDiagnosticoBanco()
        }
    }

    /**
     * Recarga la sesión y el estado de la encuesta (por ejemplo, al volver
     * de responderla o de editar el perfil).
     */
    fun recargar() {
        restoreSession()
    }

    // ----- Diagnóstico del banco -----

    /**
     * Fuerza una sincronización con `questions_v2` (botón «Buscar
     * actualizaciones») y refresca la tarjeta de diagnóstico.
     */
    fun buscarActualizaciones() {
        if (_uiState.value.banco.sincronizando) return
        _uiState.update {
            it.copy(banco = it.banco.copy(sincronizando = true, mensaje = null, error = null))
        }
        viewModelScope.launch {
            val resultado = questionRepository.sincronizarBanco()
            val mensaje = BancoDiagnostico.mensaje(resultado)
            _uiState.update {
                it.copy(
                    banco = it.banco.copy(
                        sincronizando = false,
                        mensaje = if (mensaje.esError) null else mensaje.texto,
                        error = if (mensaje.esError) mensaje.texto else null
                    )
                )
            }
            cargarDiagnosticoBanco()
        }
    }

    private suspend fun cargarDiagnosticoBanco() {
        val info = questionRepository.infoBanco()
        val total = try {
            questionRepository.countAll()
        } catch (e: Exception) {
            info?.totalSincronizado ?: 0
        }
        _uiState.update {
            it.copy(
                banco = it.banco.copy(
                    version = info?.version,
                    hash = info?.hash,
                    total = total,
                    ultimaSincronizacion = info?.ultimaSincronizacion ?: 0L,
                    schemaVersion = info?.schemaVersion ?: 0
                )
            )
        }
    }

    // ----- Formulario -----

    fun onNombreChange(nombre: String) =
        _uiState.update { it.copy(nombre = nombre, error = null) }

    fun onApellidoChange(apellido: String) =
        _uiState.update { it.copy(apellido = apellido, error = null) }

    fun onCorreoChange(correo: String) =
        _uiState.update { it.copy(correo = correo, error = null) }

    fun onPasswordChange(password: String) =
        _uiState.update { it.copy(password = password, error = null) }

    fun onConfirmPasswordChange(confirmPassword: String) =
        _uiState.update { it.copy(confirmPassword = confirmPassword, error = null) }

    fun onAceptaTerminosChange(acepta: Boolean) =
        _uiState.update { it.copy(aceptaTerminos = acepta, error = null) }

    fun toggleModo() = _uiState.update {
        it.copy(
            esRegistro = !it.esRegistro,
            password = "",
            confirmPassword = "",
            aceptaTerminos = false,
            error = null,
            mensaje = null
        )
    }

    fun submit() {
        val state = _uiState.value
        if (state.isSubmitting || state.googleCargando) return

        val correo = state.correo.trim()
        val password = state.password
        val nombre = state.nombre.trim()
        val apellido = state.apellido.trim()

        if (!ValidadorCorreo.esValido(correo)) {
            _uiState.update { it.copy(error = "Ingresa un correo válido") }
            return
        }

        if (state.esRegistro) {
            when {
                nombre.length < 2 -> {
                    _uiState.update { it.copy(error = "Ingresa tu nombre") }
                    return
                }
                apellido.length < 2 -> {
                    _uiState.update { it.copy(error = "Ingresa tu apellido") }
                    return
                }
                password.length < 6 -> {
                    _uiState.update {
                        it.copy(error = "La contraseña debe tener al menos 6 caracteres")
                    }
                    return
                }
                state.confirmPassword != password -> {
                    _uiState.update { it.copy(error = "Las contraseñas no coinciden") }
                    return
                }
                !state.aceptaTerminos -> {
                    _uiState.update {
                        it.copy(error = "Debes aceptar los términos y la política de privacidad")
                    }
                    return
                }
            }
        } else if (password.isEmpty()) {
            _uiState.update { it.copy(error = "Ingresa tu contraseña") }
            return
        }

        _uiState.update { it.copy(isSubmitting = true, error = null, mensaje = null) }
        viewModelScope.launch {
            val result = if (state.esRegistro) {
                authRepository.register(nombre, apellido, correo, password)
            } else {
                authRepository.login(correo, password)
            }
            result.fold(
                onSuccess = { user -> aplicarSesion(user) },
                onFailure = { e ->
                    _uiState.update { it.copy(isSubmitting = false, error = e.message) }
                }
            )
        }
    }

    // ----- Google -----

    /** La UI avisa que abrió el selector de cuentas de Google. */
    fun onGoogleIniciado() =
        _uiState.update { it.copy(googleCargando = true, error = null, mensaje = null) }

    /** El usuario cerró el selector: se oculta el progreso sin mostrar error. */
    fun onGoogleCancelado() =
        _uiState.update { it.copy(googleCargando = false) }

    /** La UI no pudo obtener el token (sin Play Services, sin cuentas, etc.). */
    fun onGoogleError(mensaje: String) =
        _uiState.update { it.copy(googleCargando = false, error = mensaje) }

    /** La UI obtuvo el ID token; se autentica contra Firebase. */
    fun onGoogleIdToken(idToken: String) {
        if (_uiState.value.isSubmitting) return
        _uiState.update {
            it.copy(googleCargando = true, isSubmitting = false, error = null, mensaje = null)
        }
        viewModelScope.launch {
            authRepository.signInWithGoogle(idToken).fold(
                onSuccess = { user -> aplicarSesion(user) },
                onFailure = { e ->
                    _uiState.update { it.copy(googleCargando = false, error = e.message) }
                }
            )
        }
    }

    // ----- Recuperación de contraseña -----

    fun abrirRecuperacion() = _uiState.update {
        it.copy(
            mostrarRecuperacion = true,
            correoRecuperacion = it.correo,
            recuperacionEnviada = false,
            error = null,
            mensaje = null
        )
    }

    fun cerrarRecuperacion() = _uiState.update {
        it.copy(mostrarRecuperacion = false, recuperacionEnviada = false)
    }

    fun onCorreoRecuperacionChange(correo: String) =
        _uiState.update { it.copy(correoRecuperacion = correo, error = null) }

    fun enviarRecuperacion() {
        val correo = _uiState.value.correoRecuperacion.trim()
        if (!ValidadorCorreo.esValido(correo)) {
            _uiState.update { it.copy(error = "Ingresa un correo válido") }
            return
        }
        if (_uiState.value.isSubmitting) return

        _uiState.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            authRepository.sendPasswordReset(correo).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            recuperacionEnviada = true,
                            mensaje = "Te enviamos un correo para restablecer tu contraseña"
                        )
                    }
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

    private fun aplicarSesion(user: User) {
        _uiState.update {
            it.copy(
                isSubmitting = false,
                googleCargando = false,
                user = user,
                password = "",
                confirmPassword = "",
                error = null,
                mensaje = null
            )
        }
        _evento.tryEmit(PerfilEvento.Autenticado(user.requiereOnboarding))
    }
}
