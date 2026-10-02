package com.udea.rutaudea.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.udea.rutaudea.domain.model.Question
import kotlinx.coroutines.tasks.await

/**
 * Punto único de acceso al registro de sesiones de práctica en Firestore
 * (colección `practiceSessions` — plan, sección 7).
 *
 * La sesión guarda los filtros usados (área/subtema), el resumen del
 * desempeño y el detalle por pregunta en la subcolección `practiceQuestions`.
 * Estos datos alimentarán el módulo de Progreso.
 */
class PracticeRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    /**
     * Registra una sesión de práctica completada: documento en
     * `practiceSessions` con resumen (filtros, score, tiempo) y detalle
     * por pregunta en la subcolección `practiceQuestions`.
     */
    suspend fun savePracticeSession(
        area: String?,
        subtema: String?,
        questions: List<Question>,
        userAnswers: Map<Int, String>,
        score: Int,
        total: Int,
        timeUsedMs: Long
    ): Result<String> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(Exception("Sesión no iniciada: la práctica no se guarda en la nube"))
        if (questions.isEmpty()) {
            return Result.failure(Exception("No hay preguntas para registrar"))
        }
        return try {
            val respondidas = userAnswers.size
            val omitidas = total - respondidas
            val sesionDoc = firestore.collection(COLECCION_PRACTICE_SESSIONS).document()
            sesionDoc.set(
                mapOf(
                    CAMPO_UID to uid,
                    CAMPO_FECHA to System.currentTimeMillis(),
                    CAMPO_AREA to (area ?: ""),
                    CAMPO_SUBTEMA to (subtema ?: ""),
                    CAMPO_SCORE to score,
                    CAMPO_TOTAL to total,
                    CAMPO_TIEMPO_MS to timeUsedMs,
                    CAMPO_RESPONDIDAS to respondidas,
                    CAMPO_OMITIDAS to omitidas,
                    CAMPO_ESTADO to ESTADO_COMPLETADO
                )
            ).await()

            // Escritura del detalle por pregunta en batch (un solo round-trip).
            firestore.runBatch { batch ->
                questions.forEachIndexed { index, question ->
                    val detalle = sesionDoc.collection(COLECCION_PRACTICE_QUESTIONS)
                        .document(question.id)
                    val userAnswer = userAnswers[index]
                    batch.set(
                        detalle,
                        mapOf(
                            CAMPO_INDICE to index,
                            CAMPO_SUBTEMA to question.subtema,
                            CAMPO_DIFICULTAD to question.dificultad,
                            CAMPO_RESPUESTA_USUARIO to (userAnswer ?: ""),
                            CAMPO_RESPUESTA_CORRECTA to question.respuestaCorrecta,
                            CAMPO_ES_CORRECTA to (userAnswer == question.respuestaCorrecta)
                        )
                    )
                }
            }.await()

            Result.success(sesionDoc.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val COLECCION_PRACTICE_SESSIONS = "practiceSessions"
        const val COLECCION_PRACTICE_QUESTIONS = "practiceQuestions"
        const val CAMPO_UID = "uid"
        const val CAMPO_FECHA = "fechaCreacion"
        const val CAMPO_AREA = "area"
        const val CAMPO_SUBTEMA = "subtema"
        const val CAMPO_SCORE = "score"
        const val CAMPO_TOTAL = "total"
        const val CAMPO_TIEMPO_MS = "tiempoUsedMs"
        const val CAMPO_RESPONDIDAS = "respondidas"
        const val CAMPO_OMITIDAS = "omitidas"
        const val CAMPO_ESTADO = "estado"
        const val CAMPO_INDICE = "indice"
        const val CAMPO_DIFICULTAD = "dificultad"
        const val CAMPO_RESPUESTA_USUARIO = "respuestaUsuario"
        const val CAMPO_RESPUESTA_CORRECTA = "respuestaCorrecta"
        const val CAMPO_ES_CORRECTA = "esCorrecta"
        const val ESTADO_COMPLETADO = "completado"
    }
}
