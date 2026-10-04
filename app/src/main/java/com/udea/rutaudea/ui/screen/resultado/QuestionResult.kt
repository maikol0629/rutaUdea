package com.udea.rutaudea.ui.screen.resultado

data class QuestionResult(
    val index: Int,
    val areaLabel: String,
    val isCorrect: Boolean,
    val userAnswer: String,
    val correctAnswer: String,
    val explanation: String,
    /** Enunciado de la pregunta, para saber sobre qué se está corrigiendo. */
    val enunciado: String = "",
    /** Texto de apoyo de la pregunta (CL), si existe, para releer al repasar. */
    val textoApoyo: String? = null
)