package com.udea.rutaudea.domain.services

import java.time.LocalDate
import java.time.ZoneId

/**
 * Fuente de tiempo inyectable.
 *
 * Permite probar de forma determinista la lógica de la racha, sin
 * depender del reloj real del dispositivo ni de la fecha de ejecución
 * de los tests.
 */
interface Reloj {

    /** Día calendario actual como día epoch (días desde 1970-01-01). */
    fun diaActual(): Long

    /** Instante actual en milisegundos. */
    fun ahoraMs(): Long

    companion object {
        /** Reloj real del dispositivo, en la zona horaria local. */
        val sistema: Reloj = object : Reloj {
            override fun diaActual(): Long = LocalDate.now(ZoneId.systemDefault()).toEpochDay()

            override fun ahoraMs(): Long = System.currentTimeMillis()
        }
    }
}
