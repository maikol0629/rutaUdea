package com.udea.rutaudea.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Respuesta del cuestionario de satisfacción cacheada localmente
 * (offline-first): una fila por usuario. El documento en Firestore
 * (`encuestasSatisfaccion/{uid}`) usa el mismo id, por lo que el upsert de
 * sincronización es idempotente.
 */
@Entity(tableName = "encuestas_cache")
data class EncuestaEntity(
    @PrimaryKey @ColumnInfo(name = "uid") val uid: String,
    @ColumnInfo(name = "fecha_ms") val fechaMs: Long,
    @ColumnInfo(name = "tipo_institucion") val tipoInstitucion: String,
    @ColumnInfo(name = "nombre_institucion") val nombreInstitucion: String,
    @ColumnInfo(name = "grado") val grado: String,
    @ColumnInfo(name = "semestre") val semestre: String,
    @ColumnInfo(name = "presento_examen") val presentoExamen: Boolean?,
    @ColumnInfo(name = "likert_simulacro") val likertSimulacro: Int,
    @ColumnInfo(name = "likert_practica") val likertPractica: Int,
    @ColumnInfo(name = "likert_progreso") val likertProgreso: Int,
    @ColumnInfo(name = "likert_info") val likertInfo: Int,
    @ColumnInfo(name = "claridad_rl") val claridadRL: Int,
    @ColumnInfo(name = "claridad_cl") val claridadCL: Int,
    @ColumnInfo(name = "dificultad_adecuada") val dificultadAdecuada: Int,
    @ColumnInfo(name = "facilidad_uso") val facilidadUso: Int,
    @ColumnInfo(name = "nps") val nps: Int,
    @ColumnInfo(name = "gusto_mas") val gustoMas: String,
    @ColumnInfo(name = "mejorar") val mejorar: String,
    /** `false` = pendiente de subir a Firestore. */
    @ColumnInfo(name = "synced") val synced: Boolean = false
)
