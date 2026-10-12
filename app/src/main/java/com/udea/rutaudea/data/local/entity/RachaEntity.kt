package com.udea.rutaudea.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Racha de actividad cacheada localmente (offline-first): una fila por
 * usuario. La clave primaria es el `uid`, de modo que distintas cuentas
 * del mismo dispositivo no comparten racha.
 *
 * El documento en Firestore (`rachas/{uid}`) usa el mismo id, por lo que
 * el upsert de sincronización es idempotente.
 */
@Entity(tableName = "rachas_cache")
data class RachaEntity(
    @PrimaryKey @ColumnInfo(name = "uid") val uid: String,
    @ColumnInfo(name = "actual") val actual: Int,
    @ColumnInfo(name = "mejor") val mejor: Int,
    @ColumnInfo(name = "ultimo_dia") val ultimoDia: Long?,
    @ColumnInfo(name = "ultima_actividad_ms") val ultimaActividadMs: Long,
    /** `false` = pendiente de subir a Firestore. */
    @ColumnInfo(name = "synced") val synced: Boolean = false
)
