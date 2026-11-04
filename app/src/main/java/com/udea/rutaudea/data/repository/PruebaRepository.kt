package com.udea.rutaudea.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.udea.rutaudea.data.local.dao.ProgressDao
import com.udea.rutaudea.data.local.entity.PruebaQuestionEntity
import com.udea.rutaudea.data.local.entity.PruebaSummaryEntity
import com.udea.rutaudea.domain.model.Question
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Punto único de acceso al registro de las pruebas cortas "Pruébate".
 *
 * Estrategia **local-first**: el resumen y el detalle se guardan primero
 * en Room (funciona sin conexión y no se pierde al navegar), y luego se
 * suben a Firestore (`pruebas` + `pruebaQuestions`). Si la subida falla,
 * la sesión queda marcada como pendiente y se reintenta en la siguiente
 * sincronización.
 *
 * Las pruebas viven en colecciones propias, separadas de `simulations`,
 * para que las métricas de Progreso no mezclen ambos tipos.
 */
class PruebaRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val progressDao: ProgressDao
) {

    /**
     * Registra una prueba completada: primero en Room y después en
     * Firestore (best-effort). Devuelve el id de la sesión.
     */
    suspend fun savePrueba(
        questions: List<Question>,
        userAnswers: Map<Int, String>,
        score: Int,
        total: Int,
        timeUsedMs: Long,
        /** Reutiliza el id de la sesión para enlazar los reportes de pregunta. */
        sessionId: String? = null
    ): Result<String> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(Exception("Sesión no iniciada: el resultado no se guarda"))
        if (questions.isEmpty()) {
            return Result.failure(Exception("No hay preguntas para registrar"))
        }

        val datos = DatosResumenSesion.crear(
            total = total,
            respondidas = userAnswers.size,
            sessionId = sessionId
        )

        val resumen = PruebaSummaryEntity(
            id = datos.id,
            uid = uid,
            fechaMs = datos.fechaMs,
            score = score,
            total = total,
            tiempoUsedMs = timeUsedMs,
            respondidas = datos.respondidas,
            omitidas = datos.omitidas,
            synced = false
        )
        val detalles = questions.mapIndexed { index, question ->
            val userAnswer = userAnswers[index].orEmpty()
            PruebaQuestionEntity(
                sessionId = datos.id,
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
            // Idempotencia: si la prueba ya se registró (por ejemplo, al
            // reentrar a Resultado), no se vuelve a guardar ni a subir.
            if (sessionId != null && progressDao.countPruebasById(sessionId) > 0) {
                return@withContext Result.success(sessionId)
            }
            try {
                progressDao.upsertPruebas(listOf(resumen))
                progressDao.upsertPruebaQuestions(detalles)
                // Intento de subida a la nube (no bloquea el guardado local).
                subirPrueba(resumen, detalles)
                Result.success(datos.id)
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo guardar la prueba localmente", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Sube una prueba a Firestore. Si tiene éxito, marca la sesión como
     * sincronizada en Room. Devuelve `true` si la nube quedó actualizada.
     */
    private suspend fun subirPrueba(
        resumen: PruebaSummaryEntity,
        detalles: List<PruebaQuestionEntity>
    ): Boolean {
        return try {
            val pruebaDoc = firestore.collection(COLECCION_PRUEBAS).document(resumen.id)
            pruebaDoc.set(
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
                    val ref = pruebaDoc.collection(COLECCION_PRUEBA_QUESTIONS)
                        .document(detalle.questionId)
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

            progressDao.marcarPruebasSincronizadas(listOf(resumen.id))
            true
        } catch (e: Exception) {
            Log.w(TAG, "Prueba ${resumen.id} guardada en local; pendiente de sincronizar", e)
            false
        }
    }

    /**
     * Reintenta subir las pruebas locales que quedaron pendientes.
     * Devuelve `true` si todas terminaron sincronizadas.
     */
    suspend fun sincronizarPendientes(uid: String): Boolean {
        val pendientes = try {
            progressDao.getPruebasPendientes(uid)
        } catch (e: Exception) {
            Log.e(TAG, "No se pudieron leer las pruebas pendientes", e)
            return false
        }
        if (pendientes.isEmpty()) return true

        var ok = true
        pendientes.forEach { resumen ->
            val detalles = try {
                progressDao.getPruebaQuestions(listOf(resumen.id))
            } catch (e: Exception) {
                emptyList()
            }
            if (!subirPrueba(resumen, detalles)) ok = false
        }
        return ok
    }

    /**
     * IDs de las preguntas usadas en las últimas [cantidad] pruebas del
     * usuario (motor de selección). Se lee de la cache local; si está
     * vacía se consulta Firestore (requiere el índice uid+fechaCreacion).
     */
    suspend fun getQuestionIdsFromRecentPruebas(cantidad: Int): Set<String> {
        val uid = auth.currentUser?.uid ?: return emptySet()

        val locales = try {
            val ids = progressDao.getPruebas(uid, cantidad).map { it.id }
            if (ids.isEmpty()) emptySet() else {
                progressDao.getPruebaQuestions(ids).map { it.questionId }.toSet()
            }
        } catch (e: Exception) {
            emptySet()
        }
        if (locales.isNotEmpty()) return locales

        return try {
            val pruebas = firestore.collection(COLECCION_PRUEBAS)
                .whereEqualTo(CAMPO_UID, uid)
                .orderBy(CAMPO_FECHA, Query.Direction.DESCENDING)
                .limit(cantidad.toLong())
                .get()
                .await()
            val ids = mutableSetOf<String>()
            for (prueba in pruebas.documents) {
                prueba.reference.collection(COLECCION_PRUEBA_QUESTIONS)
                    .get()
                    .await()
                    .documents
                    .forEach { ids.add(it.id) }
            }
            ids
        } catch (e: Exception) {
            Log.w(TAG, "Sin IDs de pruebas recientes", e)
            emptySet()
        }
    }

    companion object {
        private const val TAG = "PruebaRepository"
        const val COLECCION_PRUEBAS = "pruebas"
        const val COLECCION_PRUEBA_QUESTIONS = "pruebaQuestions"
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
