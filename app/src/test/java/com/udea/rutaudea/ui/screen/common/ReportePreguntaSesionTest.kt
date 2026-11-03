package com.udea.rutaudea.ui.screen.common

import com.udea.rutaudea.data.repository.ReportePreguntaRepository
import com.udea.rutaudea.domain.model.ModoReporte
import com.udea.rutaudea.domain.model.MotivoReporte
import com.udea.rutaudea.domain.model.Question
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests de la lógica compartida de reporte: apertura del diálogo,
 * deduplicación por sesión y mensajes según el resultado del guardado.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReportePreguntaSesionTest {

    private val pregunta = Question(
        id = "q1",
        area = "razonamiento_logico",
        subtema = "Series",
        dificultad = "baja",
        pregunta = "¿q1?",
        opciones = listOf("A", "B"),
        respuestaCorrecta = "A"
    )

    private fun sesion(
        scope: CoroutineScope,
        repository: ReportePreguntaRepository
    ) = ReportePreguntaSesion(repository, scope, "s1", ModoReporte.SIMULACRO)

    private fun repositorio(resultado: ReportePreguntaRepository.ResultadoReporte) =
        mockk<ReportePreguntaRepository> {
            coEvery { getReportadasDeSesion(any()) } returns emptySet()
            coEvery { guardar(any()) } returns resultado
        }

    @Test
    fun `abrir ignora preguntas nulas o bloqueadas`() = runTest {
        val sesion = sesion(this, repositorio(ReportePreguntaRepository.ResultadoReporte.SinSesion))

        sesion.abrir(null)
        assertFalse(sesion.estado.value.visible)

        sesion.abrir(pregunta, bloqueado = true)
        assertFalse(sesion.estado.value.visible)

        sesion.abrir(pregunta)
        assertTrue(sesion.estado.value.visible)
    }

    @Test
    fun `enviar guarda el reporte y marca la pregunta como reportada`() = runTest {
        val repository = repositorio(
            ReportePreguntaRepository.ResultadoReporte.Guardado(sincronizada = true)
        )
        val sesion = sesion(this, repository)

        sesion.abrir(pregunta)
        sesion.enviar(pregunta, "B", MotivoReporte.ENUNCIADO_CONFUSO, "  confuso  ")
        advanceUntilIdle()

        assertTrue("q1" in sesion.reportadas.value)
        assertTrue(sesion.estado.value.visible)
        assertNotNull(sesion.estado.value.mensaje)
        assertFalse(sesion.estado.value.enviando)

        coVerify {
            repository.guardar(match {
                it.questionId == "q1" &&
                    it.comentario == "confuso" &&
                    it.modo == ModoReporte.SIMULACRO &&
                    it.sesionId == "s1" &&
                    it.respuestaUsuario == "B"
            })
        }

        // Una pregunta ya reportada no vuelve a abrir el diálogo.
        sesion.cerrar()
        sesion.abrir(pregunta)
        assertFalse(sesion.estado.value.visible)
    }

    @Test
    fun `un guardado sin sincronizar avisa que queda pendiente`() = runTest {
        val sesion = sesion(
            this,
            repositorio(ReportePreguntaRepository.ResultadoReporte.Guardado(sincronizada = false))
        )

        sesion.enviar(pregunta, null, MotivoReporte.FALTAN_DATOS, "")
        advanceUntilIdle()

        assertTrue(sesion.estado.value.mensaje.orEmpty().contains("conexión"))
    }

    @Test
    fun `un duplicado marca la pregunta y muestra el aviso`() = runTest {
        val sesion = sesion(
            this,
            repositorio(ReportePreguntaRepository.ResultadoReporte.Duplicado)
        )

        sesion.enviar(pregunta, null, MotivoReporte.OTRA, "algo")
        advanceUntilIdle()

        assertTrue("q1" in sesion.reportadas.value)
        assertTrue(sesion.estado.value.mensaje.orEmpty().contains("Ya reportaste"))
    }

    @Test
    fun `sin sesion muestra el error correspondiente`() = runTest {
        val sesion = sesion(
            this,
            repositorio(ReportePreguntaRepository.ResultadoReporte.SinSesion)
        )

        sesion.enviar(pregunta, null, MotivoReporte.OTRA, "algo")
        advanceUntilIdle()

        assertTrue(sesion.estado.value.error.orEmpty().contains("Inicia sesión"))
        assertFalse(sesion.estado.value.enviando)
    }

    @Test
    fun `un reporte invalido muestra los errores de validacion`() = runTest {
        val sesion = sesion(
            this,
            repositorio(
                ReportePreguntaRepository.ResultadoReporte.Invalido(
                    listOf("error uno", "error dos")
                )
            )
        )

        sesion.enviar(pregunta, null, MotivoReporte.OTRA, "")
        advanceUntilIdle()

        assertEquals("error uno error dos", sesion.estado.value.error)
        assertFalse(sesion.estado.value.enviando)
    }

    @Test
    fun `carga las preguntas ya reportadas de la sesion`() = runTest {
        val repository = mockk<ReportePreguntaRepository> {
            coEvery { getReportadasDeSesion("s1") } returns setOf("q1")
        }
        val sesion = sesion(this, repository)

        sesion.cargarReportadas()
        advanceUntilIdle()

        assertEquals(setOf("q1"), sesion.reportadas.value)
    }
}
