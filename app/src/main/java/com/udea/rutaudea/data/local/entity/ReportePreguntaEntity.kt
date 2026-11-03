package com.udea.rutaudea.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Reporte de una pregunta cacheado localmente (offline-first).
 *
 * El `id` es el mismo que usa el documento de Firestore
 * (`reportesPreguntas/{id}`), por lo que reintentar la subida es
 * idempotente. `synced = false` indica que aún no llegó a la nube.
 */
@Entity(
    tableName = "reportes_preguntas_cache",
    indices = [
        Index(value = ["uid", "synced"]),
        Index(value = ["question_id"]),
        Index(value = ["fecha_ms"])
    ]
)
data class ReportePreguntaEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "uid") val uid: String,
    @ColumnInfo(name = "question_id") val questionId: String,
    @ColumnInfo(name = "motivo") val motivo: String,
    @ColumnInfo(name = "comentario") val comentario: String,
    @ColumnInfo(name = "modo") val modo: String,
    @ColumnInfo(name = "sesion_id") val sesionId: String,
    @ColumnInfo(name = "area") val area: String,
    @ColumnInfo(name = "subtema") val subtema: String,
    @ColumnInfo(name = "banco_version") val bancoVersion: String?,
    @ColumnInfo(name = "respuesta_usuario") val respuestaUsuario: String?,
    @ColumnInfo(name = "respuesta_correcta") val respuestaCorrecta: String?,
    @ColumnInfo(name = "fecha_ms") val fechaMs: Long,
    @ColumnInfo(name = "app_version_code") val appVersionCode: Int,
    @ColumnInfo(name = "estado") val estado: String,
    /** `false` = pendiente de subir a Firestore. */
    @ColumnInfo(name = "synced") val synced: Boolean = false
)
