package com.udea.rutaudea.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class DatosResumenSesionTest {

    @Test
    fun `reutiliza el id de sesion y calcula las omitidas`() {
        val datos = DatosResumenSesion.crear(
            total = 10,
            respondidas = 7,
            sessionId = "sesion-1",
            ahoraMs = 1234L
        )

        assertEquals("sesion-1", datos.id)
        assertEquals(1234L, datos.fechaMs)
        assertEquals(7, datos.respondidas)
        assertEquals(3, datos.omitidas)
    }

    @Test
    fun `genera un id cuando no hay sesion previa`() {
        val datos = DatosResumenSesion.crear(
            total = 5,
            respondidas = 0,
            sessionId = null,
            nuevoId = { "generado" }
        )

        assertEquals("generado", datos.id)
        assertEquals(0, datos.respondidas)
        assertEquals(5, datos.omitidas)
    }
}
