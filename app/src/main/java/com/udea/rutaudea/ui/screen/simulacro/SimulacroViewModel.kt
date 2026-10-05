package com.udea.rutaudea.ui.screen.simulacro

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.udea.rutaudea.data.repository.QuestionRepository
import com.udea.rutaudea.data.repository.SimulationRepository
import com.udea.rutaudea.domain.model.Question
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
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val START_TIME_KEY = "start_time_ms"
        private const val USER_ANSWERS_KEY = "user_answers"
        private const val CURRENT_INDEX_KEY = "current_index"
        private const val SELECTION_KEY = "selection_ids_json"
        private const val TOTAL_DURATION_MS = 120 * 60 * 1000L // 120 minutos
        private val gson = Gson()
    }

    private val _uiState: MutableStateFlow<SimulacroState> = MutableStateFlow(SimulacroState())
    val uiState: StateFlow<SimulacroState> = _uiState

    private var timerJob: kotlinx.coroutines.Job? = null
    private var pendingRestoredIndex: Int = 0

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
            val remainingMs = maxOf(0L, TOTAL_DURATION_MS - elapsed)

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
            val questions = cargarSeleccion()
            if (questions.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(
                    questions = questions,
                    currentIndex = pendingRestoredIndex.coerceAtMost(questions.lastIndex)
                )
                if (!savedStateHandle.contains(START_TIME_KEY)) {
                    startTimer()
                } else {
                    val state = _uiState.value
                    if (state.timeRemainingMs > 0 && !state.isFinished) {
                        resumeTimer()
                    }
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
        timerJob = viewModelScope.launch {
            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                val remaining = maxOf(0L, TOTAL_DURATION_MS - elapsed)

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
        val remaining = maxOf(0L, TOTAL_DURATION_MS - elapsed)

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val currentElapsed = System.currentTimeMillis() - startTime
                val currentRemaining = maxOf(0L, TOTAL_DURATION_MS - currentElapsed)

                val currentState = _uiState.value
                _uiState.value = currentState.copy(
                    timeRemainingMs = currentRemaining,
                    showResumeOverlay = false,
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
        val timeUsedMs = TOTAL_DURATION_MS - state.timeRemainingMs

        savedStateHandle["simulation_score"] = score
        savedStateHandle["simulation_total"] = total
        savedStateHandle["simulation_time_used_ms"] = timeUsedMs
        savedStateHandle["simulation_finished"] = true
        // Store userAnswers as JSON string to avoid type erasure issues
        savedStateHandle["simulation_user_answers_json"] = gson.toJson(userAnswers)
        // Preguntas seleccionadas para la pantalla de resultados
        savedStateHandle["simulation_questions_json"] = gson.toJson(questions)
    }

    fun onResumeConfirmed() {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(showResumeOverlay = false)
        resumeTimer()
    }

    private fun saveAnswers(answers: MutableMap<Int, String>) {
        savedStateHandle[USER_ANSWERS_KEY] = answers
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }
}