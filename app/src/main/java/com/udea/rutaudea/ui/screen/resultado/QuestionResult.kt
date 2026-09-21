package com.udea.rutaudea.ui.screen.resultado

data class QuestionResult(
    val index: Int,
    val areaLabel: String,
    val isCorrect: Boolean,
    val userAnswer: String,
    val correctAnswer: String,
    val explanation: String
)