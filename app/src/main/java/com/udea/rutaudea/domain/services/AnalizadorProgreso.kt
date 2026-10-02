package com.udea.rutaudea.domain.services

import com.udea.rutaudea.domain.model.ItemHistorial
import com.udea.rutaudea.domain.model.PracticaResumen
import com.udea.rutaudea.domain.model.SimulacroResumen
import com.udea.rutaudea.domain.model.SubtemaEstadistica
import com.udea.rutaudea.domain.model.TipoActividad
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Reglas de analítica del módulo de Progreso (plan, sección 12).
 * Clase pura de dominio, sin dependencias de Android/Firestore.
 *
 * - Fortalezas/áreas de mejora: solo subtemas con evidencia suficiente
 *   (≥ [SubtemaEstadistica.EVIDENCIA_MINIMA] respuestas), evitando marcar
 *   un tema como «débil» por una sola pregunta.
 * - Recomendaciones: los subtemas de más bajo desempeño con evidencia.
 * - Historial: simulacros y prácticas unificados, más reciente primero.
 */
object AnalizadorProgreso {

    /** Cuántos ítems mostrar en cada lista de subtemas. */
    const val TOP_SUBTEMAS = 5
    const val RECOMENDACIONES = 3

    /** Historial unificado, ordenado por fecha descendente. */
    fun historial(
        simulacros: List<SimulacroResumen>,
        practicas: List<PracticaResumen>
    ): List<ItemHistorial> {
        val items = simulacros.map {
            ItemHistorial(
                tipo = TipoActividad.SIMULACRO,
                id = it.id,
                fechaMs = it.fechaMs,
                titulo = "Simulacro ${formatFecha(it.fechaMs)}",
                score = it.score,
                total = it.total,
                porcentaje = it.porcentaje,
                tiempoUsedMs = it.tiempoUsedMs
            )
        } + practicas.map {
            ItemHistorial(
                tipo = TipoActividad.PRACTICA,
                id = it.id,
                fechaMs = it.fechaMs,
                titulo = "Práctica · ${it.filtroLabel}",
                score = it.score,
                total = it.total,
                porcentaje = it.porcentaje,
                tiempoUsedMs = it.tiempoUsedMs
            )
        }
        return items.sortedByDescending { it.fechaMs }
    }

    /** Áreas de mejora: peor acierto primero, solo con evidencia suficiente. */
    fun areasDeMejora(stats: List<SubtemaEstadistica>): List<SubtemaEstadistica> =
        stats
            .filter { it.respondidas >= SubtemaEstadistica.EVIDENCIA_MINIMA }
            .sortedWith(
                compareBy<SubtemaEstadistica> { it.porcentaje }.thenByDescending { it.respondidas }
            )
            .take(TOP_SUBTEMAS)

    /** Fortalezas: mejor acierto primero, solo con evidencia suficiente. */
    fun fortalezas(stats: List<SubtemaEstadistica>): List<SubtemaEstadistica> =
        stats
            .filter { it.respondidas >= SubtemaEstadistica.EVIDENCIA_MINIMA }
            .sortedWith(
                compareByDescending<SubtemaEstadistica> { it.porcentaje }.thenByDescending { it.respondidas }
            )
            .take(TOP_SUBTEMAS)

    /** Promedio de acierto global de los simulacros (porcentaje). */
    fun promedio(simulacros: List<SimulacroResumen>): Int =
        if (simulacros.isEmpty()) 0
        else simulacros.map { it.porcentaje }.average().toInt()

    /** Mejor puntaje de simulacro (porcentaje). */
    fun mejorPuntaje(simulacros: List<SimulacroResumen>): Int =
        simulacros.maxOfOrNull { it.porcentaje } ?: 0

    /**
     * Evolución del desempeño en simulacros (porcentajes en orden
     * cronológico, del más antiguo al más reciente).
     */
    fun evolucion(simulacros: List<SimulacroResumen>): List<Int> =
        simulacros
            .sortedBy { it.fechaMs }
            .map { it.porcentaje }

    private fun formatFecha(fechaMs: Long): String =
        SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(Date(fechaMs))
}
