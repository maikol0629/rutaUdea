package com.udea.rutaudea.ui.screen.simulacro

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.udea.rutaudea.data.source.mvp.MvpQuestionProvider
import com.udea.rutaudea.domain.model.Question
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
    private val mvpProvider: MvpQuestionProvider,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val START_TIME_KEY = "start_time_ms"
        private const val USER_ANSWERS_KEY = "user_answers"
        private const val CURRENT_INDEX_KEY = "current_index"
        private const val TOTAL_DURATION_MS = 5 * 60 * 1000L // 5 minutos
        private val gson = Gson()
    }

    private val _uiState: MutableStateFlow<SimulacroState> = MutableStateFlow(SimulacroState())
    val uiState: StateFlow<SimulacroState> = _uiState

    private var timerJob: kotlinx.coroutines.Job? = null

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

            val currentState = _uiState.value
            _uiState.value = currentState.copy(
                timeRemainingMs = remainingMs,
                userAnswers = savedAnswers.toMap(),
                currentIndex = savedIndex.coerceAtMost(currentState.questions.lastIndex),
                showResumeOverlay = remainingMs > 0L && !currentState.isFinished
            )
        }
    }

    private fun loadQuestions() {
        viewModelScope.launch {
            val questions = mvpProvider.getMvpQuestions()
            if (questions.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(questions = questions)
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