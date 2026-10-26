package com.udea.rutaudea.ui.screen.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udea.rutaudea.data.repository.AuthRepository
import com.udea.rutaudea.domain.model.DatosPerfil
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel del onboarding obligatorio (y de la edición de perfil, que
 * reutiliza los mismos campos).
 *
 * Precarga los datos del usuario autenticado, valida con [DatosPerfil] y
 * persiste con [AuthRepository.completeProfile], que marca el onboarding
 * como completado.
 */
class OnboardingViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    sealed interface Evento {
        /** Perfil guardado correctamente. */
        data object Guardado : Evento

        /** El usuario decidió cerrar sesión desde el onboarding. */
        data object SesionCerrada : Evento
    }

    data class UiState(
        val cargando: Boolean = true,
        val datos: DatosPerfil = DatosPerfil(),
        /** Errores de validación, uno por campo. */
        val errores: List<String> = emptyList(),
        /** Error de backend (red, reglas, sesión). */
        val error: String? = null,
        val guardando: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _evento = MutableSharedFlow<Evento>(extraBufferCapacity = 1)
    val evento: SharedFlow<Evento> = _evento.asSharedFlow()

    init {
        cargar()
    }

    /** Carga los datos actuales del usuario para precargar el formulario. */
    fun cargar() {
        viewModelScope.launch {
            val user = authRepository.restoreSession()
            _uiState.update {
                it.copy(
                    cargando = false,
                    datos = user?.let(DatosPerfil::desdeUsuario) ?: DatosPerfil()
                )
            }
        }
    }

    // ----- Campos -----

    fun onNombreChange(nombre: String) = actualizar {
        copy(nombre = nombre)
    }

    fun onApellidoChange(apellido: String) = actualizar {
        copy(apellido = apellido)
    }

    /**
     * Al elegir "Otra" se limpia el grado/semestre: el usuario no pertenece
     * a una institución con esa estructura.
     */
    fun onTipoInstitucionChange(tipo: String) = actualizar {
        copy(
            tipoInstitucion = tipo,
            gradoSemestre = if (tipo == DatosPerfil.TIPO_OTRA) "" else gradoSemestre
        )
    }

    fun onInstitucionChange(institucion: String) = actualizar {
        copy(institucion = institucion)
    }

    fun onGradoSemestreChange(gradoSemestre: String) = actualizar {
        copy(gradoSemestre = gradoSemestre)
    }

    fun onTelefonoChange(telefono: String) = actualizar {
        copy(telefono = telefono)
    }

    fun onCiudadChange(ciudad: String) = actualizar {
        copy(ciudad = ciudad)
    }

    fun onFechaExamenChange(fechaMs: Long?) = actualizar {
        copy(fechaExamen = fechaMs)
    }

    // ----- Guardado -----

    fun guardar() {
        val state = _uiState.value
        if (state.guardando) return

        val errores = state.datos.validar()
        if (errores.isNotEmpty()) {
            _uiState.update { it.copy(errores = errores, error = null) }
            return
        }

        _uiState.update { it.copy(guardando = true, errores = emptyList(), error = null) }
        viewModelScope.launch {
            authRepository.completeProfile(state.datos).fold(
                onSuccess = {
                    _uiState.update { it.copy(guardando = false) }
                    _evento.tryEmit(Evento.Guardado)
                },
                onFailure = { e ->
                    _uiState.update { it.copy(guardando = false, error = e.message) }
                }
            )
        }
    }

    fun cerrarSesion() {
        authRepository.logout()
        _evento.tryEmit(Evento.SesionCerrada)
    }

    private fun actualizar(transformacion: DatosPerfil.() -> DatosPerfil) {
        _uiState.update {
            it.copy(
                datos = it.datos.transformacion(),
                errores = emptyList(),
                error = null
            )
        }
    }
}
