package com.udea.rutaudea.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad Room que representa una pregunta del banco.
 *
 * Se almacena en la base local (SQLite) para permitir consultas
 * rápidas, filtradas y offline. Los campos reflejan la estructura
 * del JSONL original (udea_dataset/dataset/udea_questions_final.jsonl).
 */
@Entity(
    tableName = "questions",
    indices = [
        Index(value = ["area"]),
        Index(value = ["subtema"]),
        Index(value = ["dificultad"]),
        Index(value = ["componente"])
    ]
)
data class QuestionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "area") val area: String,
    @ColumnInfo(name = "componente") val componente: String? = null,
    @ColumnInfo(name = "subtema") val subtema: String,
    @ColumnInfo(name = "competencia") val competencia: String? = null,
    @ColumnInfo(name = "tipo_texto") val tipoTexto: String? = null,
    @ColumnInfo(name = "dificultad") val dificultad: String,
    @ColumnInfo(name = "texto_base") val textoBase: String? = null,
    @ColumnInfo(name = "contexto") val contexto: String? = null,
    @ColumnInfo(name = "contexto_id") val contextoId: String? = null,
    @ColumnInfo(name = "pregunta") val pregunta: String,
    @ColumnInfo(name = "opcion_a") val opcionA: String,
    @ColumnInfo(name = "opcion_b") val opcionB: String,
    @ColumnInfo(name = "opcion_c") val opcionC: String,
    @ColumnInfo(name = "opcion_d") val opcionD: String,
    @ColumnInfo(name = "respuesta_correcta") val respuestaCorrecta: String,
    @ColumnInfo(name = "explicacion") val explicacion: String? = null,
    @ColumnInfo(name = "fuente") val fuente: String? = null,
    @ColumnInfo(name = "tipo_fuente") val tipoFuente: String? = null,
    @ColumnInfo(name = "es_original") val esOriginal: Boolean = false,
    @ColumnInfo(name = "verificada") val verificada: Boolean = false,
    @ColumnInfo(name = "estado") val estado: String = "aprobado"
)