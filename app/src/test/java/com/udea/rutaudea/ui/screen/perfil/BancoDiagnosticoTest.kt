package com.udea.rutaudea.ui.screen.perfil

import com.udea.rutaudea.data.sync.ResultadoSync
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests de la tarjeta «Estado del banco» (código puro, sin Android). */
class BancoDiagnosticoTest {

    @Test
    fun `actualizado produce mensaje de exito`() {
        val mensaje = BancoDiagnostico.mensaje(
            ResultadoSync.Actualizado("udea-2026-10-v1", 396)
        )
        assertFalse(mensaje.esError)
        assertTrue(mensaje.esExito)
        assertTrue(mensaje.texto.contains("udea-2026-10-v1"))
        assertTrue(mensaje.texto.contains("396"))
    }

    @Test
    fun `sin cambios produce mensaje de exito`() {
        val mensaje = BancoDiagnostico.mensaje(
            ResultadoSync.SinCambios("udea-2026-10-v1", 396)
        )
        assertFalse(mensaje.esError)
        assertTrue(mensaje.esExito)
        assertTrue(mensaje.texto.contains("última versión"))
    }

    @Test
    fun `sin manifiesto se reporta como error accionable`() {
        val mensaje = BancoDiagnostico.mensaje(ResultadoSync.SinManifiesto)
        assertTrue(mensaje.esError)
        assertTrue(mensaje.texto.contains("publicó"))
    }

    @Test
    fun `sin configuracion se reporta como error`() {
        val mensaje = BancoDiagnostico.mensaje(ResultadoSync.SinConfiguracion)
        assertTrue(mensaje.esError)
        assertTrue(mensaje.texto.contains("Firestore"))
    }

    @Test
    fun `rechazado incluye los motivos`() {
        val mensaje = BancoDiagnostico.mensaje(
            ResultadoSync.Rechazado(listOf("conteo_no_coincide", "version_distinta"))
        )
        assertTrue(mensaje.esError)
        assertTrue(mensaje.texto.contains("conteo_no_coincide"))
    }

    @Test
    fun `error de red conserva el mensaje original`() {
        val mensaje = BancoDiagnostico.mensaje(ResultadoSync.Error("timeout"))
        assertTrue(mensaje.esError)
        assertTrue(mensaje.texto.contains("timeout"))
    }

    @Test
    fun `fecha cero se muestra como nunca`() {
        assertNull(BancoDiagnostico.formatearFechaHora(0L))
        assertNull(BancoDiagnostico.formatearFechaHora(-1L))
    }

    @Test
    fun `fecha valida se formatea con dia y hora`() {
        val texto = BancoDiagnostico.formatearFechaHora(1_700_000_000_000L)
        assertTrue(texto != null && Regex("\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}").matches(texto))
    }

    @Test
    fun `huella se acorta a doce caracteres`() {
        assertNull(BancoDiagnostico.huellaCorta(null))
        assertNull(BancoDiagnostico.huellaCorta("   "))
        assertEquals("abc123", BancoDiagnostico.huellaCorta("abc123"))
        val corta = BancoDiagnostico.huellaCorta("fdfe8d9fd41fcd657dde7900")
        assertEquals("fdfe8d9fd41f…", corta)
    }
}
