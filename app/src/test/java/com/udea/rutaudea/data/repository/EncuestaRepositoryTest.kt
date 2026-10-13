package com.udea.rutaudea.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.udea.rutaudea.data.local.dao.EncuestaDao
import com.udea.rutaudea.data.local.entity.EncuestaEntity
import com.udea.rutaudea.domain.model.EncuestaSatisfaccion
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests del repositorio de la encuesta en modo offline (sin red): la
 * respuesta se guarda en Room aunque Firestore falle, queda pendiente de
 * sincronizar y nunca se lanza una excepción.
 */
class EncuestaRepositoryTest {

    private fun completa(): EncuestaSatisfaccion = EncuestaSatisfaccion(
        tipoInstitucion = "Universidad",
        nombreInstitucion = "UdeA",
        semestre = "5°",
        presentoExamen = true,
        likertSimulacro = 5,
        likertPractica = 5,
        likertProgreso = 4,
        likertInfo = 4,
        claridadRL = 4,
        claridadCL = 5,
        dificultadAdecuada = 3,
        facilidadUso = 5,
        nps = 10
    )

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

    private fun daoVacio(): EncuestaDao = mockk {
        coEvery { getEncuesta(any()) } returns null
        coEvery { upsert(any()) } returns Unit
        coEvery { marcarSincronizada(any()) } returns Unit
    }

    @Before
    fun setUp() {
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
    fun `guardar sin red deja la respuesta en local como pendiente`() = runTest {
        val dao = daoVacio()
        val repo = EncuestaRepository(authConSesion(), firestoreSinRed(), dao) { 123L }

        val resultado = repo.guardar(completa())

        assertTrue(resultado is EncuestaRepository.ResultadoGuardado.Guardada)
        assertFalse((resultado as EncuestaRepository.ResultadoGuardado.Guardada).sincronizada)
        coVerify(exactly = 1) { dao.upsert(match<EncuestaEntity> { !it.synced }) }
        coVerify(exactly = 0) { dao.marcarSincronizada(any()) }
    }

    @Test
    fun `guardar encuesta invalida no toca ni local ni nube`() = runTest {
        val dao = daoVacio()
        val repo = EncuestaRepository(authConSesion(), firestoreSinRed(), dao)

        val resultado = repo.guardar(EncuestaSatisfaccion())

        assertTrue(resultado is EncuestaRepository.ResultadoGuardado.Invalida)
        assertFalse((resultado as EncuestaRepository.ResultadoGuardado.Invalida).errores.isEmpty())
        coVerify(exactly = 0) { dao.upsert(any()) }
    }

    @Test
    fun `guardar sin sesion devuelve SinSesion`() = runTest {
        val repo = EncuestaRepository(authSinSesion(), mockk(relaxed = true), daoVacio())

        val resultado = repo.guardar(completa())

        assertEquals(EncuestaRepository.ResultadoGuardado.SinSesion, resultado)
    }

    @Test
    fun `cargar sin respuesta devuelve null`() = runTest {
        val repo = EncuestaRepository(authConSesion(), firestoreSinRed(), daoVacio())

        assertNull(repo.cargar())
    }

    @Test
    fun `cargar lee primero de local`() = runTest {
        val dao = daoVacio()
        val repo = EncuestaRepository(authConSesion(), firestoreSinRed(), dao)

        repo.cargar()

        coVerify(exactly = 1) { dao.getEncuesta("u1") }
    }

    @Test
    fun `sincronizar sin red devuelve false sin lanzar`() = runTest {
        val dao: EncuestaDao = mockk {
            coEvery { getEncuesta(any()) } returns EncuestaEntity(
                uid = "u1", fechaMs = 1L, tipoInstitucion = "Colegio",
                nombreInstitucion = "IE", grado = "11°", semestre = "",
                presentoExamen = false, likertSimulacro = 5, likertPractica = 5,
                likertProgreso = 5, likertInfo = 5, claridadRL = 5, claridadCL = 5,
                dificultadAdecuada = 4, facilidadUso = 5, nps = 9,
                gustoMas = "", mejorar = "", synced = false
            )
        }
        val repo = EncuestaRepository(authConSesion(), firestoreSinRed(), dao)

        assertFalse(repo.sincronizar())
    }

    @Test
    fun `sincronizar sin pendientes devuelve true`() = runTest {
        val dao: EncuestaDao = mockk {
            coEvery { getEncuesta(any()) } returns null
        }
        val repo = EncuestaRepository(authConSesion(), mockk(relaxed = true), dao)

        assertTrue(repo.sincronizar())
    }
}
