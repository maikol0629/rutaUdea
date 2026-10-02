package com.udea.rutaudea.ui.screen.practica

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.udea.rutaudea.domain.model.Question
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Sesión de práctica: sin cronómetro límite, con retroalimentación
 * inmediata al responder (plan, sección 10): respuesta correcta,
 * respuesta seleccionada, explicación, subtema y dificultad.
 *
 * La respuesta queda bloqueada al revelarse (no se puede cambiar).
 * El progreso (índice, respuestas, reveladas) sobrevive rotación y
 * segundo plano vía [SavedStateHandle].
 */
class PracticaSesionViewModel(
    questions: List<Question>,
    val area: String,
    val subtema: String?,
    private val savedStateHandle: SavedStateHandle,
    /** Desactivable en pruebas: el reloj es un bucle infinito. */
    private val relojActivo: Boolean = true
) : ViewModel() {

    /** Navegación: la sesión terminó y los resultados están listos. */
    sealed interface SesionEvento {
        data object PracticaTerminada : SesionEvento
    }

    data class SesionState(
        val questions: List<Question> = emptyList(),
        val currentIndex: Int = 0,
        /** Letra seleccionada por el usuario, por índice de pregunta. */
        val answers: Map<Int, String> = emptyMap(),
        /** Índices ya revelados (respuesta registrada y bloqueada). */
        val revealed: Set<Int> = emptySet(),
        val elapsedMs: Long = 0L
    ) {
        val currentQuestion: Question?
            get() = questions.getOrNull(currentIndex)

        val isRevealed: Boolean
            get() = currentIndex in revealed

        val currentAnswer: String?
            get() = answers[currentIndex]

        val isCorrect: Boolean
            get() = currentQuestion?.let { answers[currentIndex] == it.respuestaCorrecta } ?: false

        val isLastQuestion: Boolean
            get() = currentIndex == questions.lastIndex

        val respondidas: Int
            get() = revealed.size

        val score: Int
            get() = questions.indices.count { index ->
                answers[index] == questions[index].respuestaCorrecta
            }

        val elapsedSeconds: Int
            get() = (elapsedMs / 1000).toInt()
    }

    private val _uiState = MutableStateFlow(SesionState(questions = questions))
    val uiState: StateFlow<SesionState> = _uiState.asStateFlow()

    private val _evento = MutableSharedFlow<SesionEvento>(extraBufferCapacity = 1)
    val evento: SharedFlow<SesionEvento> = _evento.asSharedFlow()

    private var tickerJob: Job? = null

    private val startTimeMs: Long

    init {
        // Restauración de progreso (rotación / background).
        val saved: Long? = savedStateHandle[START_TIME_KEY]
        startTimeMs = saved ?: System.currentTimeMillis().also {
            savedStateHandle[START_TIME_KEY] = it
        }
        restoreProgreso()
        iniciarTicker()
    }

    private fun restoreProgreso() {
        val answers: Map<Int, String>? = savedStateHandle[ANSWERS_KEY]
        val revealed: List<Int>? = savedStateHandle[REVEALED_KEY]
        val index: Int = savedStateHandle[CURRENT_INDEX_KEY] ?: 0
        if (answers == null && revealed == null) return

        val lastIndex = _uiState.value.questions.lastIndex
        _uiState.value = _uiState.value.copy(
            currentIndex = index.coerceIn(0, lastIndex),
            answers = answers.orEmpty(),
            revealed = revealed?.toSet().orEmpty()
        )
    }

    /** Reloj informativo ascendente (la práctica no tiene límite de tiempo). */
    private fun iniciarTicker() {
        if (!relojActivo) return
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                val elapsed = System.currentTimeMillis() - startTimeMs
                _uiState.value = _uiState.value.copy(elapsedMs = elapsed)
                delay(1000)
            }
        }
    }

    /** Registra la respuesta de la pregunta actual y revela el feedback. */
    fun onOptionSelected(letter: String) {
        val state = _uiState.value
        if (state.isRevealed) return // bloqueada: no se puede cambiar

        val index = state.currentIndex
        val answers = state.answers + (index to letter)
        val revealed = state.revealed + index

        savedStateHandle[ANSWERS_KEY] = answers
        savedStateHandle[REVEALED_KEY] = revealed.toList()
        _uiState.value = state.copy(answers = answers, revealed = revealed)
    }

    fun onNext() {
        val state = _uiState.value
        if (state.isLastQuestion) {
            finalizar(state)
        } else {
            val newIndex = state.currentIndex + 1
            savedStateHandle[CURRENT_INDEX_KEY] = newIndex
            _uiState.value = state.copy(currentIndex = newIndex)
        }
    }

    fun onPrevious() {
        val state = _uiState.value
        if (state.currentIndex > 0) {
            val newIndex = state.currentIndex - 1
            savedStateHandle[CURRENT_INDEX_KEY] = newIndex
            _uiState.value = state.copy(currentIndex = newIndex)
        }
    }

    /** Permite terminar en cualquier momento (las no reveladas quedan omitidas). */
    fun onTerminar() {
        finalizar(_uiState.value)
    }

    private fun finalizar(state: SesionState) {
        val score = state.score
        val total = state.questions.size
        val timeUsedMs = System.currentTimeMillis() - startTimeMs

        savedStateHandle[SCORE_KEY] = score
        savedStateHandle[TOTAL_KEY] = total
        savedStateHandle[TIME_USED_KEY] = timeUsedMs
        savedStateHandle[ANSWERS_JSON_KEY] = gson.toJson(state.answers)
        savedStateHandle[QUESTIONS_JSON_KEY] = gson.toJson(state.questions)
        savedStateHandle[AREA_KEY] = area
        savedStateHandle[SUBTEMA_KEY] = subtema ?: ""

        _evento.tryEmit(SesionEvento.PracticaTerminada)
    }

    override fun onCleared() {
        tickerJob?.cancel()
        super.onCleared()
    }

    companion object {
        const val SCORE_KEY = "practica_score"
        const val TOTAL_KEY = "practica_total"
        const val TIME_USED_KEY = "practica_time_used_ms"
        const val ANSWERS_JSON_KEY = "practica_user_answers_json"
        const val QUESTIONS_JSON_KEY = "practica_questions_json"
        const val AREA_KEY = "practica_area"
        const val SUBTEMA_KEY = "practica_subtema"
        private const val START_TIME_KEY = "practica_start_time_ms"
        private const val CURRENT_INDEX_KEY = "practica_current_index"
        private const val ANSWERS_KEY = "practica_answers"
        private const val REVEALED_KEY = "practica_revealed"
        private val gson = Gson()
    }
}
