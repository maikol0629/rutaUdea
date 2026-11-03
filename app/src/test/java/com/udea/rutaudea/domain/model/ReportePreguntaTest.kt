package com.udea.rutaudea.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests de validación del reporte de pregunta: la pregunta es obligatoria,
 * el comentario es opcional salvo para el motivo «Otra» y tiene un límite
 * de longitud.
 */
class ReportePreguntaTest {

    private fun reporte(
        motivo: MotivoReporte = MotivoReporte.ENUNCIADO_CONFUSO,
        comentario: String = ""
    ) = ReportePregunta(
        questionId = "q1",
        motivo = motivo,
        comentario = comentario,
        modo = ModoReporte.SIMULACRO,
        sesionId = "s1"
    )

    @Test
    fun `un reporte con motivo y sin comentario es valido`() {
        assertTrue(reporte().validar().isEmpty())
    }

    @Test
    fun `el motivo otra exige comentario`() {
        val errores = reporte(motivo = MotivoReporte.OTRA).validar()

        assertFalse(errores.isEmpty())
        assertTrue(errores.any { it.contains("Cuéntanos") })
    }

    @Test
    fun `otra con comentario es valido`() {
        assertTrue(reporte(motivo = MotivoReporte.OTRA, comentario = "Falta la figura").validar().isEmpty())
    }

    @Test
    fun `una pregunta sin id es invalida`() {
        assertFalse(reporte().copy(questionId = " ").validar().isEmpty())
    }

    @Test
    fun `el comentario no puede superar el maximo`() {
        val largo = "a".repeat(ReportePregunta.MAX_COMENTARIO + 1)

        assertFalse(reporte(comentario = largo).validar().isEmpty())
        assertTrue(
            reporte(comentario = "a".repeat(ReportePregunta.MAX_COMENTARIO))
                .validar()
                .isEmpty()
        )
    }

    @Test
    fun `las claves de los motivos son estables`() {
        assertEquals("enunciado_confuso", MotivoReporte.ENUNCIADO_CONFUSO.clave)
        assertEquals(MotivoReporte.ORTOGRAFIA, MotivoReporte.fromClave("ortografia"))
        assertEquals(MotivoReporte.OTRA, MotivoReporte.fromClave("desconocido"))
        assertEquals(ModoReporte.SIMULACRO, ModoReporte.fromClave("simulacro"))
        assertEquals(ModoReporte.PRACTICA, ModoReporte.fromClave(null))
    }
}
