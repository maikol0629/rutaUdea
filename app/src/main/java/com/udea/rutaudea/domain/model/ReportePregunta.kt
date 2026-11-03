package com.udea.rutaudea.domain.model

/**
 * Motivos por los que un aspirante puede reportar una pregunta del banco.
 * La [clave] es el valor estable que viaja a Firestore; la [etiqueta] es
 * solo para la UI.
 */
enum class MotivoReporte(val clave: String, val etiqueta: String) {
    ENUNCIADO_CONFUSO("enunciado_confuso", "Enunciado confuso"),
    RESPUESTA_INCORRECTA("respuesta_incorrecta", "La respuesta correcta no coincide"),
    EXPLICACION_INCORRECTA("explicacion_incorrecta", "La explicación es incorrecta"),
    FALTAN_DATOS("faltan_datos", "Faltan datos o imagen"),
    ORTOGRAFIA("ortografia", "Ortografía o redacción"),
    OTRA("otra", "Otra");

    companion object {
        /** Convierte la clave de Firestore/Room al motivo; degrada a [OTRA]. */
        fun fromClave(clave: String?): MotivoReporte =
            entries.firstOrNull { it.clave == clave } ?: OTRA
    }
}

/** Módulo en el que se hizo el reporte. */
enum class ModoReporte(val clave: String) {
    SIMULACRO("simulacro"),
    PRACTICA("practica");

    companion object {
        /** Convierte la clave de Firestore/Room al modo; degrada a [PRACTICA]. */
        fun fromClave(clave: String?): ModoReporte =
            entries.firstOrNull { it.clave == clave } ?: PRACTICA
    }
}

/**
 * Reporte de una pregunta hecha por un aspirante durante un simulacro o
 * una práctica.
 *
 * Es un modelo puro (sin Android) para poder validarlo y testearlo. El
 * repositorio completa [id], [fechaMs] y [appVersionCode] al guardar.
 */
data class ReportePregunta(
    val id: String = "",
    val questionId: String,
    val motivo: MotivoReporte,
    val comentario: String = "",
    val modo: ModoReporte,
    val sesionId: String,
    val area: String = "",
    val subtema: String = "",
    val bancoVersion: String? = null,
    val respuestaUsuario: String? = null,
    val respuestaCorrecta: String? = null,
    val fechaMs: Long = 0L,
    val appVersionCode: Int = 0,
    val estado: String = ESTADO_PENDIENTE
) {
    /**
     * Reglas de validación:
     * - la pregunta debe tener id;
     * - el comentario no puede superar [MAX_COMENTARIO] caracteres;
     * - si el motivo es [MotivoReporte.OTRA], el comentario es obligatorio.
     */
    fun validar(): List<String> = buildList {
        if (questionId.isBlank()) {
            add("La pregunta no es válida")
        }
        if (comentario.length > MAX_COMENTARIO) {
            add("El comentario no puede superar $MAX_COMENTARIO caracteres")
        }
        if (motivo == MotivoReporte.OTRA && comentario.isBlank()) {
            add("Cuéntanos brevemente qué encontraste")
        }
    }

    companion object {
        const val MAX_COMENTARIO = 500

        const val ESTADO_PENDIENTE = "pendiente"
        const val ESTADO_REVISADO = "revisado"
        const val ESTADO_DESCARTADO = "descartado"
    }
}
