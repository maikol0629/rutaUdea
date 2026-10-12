package com.udea.rutaudea.domain.services

/**
 * Saludo personalizado del Home según el momento del día y el nombre del
 * usuario. Lógica pura de dominio, sin dependencias de Android.
 */
object Saludo {

    /**
     * Construye el encabezado (por ejemplo, «Buenas tardes, Maikol»).
     * Si no hay nombre disponible, devuelve solo el momento del día.
     * Se usa el primer nombre para que el saludo suene natural.
     *
     * @param nombre nombre del usuario (puede ser nulo o vacío).
     * @param hora24 hora del día en formato 24h (0–23).
     */
    fun encabezado(nombre: String?, hora24: Int): String {
        val momento = when (hora24) {
            in HORA_MANANA -> "Buenos días"
            in HORA_TARDE -> "Buenas tardes"
            else -> "Buenas noches"
        }
        val primero = nombre?.trim()?.substringBefore(" ").orEmpty()
        return if (primero.isBlank()) momento else "$momento, $primero"
    }

    private val HORA_MANANA = 5..11
    private val HORA_TARDE = 12..18
}
