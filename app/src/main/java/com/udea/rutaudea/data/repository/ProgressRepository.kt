package com.udea.rutaudea.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.udea.rutaudea.data.local.dao.ProgressDao
import com.udea.rutaudea.data.local.entity.PracticeQuestionEntity
import com.udea.rutaudea.data.local.entity.PracticeSummaryEntity
import com.udea.rutaudea.data.local.entity.SimulationQuestionEntity
import com.udea.rutaudea.data.local.entity.SimulationSummaryEntity
import com.udea.rutaudea.data.mapper.ProgressMapper
import com.udea.rutaudea.domain.model.PracticaResumen
import com.udea.rutaudea.domain.model.SimulacroResumen
import com.udea.rutaudea.domain.model.SubtemaEstadistica
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Punto único de lectura del historial y la analítica del módulo de
 * Progreso.
 *
 * Lee **siempre de Room** (offline-first, rápido y sin depender de
 * índices de Firestore) y ofrece [sincronizar] para subir las sesiones
 * pendientes y descargar las que existan en la nube.
 */
class ProgressRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val progressDao: ProgressDao,
    private val simulationRepository: SimulationRepository,
    private val practiceRepository: PracticeRepository
) {

    /** Últimos [limite] simulacros del usuario, del más reciente al más antiguo. */
    suspend fun cargarSimulacros(limite: Int = LIMITE_SIMULACROS): List<SimulacroResumen> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        return try {
            ProgressMapper.listSimulacrosToDomain(progressDao.getSimulacros(uid, limite))
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo leer el historial de simulacros local", e)
            emptyList()
        }
    }

    /** Últimas [limite] sesiones de práctica del usuario, de la más reciente a la más antigua. */
    suspend fun cargarPracticas(limite: Int = LIMITE_PRACTICAS): List<PracticaResumen> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        return try {
            ProgressMapper.listPracticasToDomain(progressDao.getPracticas(uid, limite))
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo leer el historial de prácticas local", e)
            emptyList()
        }
    }

    /**
     * Acierto acumulado por subtema a partir del detalle por pregunta de
     * los simulacros y prácticas indicados. Solo cuenta preguntas
     * efectivamente respondidas (las omitidas no afectan el indicador).
     */
    suspend fun cargarEstadisticasPorSubtema(
        simulacros: List<SimulacroResumen>,
        practicas: List<PracticaResumen>
    ): List<SubtemaEstadistica> {
        return try {
            val simIds = simulacros.map { it.id }
            val pracIds = practicas.map { it.id }
            val simPreguntas = if (simIds.isEmpty()) emptyList()
            else progressDao.getSimulationQuestions(simIds)
            val pracPreguntas = if (pracIds.isEmpty()) emptyList()
            else progressDao.getPracticeQuestions(pracIds)

            val conteos = mutableMapOf<String, Pair<Int, Int>>() // subtema -> (respondidas, correctas)
            simPreguntas.forEach { contar(conteos, it.subtema, it.respuestaUsuario, it.esCorrecta) }
            pracPreguntas.forEach { contar(conteos, it.subtema, it.respuestaUsuario, it.esCorrecta) }

            conteos.map { (subtema, par) ->
                SubtemaEstadistica(subtema = subtema, respondidas = par.first, correctas = par.second)
            }
        } catch (e: Exception) {
            Log.e(TAG, "No se pudieron calcular las estadísticas por subtema", e)
            emptyList()
        }
    }

    private fun contar(
        conteos: MutableMap<String, Pair<Int, Int>>,
        subtema: String?,
        respuestaUsuario: String,
        esCorrecta: Boolean
    ) {
        if (subtema.isNullOrBlank() || respuestaUsuario.isBlank()) return
        val (resp, ok) = conteos[subtema] ?: (0 to 0)
        conteos[subtema] = (resp + 1) to (ok + if (esCorrecta) 1 else 0)
    }

    /**
     * Sincroniza en ambos sentidos:
     * 1. Sube a Firestore las sesiones locales pendientes.
     * 2. Descarga de Firestore las sesiones más recientes y las cachea.
     *
     * Nunca lanza: si no hay red o falla Firestore, deja intacta la cache.
     * Devuelve `true` si la operación con la nube fue correcta.
     */
    suspend fun sincronizar(): Boolean = withContext(Dispatchers.IO + NonCancellable) {
        val uid = auth.currentUser?.uid ?: return@withContext false
        var ok = true
        try {
            if (!simulationRepository.sincronizarPendientes(uid)) ok = false
            if (!practiceRepository.sincronizarPendientes(uid)) ok = false

            descargarSimulacros(uid)
            descargarPracticas(uid)
        } catch (e: Exception) {
            Log.e(TAG, "Error al sincronizar el módulo de Progreso", e)
            ok = false
        }
        ok
    }

    private suspend fun descargarSimulacros(uid: String) {
        val documentos = consultarSesiones(
            coleccion = SimulationRepository.COLECCION_SIMULATIONS,
            uid = uid,
            campoFecha = SimulationRepository.CAMPO_FECHA
        )

        val resumenes = documentos.mapNotNull { doc ->
            val total = doc.getLong(SimulationRepository.CAMPO_TOTAL)?.toInt()
                ?: return@mapNotNull null
            SimulationSummaryEntity(
                id = doc.id,
                uid = uid,
                fechaMs = doc.getLong(SimulationRepository.CAMPO_FECHA) ?: 0L,
                score = doc.getLong(SimulationRepository.CAMPO_SCORE)?.toInt() ?: 0,
                total = total,
                tiempoUsedMs = doc.getLong(SimulationRepository.CAMPO_TIEMPO_MS) ?: 0L,
                respondidas = doc.getLong(SimulationRepository.CAMPO_RESPONDIDAS)?.toInt() ?: 0,
                omitidas = doc.getLong(SimulationRepository.CAMPO_OMITIDAS)?.toInt() ?: 0,
                synced = true
            )
        }
        if (resumenes.isNotEmpty()) progressDao.upsertSimulacros(resumenes)

        // Descarga el detalle solo de las sesiones que aún no están cacheadas.
        documentos.forEach { doc ->
            if (progressDao.countSimulationQuestions(doc.id) > 0) return@forEach
            val detalle = doc.reference.collection(SimulationRepository.COLECCION_SIM_QUESTIONS)
                .get()
                .await()
            val preguntas = detalle.documents.map { q ->
                val respuestaUsuario = q.getString(SimulationRepository.CAMPO_RESPUESTA_USUARIO).orEmpty()
                SimulationQuestionEntity(
                    sessionId = doc.id,
                    questionId = q.id,
                    indice = q.getLong(SimulationRepository.CAMPO_INDICE)?.toInt() ?: 0,
                    area = q.getString(SimulationRepository.CAMPO_AREA).orEmpty(),
                    subtema = q.getString(SimulationRepository.CAMPO_SUBTEMA).orEmpty(),
                    dificultad = q.getString(SimulationRepository.CAMPO_DIFICULTAD).orEmpty(),
                    respuestaUsuario = respuestaUsuario,
                    respuestaCorrecta = q.getString(SimulationRepository.CAMPO_RESPUESTA_CORRECTA).orEmpty(),
                    esCorrecta = q.getBoolean(SimulationRepository.CAMPO_ES_CORRECTA) ?: false
                )
            }
            if (preguntas.isNotEmpty()) progressDao.upsertSimulationQuestions(preguntas)
        }
    }

    private suspend fun descargarPracticas(uid: String) {
        val documentos = consultarSesiones(
            coleccion = PracticeRepository.COLECCION_PRACTICE_SESSIONS,
            uid = uid,
            campoFecha = PracticeRepository.CAMPO_FECHA
        )

        val resumenes = documentos.mapNotNull { doc ->
            val total = doc.getLong(PracticeRepository.CAMPO_TOTAL)?.toInt()
                ?: return@mapNotNull null
            PracticeSummaryEntity(
                id = doc.id,
                uid = uid,
                fechaMs = doc.getLong(PracticeRepository.CAMPO_FECHA) ?: 0L,
                area = doc.getString(PracticeRepository.CAMPO_AREA).orEmpty(),
                subtema = doc.getString(PracticeRepository.CAMPO_SUBTEMA)?.ifBlank { null },
                score = doc.getLong(PracticeRepository.CAMPO_SCORE)?.toInt() ?: 0,
                total = total,
                tiempoUsedMs = doc.getLong(PracticeRepository.CAMPO_TIEMPO_MS) ?: 0L,
                respondidas = doc.getLong(PracticeRepository.CAMPO_RESPONDIDAS)?.toInt() ?: 0,
                omitidas = doc.getLong(PracticeRepository.CAMPO_OMITIDAS)?.toInt() ?: 0,
                synced = true
            )
        }
        if (resumenes.isNotEmpty()) progressDao.upsertPracticas(resumenes)

        documentos.forEach { doc ->
            if (progressDao.countPracticeQuestions(doc.id) > 0) return@forEach
            val detalle = doc.reference.collection(PracticeRepository.COLECCION_PRACTICE_QUESTIONS)
                .get()
                .await()
            val preguntas = detalle.documents.map { q ->
                val respuestaUsuario = q.getString(PracticeRepository.CAMPO_RESPUESTA_USUARIO).orEmpty()
                PracticeQuestionEntity(
                    sessionId = doc.id,
                    questionId = q.id,
                    indice = q.getLong(PracticeRepository.CAMPO_INDICE)?.toInt() ?: 0,
                    subtema = q.getString(PracticeRepository.CAMPO_SUBTEMA).orEmpty(),
                    dificultad = q.getString(PracticeRepository.CAMPO_DIFICULTAD).orEmpty(),
                    respuestaUsuario = respuestaUsuario,
                    respuestaCorrecta = q.getString(PracticeRepository.CAMPO_RESPUESTA_CORRECTA).orEmpty(),
                    esCorrecta = q.getBoolean(PracticeRepository.CAMPO_ES_CORRECTA) ?: false
                )
            }
            if (preguntas.isNotEmpty()) progressDao.upsertPracticeQuestions(preguntas)
        }
    }

    /**
     * Consulta las sesiones del usuario ordenadas por fecha descendente.
     * Si el índice compuesto `uid + fechaCreacion` aún no existe en
     * Firestore, reintenta sin `orderBy` y ordena en memoria.
     */
    private suspend fun consultarSesiones(
        coleccion: String,
        uid: String,
        campoFecha: String
    ): List<DocumentSnapshot> {
        return try {
            firestore.collection(coleccion)
                .whereEqualTo(CAMPO_UID, uid)
                .orderBy(campoFecha, Query.Direction.DESCENDING)
                .limit(LIMITE_SINCRONIZACION.toLong())
                .get()
                .await()
                .documents
        } catch (e: Exception) {
            Log.w(TAG, "Consulta con índice falló para $coleccion; usando fallback sin orderBy", e)
            firestore.collection(coleccion)
                .whereEqualTo(CAMPO_UID, uid)
                .limit(LIMITE_SINCRONIZACION.toLong())
                .get()
                .await()
                .documents
                .sortedByDescending { it.getLong(campoFecha) ?: 0L }
        }
    }

    companion object {
        private const val TAG = "ProgressRepository"
        const val CAMPO_UID = "uid"

        /** Cuántas sesiones se muestran/analizan en el dashboard. */
        const val LIMITE_SIMULACROS = 20
        const val LIMITE_PRACTICAS = 30

        /** Cuántas sesiones recientes se descargan de la nube al sincronizar. */
        const val LIMITE_SINCRONIZACION = 50
    }
}
