package com.udea.rutaudea.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.udea.rutaudea.data.local.dao.ProgressDao
import com.udea.rutaudea.data.local.entity.SimulationQuestionEntity
import com.udea.rutaudea.data.local.entity.SimulationSummaryEntity
import com.udea.rutaudea.domain.model.Question
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Punto único de acceso al registro de simulacros.
 *
 * Estrategia **local-first**: el resumen y el detalle se guardan primero
 * en Room (funciona sin conexión y no se pierde al navegar), y luego se
 * suben a Firestore (`simulations` + `simulationQuestions`). Si la subida
 * falla, la sesión queda marcada como pendiente y se reintenta en la
 * siguiente sincronización.
 *
 * Si el usuario no tiene sesión iniciada, el registro se omite.
 */
class SimulationRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val progressDao: ProgressDao
) {

    /**
     * Registra un simulacro completado: primero en Room y después en
     * Firestore (best-effort). Devuelve el id de la sesión.
     */
    suspend fun saveSimulation(
        questions: List<Question>,
        userAnswers: Map<Int, String>,
        score: Int,
        total: Int,
        timeUsedMs: Long
    ): Result<String> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(Exception("Sesión no iniciada: el resultado no se guarda"))
        if (questions.isEmpty()) {
            return Result.failure(Exception("No hay preguntas para registrar"))
        }

        val id = UUID.randomUUID().toString()
        val fechaMs = System.currentTimeMillis()
        val respondidas = userAnswers.size
        val omitidas = total - respondidas

        val resumen = SimulationSummaryEntity(
            id = id,
            uid = uid,
            fechaMs = fechaMs,
            score = score,
            total = total,
            tiempoUsedMs = timeUsedMs,
            respondidas = respondidas,
            omitidas = omitidas,
            synced = false
        )
        val detalles = questions.mapIndexed { index, question ->
            val userAnswer = userAnswers[index].orEmpty()
            SimulationQuestionEntity(
                sessionId = id,
                questionId = question.id,
                indice = index,
                area = question.area,
                subtema = question.subtema,
                dificultad = question.dificultad,
                respuestaUsuario = userAnswer,
                respuestaCorrecta = question.respuestaCorrecta,
                esCorrecta = userAnswer == question.respuestaCorrecta
            )
        }

        return withContext(Dispatchers.IO + NonCancellable) {
            try {
                progressDao.upsertSimulacros(listOf(resumen))
                progressDao.upsertSimulationQuestions(detalles)
                // Intento de subida a la nube (no bloquea el guardado local).
                subirSimulacro(resumen, detalles)
                Result.success(id)
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo guardar el simulacro localmente", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Sube un simulacro a Firestore. Si tiene éxito, marca la sesión como
     * sincronizada en Room. Devuelve `true` si la nube quedó actualizada.
     */
    private suspend fun subirSimulacro(
        resumen: SimulationSummaryEntity,
        detalles: List<SimulationQuestionEntity>
    ): Boolean {
        return try {
            val simDoc = firestore.collection(COLECCION_SIMULATIONS).document(resumen.id)
            simDoc.set(
                mapOf(
                    CAMPO_UID to resumen.uid,
                    CAMPO_FECHA to resumen.fechaMs,
                    CAMPO_SCORE to resumen.score,
                    CAMPO_TOTAL to resumen.total,
                    CAMPO_TIEMPO_MS to resumen.tiempoUsedMs,
                    CAMPO_RESPONDIDAS to resumen.respondidas,
                    CAMPO_OMITIDAS to resumen.omitidas,
                    CAMPO_ESTADO to ESTADO_COMPLETADO,
                    CAMPO_AREAS to detalles.map { it.area }.distinct()
                )
            ).await()

            firestore.runBatch { batch ->
                detalles.forEach { detalle ->
                    val ref = simDoc.collection(COLECCION_SIM_QUESTIONS).document(detalle.questionId)
                    batch.set(
                        ref,
                        mapOf(
                            CAMPO_INDICE to detalle.indice,
                            CAMPO_AREA to detalle.area,
                            CAMPO_SUBTEMA to detalle.subtema,
                            CAMPO_DIFICULTAD to detalle.dificultad,
                            CAMPO_RESPUESTA_USUARIO to detalle.respuestaUsuario,
                            CAMPO_RESPUESTA_CORRECTA to detalle.respuestaCorrecta,
                            CAMPO_ES_CORRECTA to detalle.esCorrecta,
                            CAMPO_TIEMPO_MS to 0L
                        )
                    )
                }
            }.await()

            progressDao.marcarSimulacrosSincronizados(listOf(resumen.id))
            true
        } catch (e: Exception) {
            Log.w(TAG, "Simulacro ${resumen.id} guardado en local; pendiente de sincronizar", e)
            false
        }
    }

    /**
     * Reintenta subir los simulacros locales que quedaron pendientes.
     * Devuelve `true` si todos terminaron sincronizados.
     */
    suspend fun sincronizarPendientes(uid: String): Boolean {
        val pendientes = try {
            progressDao.getSimulacrosPendientes(uid)
        } catch (e: Exception) {
            Log.e(TAG, "No se pudieron leer los simulacros pendientes", e)
            return false
        }
        if (pendientes.isEmpty()) return true

        var ok = true
        pendientes.forEach { resumen ->
            val detalles = try {
                progressDao.getSimulationQuestions(listOf(resumen.id))
            } catch (e: Exception) {
                emptyList()
            }
            if (!subirSimulacro(resumen, detalles)) ok = false
        }
        return ok
    }

    /**
     * IDs de las preguntas usadas en los últimos [cantidad] simulacros
     * del usuario (motor de selección). Se lee de la cache local; si está
     * vacía se consulta Firestore (requiere el índice uid+fechaCreacion).
     */
    suspend fun getQuestionIdsFromRecentSimulacros(cantidad: Int): Set<String> {
        val uid = auth.currentUser?.uid ?: return emptySet()

        val locales = try {
            val ids = progressDao.getSimulacros(uid, cantidad).map { it.id }
            if (ids.isEmpty()) emptySet() else {
                progressDao.getSimulationQuestions(ids).map { it.questionId }.toSet()
            }
        } catch (e: Exception) {
            emptySet()
        }
        if (locales.isNotEmpty()) return locales

        return try {
            val sims = firestore.collection(COLECCION_SIMULATIONS)
                .whereEqualTo(CAMPO_UID, uid)
                .orderBy(CAMPO_FECHA, Query.Direction.DESCENDING)
                .limit(cantidad.toLong())
                .get()
                .await()
            val ids = mutableSetOf<String>()
            for (sim in sims.documents) {
                sim.reference.collection(COLECCION_SIM_QUESTIONS)
                    .get()
                    .await()
                    .documents
                    .forEach { ids.add(it.id) }
            }
            ids
        } catch (e: Exception) {
            Log.w(TAG, "Sin IDs de simulacros recientes", e)
            emptySet()
        }
    }

    companion object {
        private const val TAG = "SimulationRepository"
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
