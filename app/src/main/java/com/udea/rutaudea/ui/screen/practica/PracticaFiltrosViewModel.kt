package com.udea.rutaudea.ui.screen.practica

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.udea.rutaudea.data.repository.QuestionRepository
import com.udea.rutaudea.domain.model.Question
import com.udea.rutaudea.domain.services.PracticeSelector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Filtros del módulo de Práctica: área + subtema + cantidad.
 *
 * La dificultad NO es elegible: la selección se estratifica por dificultad
 * de forma proporcional a la composición real del banco filtrado
 * ([PracticeSelector], decisión de diseño del módulo).
 */
class PracticaFiltrosViewModel(
    private val repository: QuestionRepository,
    private val selector: PracticeSelector,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    /** Navegación: la selección está lista para iniciar la sesión. */
    sealed interface FiltrosEvento {
        data object PracticaLista : FiltrosEvento
    }

    data class UiState(
        val isLoading: Boolean = true,
        val area: String = AREA_RL,
        val subtemas: List<String> = emptyList(),
        /** `null` = todos los subtemas. */
        val subtema: String? = null,
        val cantidad: Int = 10,
        val disponibles: Int = 0,
        val error: String? = null,
        val isStarting: Boolean = false
    ) {
        /** Cantidad efectiva a practicar (limitada por las disponibles). */
        val cantidadEfectiva: Int
            get() = minOf(cantidad, disponibles)

        val puedeIniciar: Boolean
            get() = !isLoading && !isStarting && disponibles > 0
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _evento = MutableSharedFlow<FiltrosEvento>(extraBufferCapacity = 1)
    val evento: SharedFlow<FiltrosEvento> = _evento.asSharedFlow()

    init {
        cargarSubtemas()
    }

    fun onAreaChange(area: String) {
        if (area == _uiState.value.area) return
        _uiState.update { it.copy(area = area, subtema = null) }
        cargarSubtemas()
    }

    fun onSubtemaChange(subtema: String?) {
        if (subtema == _uiState.value.subtema) return
        _uiState.update { it.copy(subtema = subtema) }
        actualizarDisponibles()
    }

    fun onCantidadChange(cantidad: Int) {
        _uiState.update { it.copy(cantidad = cantidad) }
    }

    fun iniciarPractica() {
        val state = _uiState.value
        if (state.isStarting || !state.puedeIniciar) return

        _uiState.update { it.copy(isStarting = true, error = null) }
        viewModelScope.launch {
            val pool = repository.getQuestions(
                area = state.area,
                subtema = state.subtema,
                limit = LIMITE_POOL
            )
            val seleccion = selector.select(pool, state.cantidad)
            if (seleccion.isEmpty()) {
                _uiState.update {
                    it.copy(isStarting = false, error = "No hay preguntas disponibles con estos filtros")
                }
                return@launch
            }

            // La sesión lee estos valores desde el SavedStateHandle de esta
            // entrada (patrón del simulacro: previousBackStackEntry).
            savedStateHandle[PREGUNTAS_KEY] = gson.toJson(seleccion)
            savedStateHandle[AREA_KEY] = state.area
            savedStateHandle[SUBTEMA_KEY] = state.subtema ?: ""

            _uiState.update { it.copy(isStarting = false) }
            _evento.tryEmit(FiltrosEvento.PracticaLista)
        }
    }

    private fun cargarSubtemas() {
        val area = _uiState.value.area
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val subtemas = try {
                repository.getDistinctSubtemas(area)
            } catch (e: Exception) {
                emptyList()
            }
            _uiState.update { it.copy(isLoading = false, subtemas = subtemas) }
            actualizarDisponibles()
        }
    }

    private fun actualizarDisponibles() {
        val state = _uiState.value
        viewModelScope.launch {
            val disponibles = try {
                repository.countByFilters(state.area, state.subtema)
            } catch (e: Exception) {
                0
            }
            _uiState.update { it.copy(disponibles = disponibles) }
        }
    }

    companion object {
        const val AREA_RL = "razonamiento_logico"
        const val AREA_CL = "competencia_lectora"
        const val PREGUNTAS_KEY = "practica_questions_json"
        const val AREA_KEY = "practica_area"
        const val SUBTEMA_KEY = "practica_subtema"
        private const val LIMITE_POOL = 1000
        private val gson = Gson()
    }
}
