package com.udea.rutaudea.data.mapper

import com.udea.rutaudea.data.local.entity.RachaEntity
import com.udea.rutaudea.domain.model.Racha

/**
 * Conversiones entre la cache local de la racha (Room) y el modelo de
 * dominio [Racha].
 */
object RachaMapper {

    /** Entidad → dominio. `null` se interpreta como «sin actividad». */
    fun toDomain(entity: RachaEntity?): Racha = entity?.let {
        Racha(
            actual = it.actual,
            mejor = it.mejor,
            ultimoDia = it.ultimoDia,
            ultimaActividadMs = it.ultimaActividadMs
        )
    } ?: Racha()

    /** Dominio → entidad, asociada al [uid] y con su estado de sincronización. */
    fun toEntity(uid: String, racha: Racha, synced: Boolean): RachaEntity = RachaEntity(
        uid = uid,
        actual = racha.actual,
        mejor = racha.mejor,
        ultimoDia = racha.ultimoDia,
        ultimaActividadMs = racha.ultimaActividadMs,
        synced = synced
    )
}
