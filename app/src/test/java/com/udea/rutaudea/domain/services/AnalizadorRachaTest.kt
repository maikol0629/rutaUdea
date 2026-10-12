package com.udea.rutaudea.domain.services

import com.udea.rutaudea.domain.model.Racha
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests de las reglas de la racha de actividad: conteo de días
 * consecutivos, idempotencia por día, reinicio por huecos, vigencia para
 * mostrar y fusión local↔nube.
 */
class AnalizadorRachaTest {

    private val dia = 20_000L
    private val ahora = 1_000_000L

    @Test
    fun `la primera actividad inicia la racha en uno`() {
        val resultado = AnalizadorRacha.aplicarActividad(Racha(), dia, ahora)

        assertEquals(1, resultado.actual)
        assertEquals(1, resultado.mejor)
        assertEquals(dia, resultado.ultimoDia)
        assertEquals(ahora, resultado.ultimaActividadMs)
    }

    @Test
    fun `una actividad el dia siguiente suma a la racha`() {
        val previa = Racha(actual = 3, mejor = 5, ultimoDia = dia - 1, ultimaActividadMs = ahora - 1)

        val resultado = AnalizadorRacha.aplicarActividad(previa, dia, ahora)

        assertEquals(4, resultado.actual)
        assertEquals(5, resultado.mejor)
        assertEquals(dia, resultado.ultimoDia)
    }

    @Test
    fun `varias actividades el mismo dia cuentan una sola vez`() {
        val previa = Racha(actual = 2, mejor = 2, ultimoDia = dia, ultimaActividadMs = ahora - 500)

        val resultado = AnalizadorRacha.aplicarActividad(previa, dia, ahora)

        assertEquals(2, resultado.actual)
        assertEquals(2, resultado.mejor)
        // La marca de tiempo avanza, pero el conteo no.
        assertEquals(ahora, resultado.ultimaActividadMs)
    }

    @Test
    fun `un hueco de mas de un dia reinicia la racha en uno`() {
        val previa = Racha(actual = 7, mejor = 7, ultimoDia = dia - 3, ultimaActividadMs = ahora - 1)

        val resultado = AnalizadorRacha.aplicarActividad(previa, dia, ahora)

        assertEquals(1, resultado.actual)
        assertEquals(7, resultado.mejor) // el mejor histórico se conserva
    }

    @Test
    fun `el mejor historico nunca disminuye`() {
        val previa = Racha(actual = 1, mejor = 10, ultimoDia = dia - 1, ultimaActividadMs = ahora - 1)

        val resultado = AnalizadorRacha.aplicarActividad(previa, dia, ahora)

        assertEquals(2, resultado.actual)
        assertEquals(10, resultado.mejor)
    }

    @Test
    fun `una actividad con fecha anterior no retrocede la racha`() {
        val previa = Racha(actual = 4, mejor = 4, ultimoDia = dia, ultimaActividadMs = ahora)

        val resultado = AnalizadorRacha.aplicarActividad(previa, dia - 5, ahora + 1)

        assertEquals(previa, resultado)
    }

    @Test
    fun `la racha vigente es cero si pasaron dos o mas dias`() {
        val previa = Racha(actual = 5, mejor = 5, ultimoDia = dia - 2, ultimaActividadMs = ahora)

        val vigente = AnalizadorRacha.vigente(previa, dia)

        assertEquals(0, vigente.actual)
        assertEquals(5, vigente.mejor)
    }

    @Test
    fun `la racha sigue vigente si la ultima actividad fue ayer`() {
        val previa = Racha(actual = 5, mejor = 5, ultimoDia = dia - 1, ultimaActividadMs = ahora)

        val vigente = AnalizadorRacha.vigente(previa, dia)

        assertEquals(5, vigente.actual)
    }

    @Test
    fun `la racha vigente sin actividad es cero`() {
        val vigente = AnalizadorRacha.vigente(Racha(mejor = 3), dia)

        assertEquals(0, vigente.actual)
        assertEquals(3, vigente.mejor)
    }

    @Test
    fun `fusionar conserva el dia y el mejor mas recientes`() {
        val local = Racha(actual = 2, mejor = 2, ultimoDia = dia - 1, ultimaActividadMs = ahora - 10)
        val nube = Racha(actual = 4, mejor = 6, ultimoDia = dia, ultimaActividadMs = ahora)

        val fusion = AnalizadorRacha.fusionar(local, nube)

        assertEquals(4, fusion.actual)
        assertEquals(6, fusion.mejor)
        assertEquals(dia, fusion.ultimoDia)
        assertEquals(ahora, fusion.ultimaActividadMs)
    }

    @Test
    fun `fusionar elige la racha del dia mas reciente aunque sea la local`() {
        val local = Racha(actual = 9, mejor = 9, ultimoDia = dia, ultimaActividadMs = ahora)
        val nube = Racha(actual = 1, mejor = 3, ultimoDia = dia - 4, ultimaActividadMs = ahora - 100)

        val fusion = AnalizadorRacha.fusionar(local, nube)

        assertEquals(9, fusion.actual)
        assertEquals(9, fusion.mejor)
        assertEquals(dia, fusion.ultimoDia)
    }

    @Test
    fun `fusionar con la misma fecha toma el mayor actual y mejor`() {
        val local = Racha(actual = 3, mejor = 3, ultimoDia = dia, ultimaActividadMs = ahora)
        val nube = Racha(actual = 5, mejor = 8, ultimoDia = dia, ultimaActividadMs = ahora - 1)

        val fusion = AnalizadorRacha.fusionar(local, nube)

        assertEquals(5, fusion.actual)
        assertEquals(8, fusion.mejor)
    }

    @Test
    fun `fusionar con una racha vacia devuelve la otra`() {
        val nube = Racha(actual = 2, mejor = 3, ultimoDia = dia, ultimaActividadMs = ahora)

        assertEquals(nube, AnalizadorRacha.fusionar(Racha(), nube))
        assertEquals(nube, AnalizadorRacha.fusionar(nube, Racha()))
    }
}
