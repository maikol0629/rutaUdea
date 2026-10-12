package com.udea.rutaudea.domain.services

import com.udea.rutaudea.domain.model.Racha

/**
 * Reglas puras de la racha de actividad del usuario, sin dependencias de
 * Android, Room ni Firestore (por lo tanto, fáciles de probar).
 *
 * Una racha cuenta **días calendario consecutivos** con al menos una
 * actividad completada (un simulacro o una práctica de cualquier tamaño).
 */
object AnalizadorRacha {

    /**
     * Días de tolerancia para considerar viva una racha: si la última
     * actividad fue hoy o ayer, el usuario todavía puede continuarla.
     */
    private const val DIAS_TOLERANCIA = 1L

    /**
     * Aplica una actividad realizada el día [dia]:
     *
     * - Primera actividad → racha 1.
     * - Mismo día ya registrado → sin cambios (idempotente: varias
     *   actividades el mismo día cuentan una sola vez).
     * - Día inmediatamente siguiente → racha + 1.
     * - Hueco de más de un día → la racha se reinicia en 1.
     * - Día anterior al último registrado → se ignora (no retrocede).
     *
     * El [Racha.mejor] histórico nunca disminuye.
     */
    fun aplicarActividad(racha: Racha, dia: Long, ahoraMs: Long): Racha {
        val ultimo = racha.ultimoDia
        if (ultimo != null && dia < ultimo) return racha

        val nuevaActual = when {
            ultimo == null -> 1
            dia == ultimo -> racha.actual.coerceAtLeast(1)
            dia == ultimo + DIAS_TOLERANCIA -> racha.actual + 1
            else -> 1
        }
        return racha.copy(
            actual = nuevaActual,
            mejor = maxOf(racha.mejor, nuevaActual),
            ultimoDia = dia,
            ultimaActividadMs = maxOf(racha.ultimaActividadMs, ahoraMs)
        )
    }

    /**
     * Racha «vigente» para mostrar: si la última actividad fue hoy o ayer
     * la racha sigue viva; si pasaron dos o más días, se considera rota
     * (se muestra 0, aunque el mejor histórico se conserva).
     */
    fun vigente(racha: Racha, dia: Long): Racha {
        val ultimo = racha.ultimoDia ?: return racha.copy(actual = 0)
        return if (dia - ultimo <= DIAS_TOLERANCIA) racha else racha.copy(actual = 0)
    }

    /**
     * Combina dos rachas del mismo usuario (por ejemplo, la local y la de
     * la nube) quedándose con la información más favorable: el mejor
     * histórico nunca se pierde y prevalece la racha asociada al día de
     * actividad más reciente. Si ambas comparten día, gana la mayor.
     */
    fun fusionar(local: Racha, nube: Racha): Racha {
        val actual = when {
            local.ultimoDia == null -> nube.actual
            nube.ultimoDia == null -> local.actual
            local.ultimoDia > nube.ultimoDia -> local.actual
            local.ultimoDia < nube.ultimoDia -> nube.actual
            else -> maxOf(local.actual, nube.actual)
        }
        return Racha(
            actual = actual,
            mejor = maxOf(local.mejor, nube.mejor, actual),
            ultimoDia = mayorDia(local.ultimoDia, nube.ultimoDia),
            ultimaActividadMs = maxOf(local.ultimaActividadMs, nube.ultimaActividadMs)
        )
    }

    private fun mayorDia(a: Long?, b: Long?): Long? = when {
        a == null -> b
        b == null -> a
        else -> maxOf(a, b)
    }
}
