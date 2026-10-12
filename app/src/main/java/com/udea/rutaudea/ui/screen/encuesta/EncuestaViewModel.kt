package com.udea.rutaudea.ui.screen.encuesta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udea.rutaudea.data.repository.EncuestaRepository
import com.udea.rutaudea.domain.model.EncuestaSatisfaccion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Estado del cuestionario de satisfacción de la prueba piloto.
 *
 * Al abrir se precarga la respuesta guardada (si el usuario ya respondió,
 * puede actualizarla). El envío valida en el ViewModel y delega el
 * guardado local-first al repositorio.
 */
class EncuestaViewModel(
    private val repository: EncuestaRepository
) : ViewModel() {

    data class UiState(
        val cargando: Boolean = true,
        val encuesta: EncuestaSatisfaccion = EncuestaSatisfaccion(),
        /** `true` si ya existe una respuesta guardada (se está actualizando). */
        val yaRespondio: Boolean = false,
        val enviando: Boolean = false,
        val errores: List<String> = emptyList(),
        /** `true` tras un envío exitoso (muestra el agradecimiento). */
        val enviada: Boolean = false,
        val sincronizada: Boolean = true
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    /** Carga la respuesta guardada (local primero, nube como respaldo). */
    fun cargar() {
        viewModelScope.launch {
            val previa = try {
                repository.cargar()
            } catch (e: Exception) {
                null
            }
            _uiState.value = UiState(
                cargando = false,
                encuesta = previa ?: EncuestaSatisfaccion(),
                yaRespondio = previa != null
            )
        }
    }

    /** Actualiza el borrador y limpia los errores al editar. */
    fun actualizar(encuesta: EncuestaSatisfaccion) {
        _uiState.value = _uiState.value.copy(encuesta = encuesta, errores = emptyList())
    }

    /** Valida y envía la encuesta (guardado local-first + sync best-effort). */
    fun enviar() {
        val actual = _uiState.value
        if (actual.enviando || actual.cargando) return
        val errores = actual.encuesta.validar()
        if (errores.isNotEmpty()) {
            _uiState.value = actual.copy(errores = errores)
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(enviando = true, errores = emptyList())
            when (val resultado = repository.guardar(_uiState.value.encuesta)) {
                is EncuestaRepository.ResultadoGuardado.Guardada ->
                    _uiState.value = _uiState.value.copy(
                        enviando = false,
                        enviada = true,
                        yaRespondio = true,
                        sincronizada = resultado.sincronizada
                    )
                is EncuestaRepository.ResultadoGuardado.Invalida ->
                    _uiState.value = _uiState.value.copy(enviando = false, errores = resultado.errores)
                EncuestaRepository.ResultadoGuardado.SinSesion ->
                    _uiState.value = _uiState.value.copy(
                        enviando = false,
                        errores = listOf("Inicia sesión para enviar la encuesta")
                    )
                EncuestaRepository.ResultadoGuardado.ErrorLocal ->
                    _uiState.value = _uiState.value.copy(
                        enviando = false,
                        errores = listOf("No se pudo guardar en este dispositivo, intenta de nuevo")
                    )
            }
        }
    }

    /** Vuelve al formulario tras el agradecimiento (para actualizar). */
    fun seguirEditando() {
        _uiState.value = _uiState.value.copy(enviada = false)
    }
}
