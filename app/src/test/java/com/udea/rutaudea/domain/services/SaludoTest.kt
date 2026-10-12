package com.udea.rutaudea.domain.services

import org.junit.Assert.assertEquals
import org.junit.Test

/** Tests del saludo personalizado del Home. */
class SaludoTest {

    @Test
    fun `saluda por la manana con el primer nombre`() {
        assertEquals("Buenos días, Maikol", Saludo.encabezado("Maikol Henao", 9))
    }

    @Test
    fun `saluda por la tarde`() {
        assertEquals("Buenas tardes, Ana", Saludo.encabezado("Ana", 15))
    }

    @Test
    fun `saluda por la noche y de madrugada`() {
        assertEquals("Buenas noches, Ana", Saludo.encabezado("Ana", 21))
        assertEquals("Buenas noches, Ana", Saludo.encabezado("Ana", 3))
    }

    @Test
    fun `los limites de cada franja son inclusivos`() {
        assertEquals("Buenos días, Ana", Saludo.encabezado("Ana", 5))
        assertEquals("Buenos días, Ana", Saludo.encabezado("Ana", 11))
        assertEquals("Buenas tardes, Ana", Saludo.encabezado("Ana", 12))
        assertEquals("Buenas tardes, Ana", Saludo.encabezado("Ana", 18))
        assertEquals("Buenas noches, Ana", Saludo.encabezado("Ana", 19))
        assertEquals("Buenas noches, Ana", Saludo.encabezado("Ana", 4))
    }

    @Test
    fun `sin nombre devuelve solo el momento del dia`() {
        assertEquals("Buenos días", Saludo.encabezado(null, 9))
        assertEquals("Buenas tardes", Saludo.encabezado("", 15))
        assertEquals("Buenas noches", Saludo.encabezado("   ", 22))
    }

    @Test
    fun `partes separa momento y primer nombre`() {
        assertEquals(
            Saludo.Encabezado(momento = "Buenas tardes", nombre = "Maikol"),
            Saludo.partes("Maikol Henao", 15)
        )
    }

    @Test
    fun `partes sin nombre deja el nombre vacio`() {
        assertEquals(
            Saludo.Encabezado(momento = "Buenos días", nombre = ""),
            Saludo.partes(null, 9)
        )
    }
}
