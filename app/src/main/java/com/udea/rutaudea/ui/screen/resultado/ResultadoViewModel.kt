package com.udea.rutaudea.ui.screen.resultado

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udea.rutaudea.data.source.mvp.MvpQuestionProvider
import com.udea.rutaudea.domain.model.Question
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ResultadoViewModel(
    private val mvpProvider: MvpQuestionProvider,
    private val userAnswers: Map<Int, String>,
    score: Int,
    total: Int,
    timeUsedMs: Long
) : ViewModel() {

    private val _questionResults = MutableStateFlow<List<QuestionResult>>(emptyList())
    val questionResults: StateFlow<List<QuestionResult>> = _questionResults

    private val _score = MutableStateFlow(score)
    val score: StateFlow<Int> = _score

    private val _total = MutableStateFlow(total)
    val total: StateFlow<Int> = _total

    private val _timeUsedMs = MutableStateFlow(timeUsedMs)
    val timeUsedMs: StateFlow<Long> = _timeUsedMs

    init {
        loadResults()
    }

    private fun loadResults() {
        viewModelScope.launch {
            val questions = mvpProvider.getMvpQuestions()

            val results = questions.mapIndexed { index, question ->
                val userAnswer = userAnswers[index]
                val isCorrect = userAnswer == question.respuestaCorrecta
                QuestionResult(
                    index = index,
                    areaLabel = if (question.area == "razonamiento_logico") "RL" else "CL",
                    isCorrect = isCorrect,
                    userAnswer = userAnswer ?: "—",
                    correctAnswer = question.respuestaCorrecta,
                    explanation = question.explicacion ?: ""
                )
            }

            _questionResults.value = results
        }
    }
}