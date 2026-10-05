package com.udea.rutaudea.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.udea.rutaudea.data.local.entity.PracticeQuestionEntity
import com.udea.rutaudea.data.local.entity.PracticeSummaryEntity
import com.udea.rutaudea.data.local.entity.SimulationQuestionEntity
import com.udea.rutaudea.data.local.entity.SimulationSummaryEntity

/**
 * DAO de la cache local del módulo de Progreso.
 *
 * Almacena el historial de simulacros y prácticas (resumen + detalle por
 * pregunta) para que el dashboard funcione offline y sin depender de
 * índices de Firestore. La nube se usa como respaldo/sincronización.
 */
@Dao
interface ProgressDao {

    // ----- resúmenes -----

    @Upsert
    suspend fun upsertSimulacros(items: List<SimulationSummaryEntity>)

    @Upsert
    suspend fun upsertPracticas(items: List<PracticeSummaryEntity>)

    @Query("SELECT * FROM simulations_cache WHERE uid = :uid ORDER BY fecha_ms DESC LIMIT :limit")
    suspend fun getSimulacros(uid: String, limit: Int): List<SimulationSummaryEntity>

    @Query("SELECT * FROM practice_sessions_cache WHERE uid = :uid ORDER BY fecha_ms DESC LIMIT :limit")
    suspend fun getPracticas(uid: String, limit: Int): List<PracticeSummaryEntity>

    @Query("SELECT * FROM simulations_cache WHERE uid = :uid AND synced = 0")
    suspend fun getSimulacrosPendientes(uid: String): List<SimulationSummaryEntity>

    @Query("SELECT * FROM practice_sessions_cache WHERE uid = :uid AND synced = 0")
    suspend fun getPracticasPendientes(uid: String): List<PracticeSummaryEntity>

    @Query("SELECT id FROM simulations_cache WHERE uid = :uid")
    suspend fun getSimulacroIds(uid: String): List<String>

    @Query("SELECT id FROM practice_sessions_cache WHERE uid = :uid")
    suspend fun getPracticaIds(uid: String): List<String>

    @Query("UPDATE simulations_cache SET synced = 1 WHERE id IN (:ids)")
    suspend fun marcarSimulacrosSincronizados(ids: List<String>)

    @Query("UPDATE practice_sessions_cache SET synced = 1 WHERE id IN (:ids)")
    suspend fun marcarPracticasSincronizadas(ids: List<String>)

    // ----- detalle por pregunta -----

    @Upsert
    suspend fun upsertSimulationQuestions(items: List<SimulationQuestionEntity>)

    @Upsert
    suspend fun upsertPracticeQuestions(items: List<PracticeQuestionEntity>)

    @Query("SELECT * FROM simulation_questions_cache WHERE session_id IN (:sessionIds)")
    suspend fun getSimulationQuestions(sessionIds: List<String>): List<SimulationQuestionEntity>

    @Query("SELECT * FROM practice_questions_cache WHERE session_id IN (:sessionIds)")
    suspend fun getPracticeQuestions(sessionIds: List<String>): List<PracticeQuestionEntity>

    @Query("SELECT COUNT(*) FROM simulation_questions_cache WHERE session_id = :sessionId")
    suspend fun countSimulationQuestions(sessionId: String): Int

    @Query("SELECT COUNT(*) FROM practice_questions_cache WHERE session_id = :sessionId")
    suspend fun countPracticeQuestions(sessionId: String): Int

    // ----- mantenimiento -----

    @Query("DELETE FROM simulations_cache WHERE uid = :uid")
    suspend fun limpiarSimulacros(uid: String)

    @Query("DELETE FROM practice_sessions_cache WHERE uid = :uid")
    suspend fun limpiarPracticas(uid: String)

    @Query("DELETE FROM simulation_questions_cache WHERE session_id IN (:sessionIds)")
    suspend fun limpiarSimulationQuestions(sessionIds: List<String>)

    @Query("DELETE FROM practice_questions_cache WHERE session_id IN (:sessionIds)")
    suspend fun limpiarPracticeQuestions(sessionIds: List<String>)
}
