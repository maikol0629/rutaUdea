package com.udea.rutaudea.domain.services

import com.udea.rutaudea.domain.model.PracticaResumen
import com.udea.rutaudea.domain.model.SimulacroResumen
import com.udea.rutaudea.domain.model.SubtemaEstadistica
import com.udea.rutaudea.domain.model.TipoActividad
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests de las reglas de analítica del módulo de Progreso
 * (plan, sección 12): evidencia mínima, orden de fortalezas/debilidades,
 * evolución, promedios e historial unificado.
 */
class AnalizadorProgresoTest {

    private fun simulacro(
        id: String,
        fecha: Long,
        score: Int,
        total: Int = 80
    ) = SimulacroResumen(
        id = id, fechaMs = fecha, score = score, total = total,
        tiempoUsedMs = 3600_000L, respondidas = total, omitidas = 0
    )

    private fun practica(
        id: String,
        fecha: Long,
        score: Int,
        total: Int = 10,
        subtema: String = "RL02 Porcentajes",
        area: String = "razonamiento_logico"
    ) = PracticaResumen(
        id = id, fechaMs = fecha, area = area, subtema = subtema,
        score = score, total = total, tiempoUsedMs = 300_000L
    )

    @Test
    fun `areas de mejora ignoran subtemas sin evidencia suficiente`() {
        // «Una pregunta no hace un tema débil» (plan §12).
        val stats = listOf(
            SubtemaEstadistica("RL13 Geometría", respondidas = 1, correctas = 0), // 0% pero sin evidencia
            SubtemaEstadistica("RL02 Porcentajes", respondidas = 5, correctas = 1), // 20% con evidencia
            SubtemaEstadistica("RL07 Sucesiones", respondidas = 4, correctas = 3)  // 75% con evidencia
        )

        val areas = AnalizadorProgreso.areasDeMejora(stats)

        assertEquals(listOf("RL02 Porcentajes", "RL07 Sucesiones"), areas.map { it.subtema })
    }

    @Test
    fun `areas de mejora ordenan por peor acierto primero`() {
        val stats = listOf(
            SubtemaEstadistica("A", respondidas = 10, correctas = 6),  // 60%
            SubtemaEstadistica("B", respondidas = 10, correctas = 2),  // 20%
            SubtemaEstadistica("C", respondidas = 10, correctas = 8)   // 80%
        )

        val areas = AnalizadorProgreso.areasDeMejora(stats)

        assertEquals(listOf("B", "A", "C"), areas.map { it.subtema })
    }

    @Test
    fun `fortalezas ordenan por mejor acierto primero`() {
        val stats = listOf(
            SubtemaEstadistica("A", respondidas = 10, correctas = 6),
            SubtemaEstadistica("B", respondidas = 10, correctas = 9),
            SubtemaEstadistica("C", respondidas = 10, correctas = 8)
        )

        val fortalezas = AnalizadorProgreso.fortalezas(stats)

        assertEquals(listOf("B", "C", "A"), fortalezas.map { it.subtema })
    }

    @Test
    fun `las listas de subtemas se limitan al top`() {
        val stats = (1..10).map {
            SubtemaEstadistica("s$it", respondidas = 10, correctas = it)
        }

        assertEquals(AnalizadorProgreso.TOP_SUBTEMAS, AnalizadorProgreso.areasDeMejora(stats).size)
        assertEquals(AnalizadorProgreso.TOP_SUBTEMAS, AnalizadorProgreso.fortalezas(stats).size)
    }

    @Test
    fun `evolucion ordena cronologicamente de antiguo a reciente`() {
        val simulacros = listOf(
            simulacro("s3", fecha = 300, score = 56),
            simulacro("s1", fecha = 100, score = 32),
            simulacro("s2", fecha = 200, score = 48)
        )

        assertEquals(listOf(40, 60, 70), AnalizadorProgreso.evolucion(simulacros))
    }

    @Test
    fun `promedio y mejor puntaje de simulacros`() {
        val simulacros = listOf(
            simulacro("s1", fecha = 100, score = 40),  // 50%
            simulacro("s2", fecha = 200, score = 64)   // 80%
        )

        assertEquals(65, AnalizadorProgreso.promedio(simulacros))
        assertEquals(80, AnalizadorProgreso.mejorPuntaje(simulacros))
    }

    @Test
    fun `promedio sin simulacros es cero`() {
        assertEquals(0, AnalizadorProgreso.promedio(emptyList()))
        assertEquals(0, AnalizadorProgreso.mejorPuntaje(emptyList()))
        assertTrue(AnalizadorProgreso.evolucion(emptyList()).isEmpty())
    }

    @Test
    fun `historial unifica simulacros y practicas por fecha descendente`() {
        val items = AnalizadorProgreso.historial(
            simulacros = listOf(simulacro("s1", fecha = 100, score = 40)),
            practicas = listOf(practica("p1", fecha = 200, score = 7))
        )

        assertEquals(2, items.size)
        assertEquals(TipoActividad.PRACTICA, items[0].tipo)   // más reciente
        assertEquals(TipoActividad.SIMULACRO, items[1].tipo)
        assertEquals(70, items[0].porcentaje)
        assertEquals(50, items[1].porcentaje)
    }

    @Test
    fun `el titulo de practica incluye el filtro legible`() {
        val items = AnalizadorProgreso.historial(
            simulacros = emptyList(),
            practicas = listOf(
                practica("p1", fecha = 1, score = 7, subtema = "CL02 Inferencia", area = "competencia_lectora")
            )
        )

        assertTrue(items.single().titulo.contains("CL · CL02 Inferencia"))
    }
}
