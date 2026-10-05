package com.udea.rutaudea.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.udea.rutaudea.data.local.dao.ProgressDao
import com.udea.rutaudea.data.local.entity.PracticeQuestionEntity
import com.udea.rutaudea.data.local.entity.PracticeSummaryEntity
import com.udea.rutaudea.domain.model.Question
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Punto único de acceso al registro de sesiones de práctica.
 *
 * Estrategia **local-first**: resumen + detalle se escriben primero en
 * Room y luego se suben a Firestore (`practiceSessions` +
 * `practiceQuestions`). Si la subida falla, la sesión queda pendiente y
 * se reintenta en la próxima sincronización.
 */
class PracticeRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val progressDao: ProgressDao
) {

    /**
     * Registra una sesión de práctica completada (primero en Room, luego
     * en Firestore). Devuelve el id de la sesión.
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
            ?: return Result.failure(Exception("Sesión no iniciada: la práctica no se guarda"))
        if (questions.isEmpty()) {
            return Result.failure(Exception("No hay preguntas para registrar"))
        }

        val id = UUID.randomUUID().toString()
        val fechaMs = System.currentTimeMillis()
        val respondidas = userAnswers.size
        val omitidas = total - respondidas

        val resumen = PracticeSummaryEntity(
            id = id,
            uid = uid,
            fechaMs = fechaMs,
            area = area ?: "",
            subtema = subtema?.ifBlank { null },
            score = score,
            total = total,
            tiempoUsedMs = timeUsedMs,
            respondidas = respondidas,
            omitidas = omitidas,
            synced = false
        )
        val detalles = questions.mapIndexed { index, question ->
            val userAnswer = userAnswers[index].orEmpty()
            PracticeQuestionEntity(
                sessionId = id,
                questionId = question.id,
                indice = index,
                subtema = question.subtema,
                dificultad = question.dificultad,
                respuestaUsuario = userAnswer,
                respuestaCorrecta = question.respuestaCorrecta,
                esCorrecta = userAnswer == question.respuestaCorrecta
            )
        }

        return withContext(Dispatchers.IO + NonCancellable) {
            try {
                progressDao.upsertPracticas(listOf(resumen))
                progressDao.upsertPracticeQuestions(detalles)
                subirPractica(resumen, detalles)
                Result.success(id)
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo guardar la práctica localmente", e)
                Result.failure(e)
            }
        }
    }

    private suspend fun subirPractica(
        resumen: PracticeSummaryEntity,
        detalles: List<PracticeQuestionEntity>
    ): Boolean {
        return try {
            val sesionDoc = firestore.collection(COLECCION_PRACTICE_SESSIONS).document(resumen.id)
            sesionDoc.set(
                mapOf(
                    CAMPO_UID to resumen.uid,
                    CAMPO_FECHA to resumen.fechaMs,
                    CAMPO_AREA to resumen.area,
                    CAMPO_SUBTEMA to resumen.subtema.orEmpty(),
                    CAMPO_SCORE to resumen.score,
                    CAMPO_TOTAL to resumen.total,
                    CAMPO_TIEMPO_MS to resumen.tiempoUsedMs,
                    CAMPO_RESPONDIDAS to resumen.respondidas,
                    CAMPO_OMITIDAS to resumen.omitidas,
                    CAMPO_ESTADO to ESTADO_COMPLETADO
                )
            ).await()

            firestore.runBatch { batch ->
                detalles.forEach { detalle ->
                    val ref = sesionDoc.collection(COLECCION_PRACTICE_QUESTIONS)
                        .document(detalle.questionId)
                    batch.set(
                        ref,
                        mapOf(
                            CAMPO_INDICE to detalle.indice,
                            CAMPO_SUBTEMA to detalle.subtema,
                            CAMPO_DIFICULTAD to detalle.dificultad,
                            CAMPO_RESPUESTA_USUARIO to detalle.respuestaUsuario,
                            CAMPO_RESPUESTA_CORRECTA to detalle.respuestaCorrecta,
                            CAMPO_ES_CORRECTA to detalle.esCorrecta
                        )
                    )
                }
            }.await()

            progressDao.marcarPracticasSincronizadas(listOf(resumen.id))
            true
        } catch (e: Exception) {
            Log.w(TAG, "Práctica ${resumen.id} guardada en local; pendiente de sincronizar", e)
            false
        }
    }

    /**
     * Reintenta subir las prácticas locales que quedaron pendientes.
     * Devuelve `true` si todas terminaron sincronizadas.
     */
    suspend fun sincronizarPendientes(uid: String): Boolean {
        val pendientes = try {
            progressDao.getPracticasPendientes(uid)
        } catch (e: Exception) {
            Log.e(TAG, "No se pudieron leer las prácticas pendientes", e)
            return false
        }
        if (pendientes.isEmpty()) return true

        var ok = true
        pendientes.forEach { resumen ->
            val detalles = try {
                progressDao.getPracticeQuestions(listOf(resumen.id))
            } catch (e: Exception) {
                emptyList()
            }
            if (!subirPractica(resumen, detalles)) ok = false
        }
        return ok
    }

    companion object {
        private const val TAG = "PracticeRepository"
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
