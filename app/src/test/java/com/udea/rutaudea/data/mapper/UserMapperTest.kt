package com.udea.rutaudea.data.mapper

import com.google.firebase.Timestamp
import com.udea.rutaudea.domain.model.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class UserMapperTest {

    @Test
    fun `mapea todos los campos del documento`() {
        val user = UserMapper.fromMap(
            uid = "u1",
            data = mapOf(
                "nombre" to "Ana",
                "apellido" to "Pérez",
                "correo" to "ana@udea.edu.co",
                "rol" to "admin",
                "fechaCreacion" to 100L,
                "estado" to "activo",
                "telefono" to "3001234567",
                "ciudad" to "Medellín",
                "institucion" to "UdeA",
                "tipoInstitucion" to "Universidad",
                "gradoSemestre" to "5°",
                "fechaExamen" to Timestamp(Date(200L)),
                "fotoUrl" to "https://foto",
                "proveedor" to "google",
                "onboardingCompletado" to false
            )
        )

        assertEquals("u1", user.uid)
        assertEquals("Ana", user.nombre)
        assertEquals("Pérez", user.apellido)
        assertEquals("ana@udea.edu.co", user.correo)
        assertEquals(User.ROL_ADMIN, user.rol)
        assertEquals(100L, user.fechaCreacion)
        assertEquals("3001234567", user.telefono)
        assertEquals("Medellín", user.ciudad)
        assertEquals("UdeA", user.institucion)
        assertEquals("Universidad", user.tipoInstitucion)
        assertEquals("5°", user.gradoSemestre)
        assertEquals(200L, user.fechaExamen)
        assertEquals("https://foto", user.fotoUrl)
        assertEquals(User.PROVEEDOR_GOOGLE, user.proveedor)
        assertFalse(user.onboardingCompletado)
    }

    @Test
    fun `un documento antiguo sin onboarding se considera completo`() {
        val user = UserMapper.fromMap("u1", mapOf("nombre" to "Ana"))

        assertTrue(user.onboardingCompletado)
        assertEquals(User.ROL_ASPIRANTE, user.rol)
        assertEquals(User.ESTADO_ACTIVO, user.estado)
        assertEquals(User.PROVEEDOR_EMAIL, user.proveedor)
        assertNull(user.fechaExamen)
    }

    @Test
    fun `el mapa de perfil no incluye campos inmutables`() {
        val user = User(
            uid = "u1",
            nombre = "Ana",
            apellido = "Pérez",
            correo = "ana@udea.edu.co",
            rol = User.ROL_ADMIN,
            fechaCreacion = 100L,
            estado = "inactivo",
            proveedor = User.PROVEEDOR_GOOGLE,
            onboardingCompletado = true
        )

        val mapa = UserMapper.toProfileMap(user)

        assertFalse(mapa.containsKey(UserMapper.CAMPO_CORREO))
        assertFalse(mapa.containsKey(UserMapper.CAMPO_ROL))
        assertFalse(mapa.containsKey(UserMapper.CAMPO_ESTADO))
        assertFalse(mapa.containsKey(UserMapper.CAMPO_FECHA_CREACION))
        assertFalse(mapa.containsKey(UserMapper.CAMPO_PROVEEDOR))
        assertEquals("Ana", mapa[UserMapper.CAMPO_NOMBRE])
        assertEquals(true, mapa[UserMapper.CAMPO_ONBOARDING])
    }

    @Test
    fun `el mapa completo asigna fecha de creacion si falta`() {
        val mapa = UserMapper.toMap(User(uid = "u1"))

        assertTrue(mapa[UserMapper.CAMPO_FECHA_CREACION] is Long)
        assertEquals(User.ROL_ASPIRANTE, mapa[UserMapper.CAMPO_ROL])
    }
}
