package com.udea.rutaudea.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests de validación del cuestionario de satisfacción: todo lo obligatorio
 * debe estar completo, las abiertas son opcionales y el grado/semestre solo
 * aplica según el tipo de institución.
 */
class EncuestaSatisfaccionTest {

    private fun completa(): EncuestaSatisfaccion = EncuestaSatisfaccion(
        tipoInstitucion = "Colegio",
        nombreInstitucion = "I. E. Ejemplo",
        grado = "11°",
        presentoExamen = false,
        likertSimulacro = 5,
        likertPractica = 4,
        likertProgreso = 4,
        likertInfo = 5,
        claridadRL = 4,
        claridadCL = 5,
        dificultadAdecuada = 3,
        facilidadUso = 5,
        nps = 9,
        gustoMas = "Los simulacros",
        mejorar = "Más preguntas"
    )

    @Test
    fun `encuesta completa es valida`() {
        assertTrue(completa().esValida())
        assertEquals(emptyList<String>(), completa().validar())
    }

    @Test
    fun `encuesta vacia reporta errores`() {
        val errores = EncuestaSatisfaccion().validar()

        assertFalse(errores.isEmpty())
        assertTrue(errores.any { it.contains("institución") })
    }

    @Test
    fun `colegio exige grado pero no semestre`() {
        val sinGrado = completa().copy(grado = "")
        assertTrue(sinGrado.validar().any { it.contains("grado") })

        val conSemestre = completa().copy(semestre = "3°")
        assertTrue(conSemestre.esValida())
    }

    @Test
    fun `universidad exige semestre pero no grado`() {
        val uni = completa().copy(
            tipoInstitucion = "Universidad",
            grado = "",
            semestre = ""
        )
        assertTrue(uni.validar().any { it.contains("semestre") })

        assertTrue(uni.copy(semestre = "5°").esValida())
    }

    @Test
    fun `las preguntas abiertas son opcionales`() {
        assertTrue(completa().copy(gustoMas = "", mejorar = "").esValida())
    }

    @Test
    fun `nps fuera de rango es invalido`() {
        assertFalse(completa().copy(nps = -1).esValida())
        assertFalse(completa().copy(nps = 11).esValida())
        assertTrue(completa().copy(nps = 0).esValida())
        assertTrue(completa().copy(nps = 10).esValida())
    }

    @Test
    fun `likert fuera de 1 a 5 es invalido`() {
        assertFalse(completa().copy(likertSimulacro = 0).esValida())
        assertFalse(completa().copy(claridadCL = 6).esValida())
    }
}
