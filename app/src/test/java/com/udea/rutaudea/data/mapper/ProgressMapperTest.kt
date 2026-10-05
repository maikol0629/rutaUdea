package com.udea.rutaudea.data.mapper

import com.udea.rutaudea.data.local.entity.PracticeSummaryEntity
import com.udea.rutaudea.data.local.entity.SimulationSummaryEntity
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests del mapeo entidad Room → dominio del módulo de Progreso.
 */
class ProgressMapperTest {

    @Test
    fun `mapea resumen de simulacro conservando score y porcentaje`() {
        val entity = SimulationSummaryEntity(
            id = "sim-1",
            uid = "u1",
            fechaMs = 1_000L,
            score = 60,
            total = 80,
            tiempoUsedMs = 5_000L,
            respondidas = 78,
            omitidas = 2,
            synced = false
        )

        val dominio = ProgressMapper.simulacroToDomain(entity)

        assertEquals("sim-1", dominio.id)
        assertEquals(1_000L, dominio.fechaMs)
        assertEquals(60, dominio.score)
        assertEquals(80, dominio.total)
        assertEquals(78, dominio.respondidas)
        assertEquals(2, dominio.omitidas)
        assertEquals(75, dominio.porcentaje)
    }

    @Test
    fun `mapea resumen de practica con filtro legible`() {
        val entity = PracticeSummaryEntity(
            id = "prac-1",
            uid = "u1",
            fechaMs = 2_000L,
            area = "razonamiento_logico",
            subtema = "RL07 Sucesiones",
            score = 8,
            total = 10,
            tiempoUsedMs = 3_000L,
            respondidas = 10,
            omitidas = 0,
            synced = true
        )

        val dominio = ProgressMapper.practicaToDomain(entity)

        assertEquals("prac-1", dominio.id)
        assertEquals("RL07 Sucesiones", dominio.subtema)
        assertEquals(80, dominio.porcentaje)
        assertEquals("RL · RL07 Sucesiones", dominio.filtroLabel)
    }

    @Test
    fun `total cero no divide por cero`() {
        val entity = SimulationSummaryEntity(
            id = "sim-0", uid = "u1", fechaMs = 0L, score = 0, total = 0,
            tiempoUsedMs = 0L, respondidas = 0, omitidas = 0
        )
        assertEquals(0, ProgressMapper.simulacroToDomain(entity).porcentaje)
    }
}
