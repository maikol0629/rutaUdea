package com.udea.rutaudea.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DatosPerfilTest {

    private fun perfilValido(
        tipo: String = DatosPerfil.TIPO_COLEGIO,
        grado: String = "11°"
    ) = DatosPerfil(
        nombre = "Ana",
        apellido = "Pérez",
        tipoInstitucion = tipo,
        institucion = "I.E. San Juan",
        gradoSemestre = grado
    )

    @Test
    fun `un perfil completo es valido`() {
        assertTrue(perfilValido().esValido())
        assertEquals(emptyList<String>(), perfilValido().validar())
    }

    @Test
    fun `nombre y apellido son obligatorios`() {
        val errores = perfilValido().copy(nombre = "", apellido = " ").validar()

        assertTrue(errores.contains(DatosPerfil.ERROR_NOMBRE))
        assertTrue(errores.contains(DatosPerfil.ERROR_APELLIDO))
    }

    @Test
    fun `debe elegirse un tipo de institucion valido`() {
        val errores = perfilValido().copy(tipoInstitucion = "").validar()

        assertTrue(errores.contains(DatosPerfil.ERROR_TIPO_INSTITUCION))
    }

    @Test
    fun `la institucion es obligatoria`() {
        val errores = perfilValido().copy(institucion = "").validar()

        assertTrue(errores.contains(DatosPerfil.ERROR_INSTITUCION))
    }

    @Test
    fun `colegio exige grado`() {
        val errores = perfilValido(tipo = DatosPerfil.TIPO_COLEGIO, grado = "").validar()

        assertTrue(errores.contains(DatosPerfil.ERROR_GRADO))
    }

    @Test
    fun `universidad exige semestre`() {
        val errores = perfilValido(tipo = DatosPerfil.TIPO_UNIVERSIDAD, grado = "").validar()

        assertTrue(errores.contains(DatosPerfil.ERROR_GRADO))
    }

    @Test
    fun `otra no exige grado ni semestre`() {
        val perfil = perfilValido(tipo = DatosPerfil.TIPO_OTRA, grado = "")

        assertTrue(perfil.esValido())
        assertFalse(perfil.exigeGradoSemestre)
    }

    @Test
    fun `el telefono es opcional pero si se escribe debe ser valido`() {
        assertTrue(perfilValido().copy(telefono = "").esValido())
        assertTrue(perfilValido().copy(telefono = "300 123 4567").esValido())
        assertTrue(perfilValido().copy(telefono = "+57 300-123-4567").esValido())

        val errores = perfilValido().copy(telefono = "abc").validar()
        assertTrue(errores.contains(DatosPerfil.ERROR_TELEFONO))
    }

    @Test
    fun `la fecha del examen debe ser un timestamp positivo si se indica`() {
        assertTrue(perfilValido().copy(fechaExamen = null).esValido())

        val errores = perfilValido().copy(fechaExamen = -1L).validar()
        assertTrue(errores.contains(DatosPerfil.ERROR_FECHA_EXAMEN))
    }

    @Test
    fun `al guardar otra se limpia el grado aunque venga precargado`() {
        val usuario = DatosPerfil(
            nombre = "Ana",
            apellido = "Pérez",
            tipoInstitucion = DatosPerfil.TIPO_OTRA,
            institucion = "Academia",
            gradoSemestre = "5°"
        ).aUsuario(User(uid = "u1"))

        assertEquals("", usuario.gradoSemestre)
        assertTrue(usuario.onboardingCompletado)
    }

    @Test
    fun `aUsuario conserva los campos que no edita el onboarding`() {
        val base = User(
            uid = "u1",
            correo = "ana@udea.edu.co",
            rol = User.ROL_ADMIN,
            estado = User.ESTADO_ACTIVO,
            proveedor = User.PROVEEDOR_GOOGLE
        )

        val usuario = perfilValido().aUsuario(base)

        assertEquals("u1", usuario.uid)
        assertEquals("ana@udea.edu.co", usuario.correo)
        assertEquals(User.ROL_ADMIN, usuario.rol)
        assertEquals(User.PROVEEDOR_GOOGLE, usuario.proveedor)
        assertEquals("Ana", usuario.nombre)
        assertEquals("Pérez", usuario.apellido)
        assertEquals("I.E. San Juan", usuario.institucion)
        assertEquals("11°", usuario.gradoSemestre)
    }

    @Test
    fun `desdeUsuario precarga los datos del perfil`() {
        val usuario = User(
            uid = "u1",
            nombre = "Ana",
            apellido = "Pérez",
            tipoInstitucion = DatosPerfil.TIPO_UNIVERSIDAD,
            institucion = "UdeA",
            gradoSemestre = "5°",
            telefono = "3001234567",
            ciudad = "Medellín",
            fechaExamen = 123L
        )

        assertEquals(
            DatosPerfil("Ana", "Pérez", "Universidad", "UdeA", "5°", "3001234567", "Medellín", 123L),
            DatosPerfil.desdeUsuario(usuario)
        )
    }

    @Test
    fun `las opciones de grado dependen del tipo de institucion`() {
        assertEquals(DatosPerfil.GRADOS, DatosPerfil.opcionesGrado(DatosPerfil.TIPO_COLEGIO))
        assertEquals(DatosPerfil.SEMESTRES, DatosPerfil.opcionesGrado(DatosPerfil.TIPO_UNIVERSIDAD))
        assertEquals(emptyList<String>(), DatosPerfil.opcionesGrado(DatosPerfil.TIPO_OTRA))
    }
}
