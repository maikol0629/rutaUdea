package com.udea.rutaudea.ui.screen.resultado

import com.udea.rutaudea.domain.model.ImagenPregunta

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
    val textoApoyo: String? = null,
    /** Figuras del contexto y de la pregunta (D-05) para releer al repasar. */
    val imagenesContexto: List<ImagenPregunta> = emptyList(),
    val imagenesPregunta: List<ImagenPregunta> = emptyList()
)