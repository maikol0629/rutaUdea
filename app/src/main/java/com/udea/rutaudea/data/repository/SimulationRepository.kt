package com.udea.rutaudea.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.udea.rutaudea.domain.model.Question
import kotlinx.coroutines.tasks.await

/**
 * Punto único de acceso al registro de simulacros en Firestore
 * (colecciones `simulations` y `simulationQuestions` — plan, sección 7).
 *
 * Si el usuario no tiene sesión iniciada, el registro se omite
 * (el MVP funciona en local sin persistencia en la nube).
 */
class SimulationRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    /**
     * Registra un simulacro completado: documento en `simulations` con
     * resumen (score, tiempos, areas) y detalle por pregunta en la
     * subcolección `simulationQuestions`.
     */
    suspend fun saveSimulation(
        questions: List<Question>,
        userAnswers: Map<Int, String>,
        score: Int,
        total: Int,
        timeUsedMs: Long
    ): Result<String> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(Exception("Sesión no iniciada: el resultado no se guarda en la nube"))
        return try {
            val respondidas = userAnswers.size
            val omitidas = total - respondidas
            val simDoc = firestore.collection(COLECCION_SIMULATIONS).document()
            simDoc.set(
                mapOf(
                    CAMPO_UID to uid,
                    CAMPO_FECHA to System.currentTimeMillis(),
                    CAMPO_SCORE to score,
                    CAMPO_TOTAL to total,
                    CAMPO_TIEMPO_MS to timeUsedMs,
                    CAMPO_RESPONDIDAS to respondidas,
                    CAMPO_OMITIDAS to omitidas,
                    CAMPO_ESTADO to ESTADO_COMPLETADO,
                    CAMPO_AREAS to questions.map { it.area }.distinct()
                )
            ).await()

            questions.forEachIndexed { index, question ->
                val userAnswer = userAnswers[index]
                simDoc.collection(COLECCION_SIM_QUESTIONS).document(question.id).set(
                    mapOf(
                        CAMPO_INDICE to index,
                        CAMPO_AREA to question.area,
                        CAMPO_SUBTEMA to question.subtema,
                        CAMPO_DIFICULTAD to question.dificultad,
                        CAMPO_RESPUESTA_USUARIO to (userAnswer ?: ""),
                        CAMPO_RESPUESTA_CORRECTA to question.respuestaCorrecta,
                        CAMPO_ES_CORRECTA to (userAnswer == question.respuestaCorrecta),
                        CAMPO_TIEMPO_MS to 0L
                    )
                ).await()
            }
            Result.success(simDoc.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val COLECCION_SIMULATIONS = "simulations"
        const val COLECCION_SIM_QUESTIONS = "simulationQuestions"
        const val CAMPO_UID = "uid"
        const val CAMPO_FECHA = "fechaCreacion"
        const val CAMPO_SCORE = "score"
        const val CAMPO_TOTAL = "total"
        const val CAMPO_TIEMPO_MS = "tiempoUsedMs"
        const val CAMPO_RESPONDIDAS = "respondidas"
        const val CAMPO_OMITIDAS = "omitidas"
        const val CAMPO_ESTADO = "estado"
        const val CAMPO_AREAS = "areas"
        const val CAMPO_INDICE = "indice"
        const val CAMPO_AREA = "area"
        const val CAMPO_SUBTEMA = "subtema"
        const val CAMPO_DIFICULTAD = "dificultad"
        const val CAMPO_RESPUESTA_USUARIO = "respuestaUsuario"
        const val CAMPO_RESPUESTA_CORRECTA = "respuestaCorrecta"
        const val CAMPO_ES_CORRECTA = "esCorrecta"
        const val ESTADO_COMPLETADO = "completado"
    }
}
