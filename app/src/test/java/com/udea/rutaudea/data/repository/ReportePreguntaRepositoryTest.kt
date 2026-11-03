package com.udea.rutaudea.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.udea.rutaudea.data.local.dao.ReportePreguntaDao
import com.udea.rutaudea.data.local.entity.ReportePreguntaEntity
import com.udea.rutaudea.domain.model.ModoReporte
import com.udea.rutaudea.domain.model.MotivoReporte
import com.udea.rutaudea.domain.model.ReportePregunta
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
 * Tests del repositorio de reportes en modo offline: el reporte se guarda
 * en Room aunque Firestore falle, queda pendiente de sincronizar y nunca
 * se lanza una excepción.
 */
class ReportePreguntaRepositoryTest {

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

    private fun daoVacio(): ReportePreguntaDao = mockk {
        coEvery { countReporte(any(), any(), any()) } returns 0
        coEvery { upsert(any()) } returns Unit
        coEvery { marcarSincronizado(any()) } returns Unit
        coEvery { getPendientes(any()) } returns emptyList()
        coEvery { getQuestionIdsReportadas(any(), any()) } returns emptyList()
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
    fun `guardar sin red deja el reporte en local como pendiente`() = runTest {
        val dao = daoVacio()
        val repo = ReportePreguntaRepository(
            auth = authConSesion(),
            firestore = firestoreSinRed(),
            dao = dao,
            appVersionCode = 7,
            ahoraMs = { 123L },
            nuevoId = { "r1" }
        )

        val resultado = repo.guardar(reporte())

        assertTrue(resultado is ReportePreguntaRepository.ResultadoReporte.Guardado)
        assertFalse(
            (resultado as ReportePreguntaRepository.ResultadoReporte.Guardado).sincronizada
        )
        coVerify(exactly = 1) {
            dao.upsert(
                match<ReportePreguntaEntity> {
                    it.id == "r1" && !it.synced && it.fechaMs == 123L &&
                        it.appVersionCode == 7 && it.uid == "u1"
                }
            )
        }
        coVerify(exactly = 0) { dao.marcarSincronizado(any()) }
    }

    @Test
    fun `un reporte invalido no toca ni local ni nube`() = runTest {
        val dao = daoVacio()
        val repo = ReportePreguntaRepository(authConSesion(), firestoreSinRed(), dao)

        val resultado = repo.guardar(reporte(motivo = MotivoReporte.OTRA))

        assertTrue(resultado is ReportePreguntaRepository.ResultadoReporte.Invalido)
        assertFalse(
            (resultado as ReportePreguntaRepository.ResultadoReporte.Invalido).errores.isEmpty()
        )
        coVerify(exactly = 0) { dao.upsert(any()) }
    }

    @Test
    fun `guardar sin sesion devuelve SinSesion`() = runTest {
        val repo = ReportePreguntaRepository(authSinSesion(), mockk(relaxed = true), daoVacio())

        assertEquals(
            ReportePreguntaRepository.ResultadoReporte.SinSesion,
            repo.guardar(reporte())
        )
    }

    @Test
    fun `un reporte duplicado en la misma sesion se rechaza`() = runTest {
        val dao = daoVacio()
        coEvery { dao.countReporte("u1", "q1", "s1") } returns 1
        val repo = ReportePreguntaRepository(authConSesion(), firestoreSinRed(), dao)

        val resultado = repo.guardar(reporte())

        assertEquals(
            ReportePreguntaRepository.ResultadoReporte.Duplicado,
            resultado
        )
        coVerify(exactly = 0) { dao.upsert(any()) }
    }

    @Test
    fun `sincronizar sin pendientes devuelve true`() = runTest {
        val repo = ReportePreguntaRepository(
            authConSesion(),
            mockk(relaxed = true),
            daoVacio()
        )

        assertTrue(repo.sincronizarPendientes())
    }

    @Test
    fun `sincronizar con un pendiente y sin red devuelve false`() = runTest {
        val dao = daoVacio()
        coEvery { dao.getPendientes("u1") } returns listOf(
            ReportePreguntaEntity(
                id = "r1",
                uid = "u1",
                questionId = "q1",
                motivo = "enunciado_confuso",
                comentario = "",
                modo = "simulacro",
                sesionId = "s1",
                area = "",
                subtema = "",
                bancoVersion = null,
                respuestaUsuario = null,
                respuestaCorrecta = null,
                fechaMs = 1L,
                appVersionCode = 1,
                estado = ReportePregunta.ESTADO_PENDIENTE,
                synced = false
            )
        )
        val repo = ReportePreguntaRepository(authConSesion(), firestoreSinRed(), dao)

        assertFalse(repo.sincronizarPendientes())
        coVerify(exactly = 0) { dao.marcarSincronizado(any()) }
    }

    @Test
    fun `las preguntas reportadas de una sesion se leen de local`() = runTest {
        val dao = daoVacio()
        coEvery { dao.getQuestionIdsReportadas("u1", "s1") } returns listOf("q1", "q2")
        val repo = ReportePreguntaRepository(authConSesion(), firestoreSinRed(), dao)

        assertEquals(setOf("q1", "q2"), repo.getReportadasDeSesion("s1"))
    }
}
