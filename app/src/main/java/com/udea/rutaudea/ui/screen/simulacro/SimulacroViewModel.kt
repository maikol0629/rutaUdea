package com.udea.rutaudea.ui.screen.simulacro

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.udea.rutaudea.data.repository.QuestionRepository
import com.udea.rutaudea.data.repository.SimulationRepository
import com.udea.rutaudea.domain.model.DistribucionDificultad
import com.udea.rutaudea.domain.model.Question
import com.udea.rutaudea.domain.model.SimulacroConfig
import com.udea.rutaudea.domain.services.QuestionSelector
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SimulacroState(
    val questions: List<Question> = emptyList(),
    val currentIndex: Int = 0,
    val userAnswers: Map<Int, String> = emptyMap(),
    val timeRemainingMs: Long = 0,
    val isFinished: Boolean = false,
    val showResumeOverlay: Boolean = false
) {
    val currentAnswer: String?
        get() = userAnswers[currentIndex]

    val isLastQuestion: Boolean
        get() = currentIndex == questions.lastIndex

    val timeRemainingSeconds: Int
        get() = (timeRemainingMs / 1000).toInt()
}

class SimulacroViewModel(
    private val selector: QuestionSelector,
    private val repository: QuestionRepository,
    private val simulationRepository: SimulationRepository,
    private val savedStateHandle: SavedStateHandle,
    /** Desactivable en pruebas: el cronómetro es un bucle infinito. */
    private val relojActivo: Boolean = true
) : ViewModel() {

    companion object {
        private const val START_TIME_KEY = "start_time_ms"
        private const val USER_ANSWERS_KEY = "user_answers"
        private const val CURRENT_INDEX_KEY = "current_index"
        private const val SELECTION_KEY = "selection_ids_json"
        private val gson = Gson()
    }

    /** Duración del simulacro en milisegundos (desde settings/config o 120 min por defecto). */
    private var duracionSimulacroMs: Long = 120 * 60 * 1000L

    private val _uiState: MutableStateFlow<SimulacroState> = MutableStateFlow(SimulacroState())
    val uiState: StateFlow<SimulacroState> = _uiState

    private var timerJob: kotlinx.coroutines.Job? = null
    private var pendingRestoredIndex: Int = 0

    /**
     * Marca que la app pasó a segundo plano de verdad (no una rotación).
     * Se usa para no mostrar la pausa en el `ON_START` sintético que
     * dispara el registro del observador de ciclo de vida.
     */
    private var pausadoPorBackground: Boolean = false

    init {
        restoreState()
        loadQuestions()
    }

    private fun restoreState() {
        val hasSavedState = savedStateHandle.contains(START_TIME_KEY)

        if (hasSavedState) {
            val savedStartTime = savedStateHandle.get<Long>(START_TIME_KEY) ?: 0
            val savedAnswers = savedStateHandle.get<MutableMap<Int, String>>(USER_ANSWERS_KEY) ?: mutableMapOf()
            val savedIndex = savedStateHandle.get<Int>(CURRENT_INDEX_KEY) ?: 0
            val elapsed = System.currentTimeMillis() - savedStartTime
            val remainingMs = maxOf(0L, duracionSimulacroMs - elapsed)

            pendingRestoredIndex = savedIndex
            val currentState = _uiState.value
            _uiState.value = currentState.copy(
                timeRemainingMs = remainingMs,
                userAnswers = savedAnswers.toMap(),
                showResumeOverlay = remainingMs > 0L && !currentState.isFinished
            )
        }
    }

    private fun loadQuestions() {
        viewModelScope.launch {
            // Lee la configuración publicada en el panel antes de arrancar el cronómetro.
            duracionSimulacroMs = repository.leerConfiguracionSimulacro().duracionMs
            val questions = cargarSeleccion()
            if (questions.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(
                    questions = questions,
                    currentIndex = pendingRestoredIndex.coerceAtMost(questions.lastIndex)
                )
                val state = _uiState.value
                if (!savedStateHandle.contains(START_TIME_KEY)) {
                    startTimer()
                } else if (state.timeRemainingMs <= 0L) {
                    // El tiempo se agotó mientras el proceso estaba muerto:
                    // persistir el resultado en vez de quedar atascado en 0:00.
                    saveSimulationResults(state)
                    _uiState.value = state.copy(isFinished = true)
                } else if (!state.isFinished) {
                    resumeTimer()
                }
            }
        }
    }

    /**
     * Carga la selección del simulacro: si hay una selección persistida
     * (rotación/background), restaura esas preguntas; si no, usa el
     * motor de selección (plan sección 8) excluyendo las preguntas de
     * los últimos 3 simulacros.
     */
    private suspend fun cargarSeleccion(): List<Question> {
        val savedIds = savedStateHandle.get<String>(SELECTION_KEY)
        if (savedIds != null) {
            val ids = gson.fromJson(savedIds, Array<String>::class.java).toList()
            return ids.mapNotNull { repository.getById(it) }
        }
        val bank = repository.getQuestions(limit = 1000)
        val excluidas = simulationRepository.getQuestionIdsFromRecentSimulacros(
            QuestionSelector.SIMULACROS_RECIENTES
        )
        return selector.select(bank, excluidas).fold(
            onSuccess = { seleccion ->
                savedStateHandle[SELECTION_KEY] = gson.toJson(seleccion.map { it.id })
                seleccion
            },
            onFailure = { emptyList() }
        )
    }

    private fun startTimer() {
        val startTime = System.currentTimeMillis()
        savedStateHandle[START_TIME_KEY] = startTime
        savedStateHandle[CURRENT_INDEX_KEY] = 0

        timerJob?.cancel()
        if (!relojActivo) {
            _uiState.value = _uiState.value.copy(timeRemainingMs = duracionSimulacroMs)
            return
        }
        timerJob = viewModelScope.launch {
            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                val remaining = maxOf(0L, duracionSimulacroMs - elapsed)

                val currentState = _uiState.value
                _uiState.value = currentState.copy(
                    timeRemainingMs = remaining,
                    isFinished = remaining == 0L && !currentState.isFinished
                )

                if (remaining == 0L) {
                    val finalState = _uiState.value
                    // Persistir el resultado antes de marcar como finalizado:
                    // si no, un simulacro que se agota por tiempo no se guarda.
                    saveSimulationResults(finalState)
                    _uiState.value = finalState.copy(isFinished = true)
                    break
                }
                delay(1000)
            }
        }
    }

    private fun resumeTimer() {
        val startTime = savedStateHandle.get<Long>(START_TIME_KEY) ?: System.currentTimeMillis()
        val elapsed = System.currentTimeMillis() - startTime
        val remaining = maxOf(0L, duracionSimulacroMs - elapsed)

        timerJob?.cancel()
        if (!relojActivo) {
            _uiState.value = _uiState.value.copy(timeRemainingMs = remaining)
            return
        }
        timerJob = viewModelScope.launch {
            while (true) {
                val currentElapsed = System.currentTimeMillis() - startTime
                val currentRemaining = maxOf(0L, duracionSimulacroMs - currentElapsed)

                val currentState = _uiState.value
                // No se limpia `showResumeOverlay` aquí: el overlay debe
                // permanecer hasta que el usuario pulse «Resumir».
                _uiState.value = currentState.copy(
                    timeRemainingMs = currentRemaining,
                    isFinished = currentRemaining == 0L && !currentState.isFinished
                )

                if (currentRemaining == 0L) {
                    val finalState = _uiState.value
                    saveSimulationResults(finalState)
                    _uiState.value = finalState.copy(isFinished = true)
                    break
                }
                delay(1000)
            }
        }
    }

    val currentQuestion: Question?
        get() {
            val state = uiState.value
            if (state.questions.isEmpty() || state.currentIndex >= state.questions.size) return null
            return state.questions[state.currentIndex]
        }

    fun onOptionSelected(option: String) {
        val currentState = _uiState.value
        val newAnswers = currentState.userAnswers.toMutableMap()
        newAnswers[currentState.currentIndex] = option
        saveAnswers(newAnswers)
        _uiState.value = currentState.copy(userAnswers = newAnswers.toMap())
    }

    fun onNextOrFinish() {
        val currentState = _uiState.value
        if (currentState.isLastQuestion) {
            saveSimulationResults(currentState)
            _uiState.value = currentState.copy(isFinished = true)
        } else {
            val newIndex = currentState.currentIndex + 1
            savedStateHandle[CURRENT_INDEX_KEY] = newIndex
            _uiState.value = currentState.copy(currentIndex = newIndex)
        }
    }

    private fun saveSimulationResults(state: SimulacroState) {
        val questions = state.questions
        val userAnswers = state.userAnswers
        val score = questions.mapIndexed { index, question ->
            val userAnswer = userAnswers[index]
            if (userAnswer == question.respuestaCorrecta) 1 else 0
        }.sum()
        val total = questions.size
        val timeUsedMs = duracionSimulacroMs - state.timeRemainingMs

        savedStateHandle["simulation_score"] = score
        savedStateHandle["simulation_total"] = total
        savedStateHandle["simulation_time_used_ms"] = timeUsedMs
        savedStateHandle["simulation_finished"] = true
        // Store userAnswers as JSON string to avoid type erasure issues
        savedStateHandle["simulation_user_answers_json"] = gson.toJson(userAnswers)
        // Preguntas seleccionadas para la pantalla de resultados
        savedStateHandle["simulation_questions_json"] = gson.toJson(questions)
    }

    // ----- Pausa / persistencia al cambiar de app -----

    /**
     * La app pasó a segundo plano. En el modo simulacro el cronómetro
     * **sigue corriendo** (examen realista); solo marcamos que hubo un
     * background real para mostrar el overlay al volver.
     */
    fun onAppBackgrounded() {
        val state = _uiState.value
        if (state.questions.isEmpty() || state.isFinished) return
        pausadoPorBackground = true
    }

    /** La app volvió a primer plano: mostrar el overlay de pausa. */
    fun onAppResumed() {
        val state = _uiState.value
        if (!pausadoPorBackground || state.isFinished || state.questions.isEmpty()) return
        pausadoPorBackground = false
        _uiState.value = state.copy(showResumeOverlay = true)
    }

    /** El usuario pulsó atrás: abrir el overlay en vez de salir. */
    fun onPauseRequested() {
        val state = _uiState.value
        if (state.questions.isEmpty() || state.isFinished) return
        _uiState.value = state.copy(showResumeOverlay = true)
    }

    fun onResumeConfirmed() {
        pausadoPorBackground = false
        _uiState.value = _uiState.value.copy(showResumeOverlay = false)
        // El cronómetro sigue activo en segundo plano; si el proceso fue
        // recreado y aún no late, se reanuda.
        if (timerJob?.isActive != true) {
            resumeTimer()
        }
    }

    /**
     * El usuario abandona el simulacro (botón «Salir» de la pausa): se
     * detiene el cronómetro y se oculta el overlay para que ningún tick
     * tardío marque el simulacro como finalizado ni navegue a Resultado
     * mientras la entrada ya está saliendo de la pila.
     */
    fun onAbandonar() {
        timerJob?.cancel()
        timerJob = null
        pausadoPorBackground = false
        _uiState.value = _uiState.value.copy(showResumeOverlay = false)
    }

    private fun saveAnswers(answers: MutableMap<Int, String>) {
        savedStateHandle[USER_ANSWERS_KEY] = answers
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }
}