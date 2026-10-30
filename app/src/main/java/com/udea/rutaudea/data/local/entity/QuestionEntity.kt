package com.udea.rutaudea.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.udea.rutaudea.domain.model.Question

/**
 * Entidad Room que representa una pregunta del banco.
 *
 * Se almacena en la base local (SQLite) para permitir consultas
 * rápidas, filtradas y offline. Los campos reflejan el contrato v2
 * (`migracion/01_esquema/contrato_datos.md`).
 *
 * Las imágenes se guardan serializadas en [imagenesJson] para no exigir
 * conversores de Room ni tablas adicionales; son pocas por pregunta.
 */
@Entity(
    tableName = "questions",
    indices = [
        Index(value = ["area"]),
        Index(value = ["subtema"]),
        Index(value = ["dificultad"]),
        Index(value = ["componente"]),
        Index(value = ["estado"]),
        Index(value = ["banco_version"])
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
    /**
     * Estado del contrato v2. El valor por defecto es `pendiente` a
     * propósito (D-08): una fila sin estado explícito nunca debe
     * publicarse como aprobada.
     */
    @ColumnInfo(name = "estado") val estado: String = Question.ESTADO_PENDIENTE,
    /** URL pública de la figura principal (compatibilidad con el banco viejo). */
    @ColumnInfo(name = "imagen_url") val imagenUrl: String? = null,
    /** Lista JSON de imágenes del contrato v2: [{url, rol, orden, alt, path_origen}]. */
    @ColumnInfo(name = "imagenes") val imagenesJson: String = "[]",
    @ColumnInfo(name = "opciones_en_imagen") val opcionesEnImagen: Boolean = false,
    @ColumnInfo(name = "requiere_imagen") val requiereImagen: Boolean = false,
    /** Versión del banco que originó la fila (D-07). */
    @ColumnInfo(name = "banco_version") val bancoVersion: String = "",
    @ColumnInfo(name = "tarea") val tarea: String? = null,
    @ColumnInfo(name = "numero") val numero: Int? = null,
    @ColumnInfo(name = "confianza_modelo") val confianzaModelo: String? = null,
    @ColumnInfo(name = "revisado_por") val revisadoPor: String? = null,
    @ColumnInfo(name = "fecha_revision") val fechaRevision: String? = null,
    @ColumnInfo(name = "notas") val notas: String? = null
)
