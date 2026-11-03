package com.udea.rutaudea.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.udea.rutaudea.data.local.entity.ReportePreguntaEntity

/**
 * DAO de los reportes de preguntas.
 *
 * Room es la fuente primaria (funciona sin conexión); Firestore se usa
 * para que el equipo pueda revisar y priorizar las mejoras del banco.
 */
@Dao
interface ReportePreguntaDao {

    @Upsert
    suspend fun upsert(reporte: ReportePreguntaEntity)

    @Query(
        """
        SELECT * FROM reportes_preguntas_cache
        WHERE uid = :uid AND synced = 0
        ORDER BY fecha_ms ASC
        """
    )
    suspend fun getPendientes(uid: String): List<ReportePreguntaEntity>

    @Query("UPDATE reportes_preguntas_cache SET synced = 1 WHERE id = :id")
    suspend fun marcarSincronizado(id: String)

    /** Evita reportes duplicados de la misma pregunta en la misma sesión. */
    @Query(
        """
        SELECT COUNT(*) FROM reportes_preguntas_cache
        WHERE uid = :uid AND question_id = :questionId AND sesion_id = :sesionId
        """
    )
    suspend fun countReporte(uid: String, questionId: String, sesionId: String): Int

    /** Preguntas ya reportadas en una sesión (para restaurar el estado del icono). */
    @Query(
        """
        SELECT question_id FROM reportes_preguntas_cache
        WHERE uid = :uid AND sesion_id = :sesionId
        """
    )
    suspend fun getQuestionIdsReportadas(uid: String, sesionId: String): List<String>
}
