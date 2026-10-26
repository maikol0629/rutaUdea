package com.udea.rutaudea.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidadorCorreoTest {

    @Test
    fun `acepta correos con formato valido`() {
        assertTrue(ValidadorCorreo.esValido("ana.perez@udea.edu.co"))
        assertTrue(ValidadorCorreo.esValido("ana+rutas@correo.com"))
        assertTrue(ValidadorCorreo.esValido("  ana@correo.com  "))
    }

    @Test
    fun `rechaza correos invalidos`() {
        assertFalse(ValidadorCorreo.esValido(""))
        assertFalse(ValidadorCorreo.esValido("ana"))
        assertFalse(ValidadorCorreo.esValido("ana@"))
        assertFalse(ValidadorCorreo.esValido("@correo.com"))
        assertFalse(ValidadorCorreo.esValido("ana@correo"))
        assertFalse(ValidadorCorreo.esValido("ana correo@correo.com"))
    }
}
