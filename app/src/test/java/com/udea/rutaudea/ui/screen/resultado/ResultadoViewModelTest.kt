package com.udea.rutaudea.ui.screen.resultado

import com.udea.rutaudea.data.repository.PruebaRepository
import com.udea.rutaudea.data.repository.RachaRepository
import com.udea.rutaudea.data.repository.SimulationRepository
import com.udea.rutaudea.domain.model.Question
import com.udea.rutaudea.domain.model.TipoSimulacro
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Verifica que el resultado elija el repositorio correcto según el tipo
 * de sesión y que la actividad cuente una sola vez para la racha.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ResultadoViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val pregunta = Question(
        id = "q1",
        area = "razonamiento_logico",
        subtema = "RL07 Sucesiones",
        dificultad = "baja",
        pregunta = "¿Cuál es el siguiente término?",
        opciones = listOf("A", "B", "C", "D"),
        respuestaCorrecta = "A",
        explicacion = "Porque sí"
    )

    @Test
    fun `una prueba se guarda en el repositorio de pruebas`() = runTest {
        val simulationRepository = mockk<SimulationRepository>(relaxed = true)
        val pruebaRepository = mockk<PruebaRepository>(relaxed = true) {
            coEvery { savePrueba(any(), any(), any(), any(), any(), any()) } returns
                Result.success("p1")
        }
        val rachaRepository = mockk<RachaRepository>(relaxed = true)

        ResultadoViewModel(
            questions = listOf(pregunta),
            simulationRepository = simulationRepository,
            pruebaRepository = pruebaRepository,
            rachaRepository = rachaRepository,
            userAnswers = mapOf(0 to "A"),
            score = 1,
            total = 1,
            timeUsedMs = 1_000L,
            sesionId = "p1",
            tipo = TipoSimulacro.PRUEBA
        )

        coVerify(exactly = 1) {
            pruebaRepository.savePrueba(
                listOf(pregunta), mapOf(0 to "A"), 1, 1, 1_000L, "p1"
            )
        }
        coVerify(exactly = 0) {
            simulationRepository.saveSimulation(any(), any(), any(), any(), any(), any())
        }
        coVerify(exactly = 1) { rachaRepository.registrarActividad() }
    }

    @Test
    fun `un simulacro completo no usa el repositorio de pruebas`() = runTest {
        val simulationRepository = mockk<SimulationRepository>(relaxed = true) {
            coEvery { saveSimulation(any(), any(), any(), any(), any(), any()) } returns
                Result.success("s1")
        }
        val pruebaRepository = mockk<PruebaRepository>(relaxed = true)
        val rachaRepository = mockk<RachaRepository>(relaxed = true)

        ResultadoViewModel(
            questions = listOf(pregunta),
            simulationRepository = simulationRepository,
            pruebaRepository = pruebaRepository,
            rachaRepository = rachaRepository,
            userAnswers = mapOf(0 to "A"),
            score = 1,
            total = 1,
            timeUsedMs = 1_000L,
            sesionId = "s1",
            tipo = TipoSimulacro.COMPLETO
        )

        coVerify(exactly = 1) {
            simulationRepository.saveSimulation(
                listOf(pregunta), mapOf(0 to "A"), 1, 1, 1_000L, "s1"
            )
        }
        coVerify(exactly = 0) {
            pruebaRepository.savePrueba(any(), any(), any(), any(), any(), any())
        }
        coVerify(exactly = 1) { rachaRepository.registrarActividad() }
    }
}
