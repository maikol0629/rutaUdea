package com.udea.rutaudea.data.source.mvp

import com.udea.rutaudea.data.repository.QuestionRepository
import com.udea.rutaudea.domain.model.Question
import kotlinx.coroutines.flow.first

class MvpQuestionProvider(private val repository: QuestionRepository) {

    // IDs de las 6 preguntas seleccionadas para el MVP
    // 3 Razonamiento Lógico (baja dificultad: series, porcentajes, posibilidades lógicas)
    // 3 Competencia Lectora (con texto_base)
    private val mvpQuestionIds = setOf(
        "profe_alex_4",   // RL - Series - baja
        "profe_alex_7",   // RL - Porcentajes - baja
        "profe_alex_23",  // RL - Posibilidades lógicas - baja
        "CL-ORG-001",     // CL - CL11 Supuestos - media (con texto_base)
        "CL-ORG-002",     // CL - CL02 Inferencia - media (con texto_base)
        "CL-ORG-007"      // CL - CL03 Idea principal - media (con texto_base)
    )

    suspend fun getMvpQuestions(): List<Question> {
        val allQuestions = repository.getQuestions(limit = 200) // Obtener todas las preguntas disponibles
        return allQuestions
            .filter { mvpQuestionIds.contains(it.id) }
            .sortedBy { mvpQuestionIds.toList().indexOf(it.id) } // Mantener orden específico
    }

    suspend fun getMvpQuestionsByArea(area: String): List<Question> {
        val questions = repository.getQuestions(area = area, limit = 100)
        return questions
            .filter { mvpQuestionIds.contains(it.id) }
            .sortedBy { mvpQuestionIds.toList().indexOf(it.id) }
    }
}