package com.udea.rutaudea.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.udea.rutaudea.data.local.dao.RachaDao
import com.udea.rutaudea.data.local.entity.RachaEntity
import com.udea.rutaudea.domain.model.Racha
import com.udea.rutaudea.domain.services.Reloj
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests del repositorio de racha en modo offline (sin red): el contador
 * se calcula y guarda en Room aunque Firestore no esté disponible, y la
 * sincronización reporta el pendiente sin lanzar excepciones.
 */
class RachaRepositoryTest {

    private val dia = 20_000L
    private val ahora = 1_000_000L

    private val reloj = object : Reloj {
        override fun diaActual(): Long = dia
        override fun ahoraMs(): Long = ahora
    }

    private fun authConSesion(uid: String = "u1"): FirebaseAuth {
        val user = mockk<FirebaseUser> {
            every { this@mockk.uid } returns uid
        }
        return mockk {
            every { currentUser } returns user
        }
    }

    private fun authSinSesion(): FirebaseAuth = mockk {
        every { currentUser } returns null
    }

    /** Firestore que siempre falla, simulando ausencia de red. */
    private fun firestoreSinRed(): FirebaseFirestore = mockk {
        every { collection(any()) } throws RuntimeException("sin red")
    }

    private fun daoVacio(): RachaDao = mockk {
        coEvery { getRacha(any()) } returns null
        coEvery { upsert(any()) } returns Unit
        coEvery { marcarSincronizada(any()) } returns Unit
    }

    @Before
    fun setUp() {
        // android.util.Log no existe en tests JVM: se mockea para que las
        // ramas offline (que registran el fallo) no lancen excepción.
        mockkStatic(android.util.Log::class)
        every { android.util.Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { android.util.Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { android.util.Log.w(any<String>(), any<String>()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(android.util.Log::class)
    }

    @Test
    fun `cargar sin sesion devuelve una racha vacia`() = runTest {
        val repo = RachaRepository(authSinSesion(), mockk(relaxed = true), daoVacio(), reloj)

        assertEquals(Racha(), repo.cargar())
    }

    @Test
    fun `cargar lee la racha local del usuario`() = runTest {
        val dao = daoVacio()
        coEvery { dao.getRacha("u1") } returns RachaEntity("u1", 3, 5, dia, ahora, synced = true)
        val repo = RachaRepository(authConSesion(), mockk(relaxed = true), dao, reloj)

        val racha = repo.cargar()

        assertEquals(3, racha.actual)
        assertEquals(5, racha.mejor)
    }

    @Test
    fun `cargarVigente muestra cero si pasaron dos o mas dias`() = runTest {
        val dao = daoVacio()
        coEvery { dao.getRacha("u1") } returns RachaEntity("u1", 5, 5, dia - 3, ahora, synced = true)
        val repo = RachaRepository(authConSesion(), mockk(relaxed = true), dao, reloj)

        val racha = repo.cargarVigente()

        assertEquals(0, racha.actual)
        assertEquals(5, racha.mejor)
    }

    @Test
    fun `registrarActividad sin sesion no toca la base local`() = runTest {
        val dao = daoVacio()
        val repo = RachaRepository(authSinSesion(), mockk(relaxed = true), dao, reloj)

        assertEquals(Racha(), repo.registrarActividad())
        coVerify(exactly = 0) { dao.upsert(any()) }
    }

    @Test
    fun `registrarActividad guarda en local aunque no haya red`() = runTest {
        val dao = daoVacio()
        val repo = RachaRepository(authConSesion(), firestoreSinRed(), dao, reloj)

        val racha = repo.registrarActividad()

        assertEquals(1, racha.actual)
        assertEquals(1, racha.mejor)
        coVerify(exactly = 1) { dao.upsert(match { !it.synced && it.actual == 1 }) }
        coVerify(exactly = 0) { dao.marcarSincronizada(any()) }
    }

    @Test
    fun `registrarActividad suma un dia consecutivo sin red`() = runTest {
        val dao = daoVacio()
        coEvery { dao.getRacha("u1") } returns RachaEntity("u1", 2, 4, dia - 1, ahora - 1, synced = true)
        val repo = RachaRepository(authConSesion(), firestoreSinRed(), dao, reloj)

        val racha = repo.registrarActividad()

        assertEquals(3, racha.actual)
        assertEquals(4, racha.mejor)
    }

    @Test
    fun `registrarActividad el mismo dia no duplica el conteo`() = runTest {
        val dao = daoVacio()
        coEvery { dao.getRacha("u1") } returns RachaEntity("u1", 2, 2, dia, ahora - 1, synced = true)
        val repo = RachaRepository(authConSesion(), firestoreSinRed(), dao, reloj)

        val racha = repo.registrarActividad()

        assertEquals(2, racha.actual)
        assertEquals(2, racha.mejor)
    }

    @Test
    fun `sincronizar sin sesion devuelve falso`() = runTest {
        val repo = RachaRepository(authSinSesion(), mockk(relaxed = true), daoVacio(), reloj)

        assertFalse(repo.sincronizar())
    }

    @Test
    fun `sincronizar con pendiente y sin red devuelve falso y conserva el local`() = runTest {
        val dao = daoVacio()
        coEvery { dao.getRacha("u1") } returns RachaEntity("u1", 2, 2, dia, ahora, synced = false)
        val repo = RachaRepository(authConSesion(), firestoreSinRed(), dao, reloj)

        assertFalse(repo.sincronizar())
        coVerify(exactly = 0) { dao.marcarSincronizada(any()) }
    }

    @Test
    fun `sincronizar sin nada pendiente ni nube devuelve verdadero`() = runTest {
        val dao = daoVacio()
        val repo = RachaRepository(authConSesion(), firestoreSinRed(), dao, reloj)

        assertTrue(repo.sincronizar())
    }
}
