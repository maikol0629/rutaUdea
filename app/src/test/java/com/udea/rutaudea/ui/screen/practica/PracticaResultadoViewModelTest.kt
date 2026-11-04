package com.udea.rutaudea.ui.screen.practica

import com.udea.rutaudea.data.repository.PracticeRepository
import com.udea.rutaudea.data.repository.RachaRepository
import com.udea.rutaudea.domain.model.Question
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
 * Verifica que el resultado de práctica guarde la sesión y registre la
 * actividad de la racha una sola vez.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PracticaResultadoViewModelTest {

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
    fun `guarda la practica y registra la racha una vez`() = runTest {
        val practiceRepository = mockk<PracticeRepository>(relaxed = true) {
            coEvery { savePracticeSession(any(), any(), any(), any(), any(), any(), any(), any()) } returns
                Result.success("pr1")
        }
        val rachaRepository = mockk<RachaRepository>(relaxed = true)

        PracticaResultadoViewModel(
            questions = listOf(pregunta),
            practiceRepository = practiceRepository,
            rachaRepository = rachaRepository,
            userAnswers = mapOf(0 to "A"),
            area = "razonamiento_logico",
            subtema = "RL07 Sucesiones",
            score = 1,
            total = 1,
            timeUsedMs = 1_000L,
            sesionId = "pr1"
        )

        coVerify(exactly = 1) {
            practiceRepository.savePracticeSession(
                "razonamiento_logico",
                "RL07 Sucesiones",
                listOf(pregunta),
                mapOf(0 to "A"),
                1,
                1,
                1_000L,
                "pr1"
            )
        }
        coVerify(exactly = 1) { rachaRepository.registrarActividad() }
    }
}
