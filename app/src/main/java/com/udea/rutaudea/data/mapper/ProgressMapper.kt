package com.udea.rutaudea.data.mapper

import com.udea.rutaudea.data.local.entity.PracticeSummaryEntity
import com.udea.rutaudea.data.local.entity.SimulationSummaryEntity
import com.udea.rutaudea.domain.model.PracticaResumen
import com.udea.rutaudea.domain.model.SimulacroResumen

/**
 * Conversiones entre la cache local (Room) y los modelos de dominio
 * del módulo de Progreso.
 */
object ProgressMapper {

    fun simulacroToDomain(entity: SimulationSummaryEntity): SimulacroResumen = SimulacroResumen(
        id = entity.id,
        fechaMs = entity.fechaMs,
        score = entity.score,
        total = entity.total,
        tiempoUsedMs = entity.tiempoUsedMs,
        respondidas = entity.respondidas,
        omitidas = entity.omitidas
    )

    fun practicaToDomain(entity: PracticeSummaryEntity): PracticaResumen = PracticaResumen(
        id = entity.id,
        fechaMs = entity.fechaMs,
        area = entity.area,
        subtema = entity.subtema,
        score = entity.score,
        total = entity.total,
        tiempoUsedMs = entity.tiempoUsedMs
    )

    fun listSimulacrosToDomain(entities: List<SimulationSummaryEntity>): List<SimulacroResumen> =
        entities.map { simulacroToDomain(it) }

    fun listPracticasToDomain(entities: List<PracticeSummaryEntity>): List<PracticaResumen> =
        entities.map { practicaToDomain(it) }
}
