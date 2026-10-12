package com.udea.rutaudea.domain.model

/**
 * Racha de días consecutivos con actividad del usuario.
 *
 * El «día» se representa como día epoch (días transcurridos desde
 * 1970-01-01) para evitar ambigüedades de zona horaria y cambios de
 * horario: dos actividades realizadas el mismo día calendario cuentan
 * una sola vez.
 *
 * @property actual días consecutivos con actividad (la racha vigente la
 *   calcula [com.udea.rutaudea.domain.services.AnalizadorRacha]).
 * @property mejor mejor racha histórica alcanzada (nunca disminuye).
 * @property ultimoDia día epoch de la última actividad (`null` = sin actividad).
 * @property ultimaActividadMs marca de tiempo (ms) de la última actividad,
 *   usada para sincronización y orden entre dispositivos.
 */
data class Racha(
    val actual: Int = 0,
    val mejor: Int = 0,
    val ultimoDia: Long? = null,
    val ultimaActividadMs: Long = 0L
) {
    /** `true` si el usuario ha registrado alguna actividad alguna vez. */
    val tieneActividad: Boolean
        get() = ultimoDia != null
}
