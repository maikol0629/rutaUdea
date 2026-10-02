package com.udea.rutaudea.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.udea.rutaudea.data.repository.PracticeRepository.Companion.COLECCION_PRACTICE_QUESTIONS
import com.udea.rutaudea.data.repository.PracticeRepository.Companion.COLECCION_PRACTICE_SESSIONS
import com.udea.rutaudea.data.repository.SimulationRepository.Companion.COLECCION_SIMULATIONS
import com.udea.rutaudea.data.repository.SimulationRepository.Companion.COLECCION_SIM_QUESTIONS
import com.udea.rutaudea.domain.model.PracticaResumen
import com.udea.rutaudea.domain.model.SimulacroResumen
import com.udea.rutaudea.domain.model.SubtemaEstadistica
import kotlinx.coroutines.tasks.await

/**
 * Punto único de lectura del historial y la analítica del usuario
 * (módulo de Progreso — plan, sección 12).
 *
 * Lee `simulations` y `practiceSessions` del usuario y agrega los
 * detalles por pregunta (`simulationQuestions` / `practiceQuestions`)
 * para calcular el acierto por subtema.
 *
 * Si no hay sesión o Firestore no está disponible, devuelve listas
 * vacías (el progreso es meramente informativo, nunca bloquea).
 */
class ProgressRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    /** Últimos [limite] simulacros del usuario, del más reciente al más antiguo. */
    suspend fun cargarSimulacros(limite: Int = LIMITE_SIMULACROS): List<SimulacroResumen> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        return try {
            val snapshot = firestore.collection(COLECCION_SIMULATIONS)
                .whereEqualTo(CAMPO_UID, uid)
                .orderBy(CAMPO_FECHA, Query.Direction.DESCENDING)
                .limit(limite.toLong())
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                val total = doc.getLong(CAMPO_TOTAL)?.toInt() ?: return@mapNotNull null
                SimulacroResumen(
                    id = doc.id,
                    fechaMs = doc.getLong(CAMPO_FECHA) ?: 0L,
                    score = doc.getLong(CAMPO_SCORE)?.toInt() ?: 0,
                    total = total,
                    tiempoUsedMs = doc.getLong(CAMPO_TIEMPO_MS) ?: 0L,
                    respondidas = doc.getLong(CAMPO_RESPONDIDAS)?.toInt() ?: 0,
                    omitidas = doc.getLong(CAMPO_OMITIDAS)?.toInt() ?: 0
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Últimas [limite] sesiones de práctica del usuario, de la más reciente a la más antigua. */
    suspend fun cargarPracticas(limite: Int = LIMITE_PRACTICAS): List<PracticaResumen> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        return try {
            val snapshot = firestore.collection(COLECCION_PRACTICE_SESSIONS)
                .whereEqualTo(CAMPO_UID, uid)
                .orderBy(CAMPO_FECHA, Query.Direction.DESCENDING)
                .limit(limite.toLong())
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                val total = doc.getLong(CAMPO_TOTAL)?.toInt() ?: return@mapNotNull null
                PracticaResumen(
                    id = doc.id,
                    fechaMs = doc.getLong(CAMPO_FECHA) ?: 0L,
                    area = doc.getString(CAMPO_AREA) ?: "razonamiento_logico",
                    subtema = doc.getString(CAMPO_SUBTEMA)?.ifBlank { null },
                    score = doc.getLong(CAMPO_SCORE)?.toInt() ?: 0,
                    total = total,
                    tiempoUsedMs = doc.getLong(CAMPO_TIEMPO_MS) ?: 0L
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Acierto acumulado por subtema a partir de los detalles por pregunta
     * de los simulacros y prácticas indicados. Solo cuenta preguntas
     * efectivamente respondidas (las omitidas no afectan el indicador).
     */
    suspend fun cargarEstadisticasPorSubtema(
        simulacros: List<SimulacroResumen>,
        practicas: List<PracticaResumen>
    ): List<SubtemaEstadistica> {
        val conteos = mutableMapOf<String, Pair<Int, Int>>() // subtema -> (respondidas, correctas)

        suspend fun agregar(coleccion: String, documentoId: String, subcoleccion: String) {
            try {
                val detalle = firestore.collection(coleccion)
                    .document(documentoId)
                    .collection(subcoleccion)
                    .get()
                    .await()
                detalle.documents.forEach { doc ->
                    val subtema = doc.getString(CAMPO_SUBTEMA) ?: return@forEach
                    val respondida = !doc.getString(CAMPO_RESPUESTA_USUARIO).isNullOrBlank()
                    if (!respondida) return@forEach
                    val (resp, ok) = conteos[subtema] ?: (0 to 0)
                    val esCorrecta = doc.getBoolean(CAMPO_ES_CORRECTA) ?: false
                    conteos[subtema] = (resp + 1) to (ok + if (esCorrecta) 1 else 0)
                }
            } catch (e: Exception) {
                // Una sesión que no se pueda leer no debe romper la analítica.
            }
        }

        simulacros.forEach {
            agregar(COLECCION_SIMULATIONS, it.id, COLECCION_SIM_QUESTIONS)
        }
        practicas.forEach {
            agregar(COLECCION_PRACTICE_SESSIONS, it.id, COLECCION_PRACTICE_QUESTIONS)
        }

        return conteos.map { (subtema, par) ->
            SubtemaEstadistica(subtema = subtema, respondidas = par.first, correctas = par.second)
        }
    }

    companion object {
        const val CAMPO_UID = "uid"
        const val CAMPO_FECHA = "fechaCreacion"
        const val CAMPO_SCORE = "score"
        const val CAMPO_TOTAL = "total"
        const val CAMPO_TIEMPO_MS = "tiempoUsedMs"
        const val CAMPO_RESPONDIDAS = "respondidas"
        const val CAMPO_OMITIDAS = "omitidas"
        const val CAMPO_AREA = "area"
        const val CAMPO_SUBTEMA = "subtema"
        const val CAMPO_RESPUESTA_USUARIO = "respuestaUsuario"
        const val CAMPO_ES_CORRECTA = "esCorrecta"

        /** Cuántas sesiones recientes alimentan la analítica (costo de lecturas). */
        const val LIMITE_SIMULACROS = 5
        const val LIMITE_PRACTICAS = 10
    }
}
