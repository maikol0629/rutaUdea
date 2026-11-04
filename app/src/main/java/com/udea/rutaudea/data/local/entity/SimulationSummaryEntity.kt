package com.udea.rutaudea.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Resumen local de un simulacro completado (cache offline de `simulations`).
 *
 * El `id` es el mismo que el identificador del documento en Firestore
 * (generado en el cliente), de modo que la sincronización es idempotente:
 * se puede hacer upsert tanto en local como en la nube.
 */
@Entity(
    tableName = "simulations_cache",
    indices = [
        Index(value = ["uid", "fecha_ms"]),
        Index(value = ["synced"])
    ]
)
data class SimulationSummaryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "uid") val uid: String,
    @ColumnInfo(name = "fecha_ms") val fechaMs: Long,
    @ColumnInfo(name = "score") val score: Int,
    @ColumnInfo(name = "total") val total: Int,
    @ColumnInfo(name = "tiempo_used_ms") val tiempoUsedMs: Long,
    @ColumnInfo(name = "respondidas") val respondidas: Int,
    @ColumnInfo(name = "omitidas") val omitidas: Int,
    /** `false` = pendiente de subir a Firestore. */
    @ColumnInfo(name = "synced") val synced: Boolean = false
)
