package com.udea.rutaudea.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Resumen local de una sesión de práctica completada
 * (cache offline de `practiceSessions`).
 *
 * El `id` es el mismo que el documento de Firestore (generado en el
 * cliente) para que el upsert de sincronización sea idempotente.
 */
@Entity(
    tableName = "practice_sessions_cache",
    indices = [
        Index(value = ["uid", "fecha_ms"]),
        Index(value = ["synced"])
    ]
)
data class PracticeSummaryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "uid") val uid: String,
    @ColumnInfo(name = "fecha_ms") val fechaMs: Long,
    @ColumnInfo(name = "area") val area: String,
    @ColumnInfo(name = "subtema") val subtema: String?,
    @ColumnInfo(name = "score") val score: Int,
    @ColumnInfo(name = "total") val total: Int,
    @ColumnInfo(name = "tiempo_used_ms") val tiempoUsedMs: Long,
    @ColumnInfo(name = "respondidas") val respondidas: Int,
    @ColumnInfo(name = "omitidas") val omitidas: Int,
    /** `false` = pendiente de subir a Firestore. */
    @ColumnInfo(name = "synced") val synced: Boolean = false
)
