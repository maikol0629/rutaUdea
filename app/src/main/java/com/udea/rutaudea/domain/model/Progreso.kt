package com.udea.rutaudea.domain.model

/**
 * Modelos del módulo de Progreso (plan, sección 12: analítica y
 * recomendaciones). Reflejan los resúmenes persistidos en Firestore
 * (`simulations` y `practiceSessions`).
 */

/** Resumen de un simulacro completado (colección `simulations`). */
data class SimulacroResumen(
    val id: String,
    val fechaMs: Long,
    val score: Int,
    val total: Int,
    val tiempoUsedMs: Long,
    val respondidas: Int,
    val omitidas: Int
) {
    val porcentaje: Int
        get() = if (total > 0) score * 100 / total else 0
}

/** Resumen de una sesión de práctica completada (colección `practiceSessions`). */
data class PracticaResumen(
    val id: String,
    val fechaMs: Long,
    val area: String,
    val subtema: String?,
    val score: Int,
    val total: Int,
    val tiempoUsedMs: Long
) {
    val porcentaje: Int
        get() = if (total > 0) score * 100 / total else 0

    /** Etiqueta legible de los filtros usados. */
    val filtroLabel: String
        get() = buildString {
            append(if (area == "razonamiento_logico") "RL" else "CL")
            if (!subtema.isNullOrBlank()) append(" · $subtema")
        }
}

/**
 * Acierto acumulado por subtema (agregado de `simulationQuestions` y
 * `practiceQuestions` de las sesiones recientes).
 */
data class SubtemaEstadistica(
    val subtema: String,
    val respondidas: Int,
    val correctas: Int
) {
    val porcentaje: Int
        get() = if (respondidas > 0) correctas * 100 / respondidas else 0

    companion object {
        /**
         * Evidencia mínima para considerar el subtema en recomendaciones
         * (plan §12: no recomendar un tema como «débil» por una sola
         * pregunta).
         */
        const val EVIDENCIA_MINIMA = 3
    }
}

/** Tipo de actividad del historial. */
enum class TipoActividad { SIMULACRO, PRACTICA }

/** Entrada unificada del historial (simulacros y prácticas). */
data class ItemHistorial(
    val tipo: TipoActividad,
    val id: String,
    val fechaMs: Long,
    val titulo: String,
    val score: Int,
    val total: Int,
    val porcentaje: Int,
    val tiempoUsedMs: Long
)
