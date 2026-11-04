package com.udea.rutaudea.ui.screen.simulacro

import androidx.lifecycle.SavedStateHandle
import com.google.gson.Gson
import com.udea.rutaudea.data.repository.PruebaRepository
import com.udea.rutaudea.data.repository.QuestionRepository
import com.udea.rutaudea.data.repository.ReportePreguntaRepository
import com.udea.rutaudea.data.repository.SimulationRepository
import com.udea.rutaudea.domain.model.MotivoReporte
import com.udea.rutaudea.domain.model.Question
import com.udea.rutaudea.domain.model.SimulacroConfig
import com.udea.rutaudea.domain.model.TipoSimulacro
import com.udea.rutaudea.domain.services.QuestionSelector
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

/**
 * Tests de la pausa/persistencia del simulacro al cambiar de app:
 * overlay al volver, resumir, botón atrás y finalización por tiempo.
 *
 * `relojActivo = false` desactiva el bucle infinito del cronómetro.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SimulacroViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun pregunta(
        id: String,
        correcta: String = "A",
        area: String = "razonamiento_logico"
    ) = Question(
        id = id,
        area = area,
        subtema = if (area == "razonamiento_logico") "RL07 Sucesiones" else "CL02 Inferencia",
        dificultad = "baja",
        pregunta = "¿$id?",
        opciones = listOf("op A", "op B", "op C", "op D"),
        respuestaCorrecta = correcta,
        explicacion = "explicación $id"
    )

    private fun sahConSeleccion(ids: List<String>) =
        SavedStateHandle(mapOf("selection_ids_json" to Gson().toJson(ids)))

    private fun viewModel(
        preguntas: List<Question>,
        sah: SavedStateHandle,
        reporteRepository: ReportePreguntaRepository = mockk(relaxed = true) {
            coEvery { getReportadasDeSesion(any()) } returns emptySet()
        },
        tipo: TipoSimulacro = TipoSimulacro.COMPLETO,
        selector: QuestionSelector = mockk(relaxed = true),
        simulationRepository: SimulationRepository = mockk(relaxed = true),
        pruebaRepository: PruebaRepository = mockk(relaxed = true),
        banco: List<Question> = emptyList()
    ): SimulacroViewModel {
        val repository = mockk<QuestionRepository> {
            coEvery { getById(any()) } answers {
                val id = firstArg<String>()
                preguntas.firstOrNull { it.id == id }
            }
            coEvery { getQuestions(limit = 1000) } returns banco
            coEvery { leerConfiguracionSimulacro() } returns SimulacroConfig()
        }
        return SimulacroViewModel(
            selector = selector,
            repository = repository,
            simulationRepository = simulationRepository,
            pruebaRepository = pruebaRepository,
            reporteRepository = reporteRepository,
            savedStateHandle = sah,
            tipo = tipo,
            relojActivo = false
        )
    }

    @Test
    fun `al cambiar de app y volver se muestra el overlay de pausa`() = runTest {
        val vm = viewModel(
            listOf(pregunta("q1"), pregunta("q2")),
            sahConSeleccion(listOf("q1", "q2"))
        )

        assertFalse(vm.uiState.value.showResumeOverlay)

        vm.onAppBackgrounded()
        assertFalse(vm.uiState.value.showResumeOverlay) // aún no vuelve

        vm.onAppResumed()
        assertTrue(vm.uiState.value.showResumeOverlay)
    }

    @Test
    fun `sin background previo no se muestra la pausa`() = runTest {
        val vm = viewModel(listOf(pregunta("q1")), sahConSeleccion(listOf("q1")))

        vm.onAppResumed()

        assertFalse(vm.uiState.value.showResumeOverlay)
    }

    @Test
    fun `el boton atras abre la pausa y resumir la oculta`() = runTest {
        val vm = viewModel(listOf(pregunta("q1")), sahConSeleccion(listOf("q1")))

        vm.onPauseRequested()
        assertTrue(vm.uiState.value.showResumeOverlay)

        vm.onResumeConfirmed()
        assertFalse(vm.uiState.value.showResumeOverlay)
    }

    @Test
    fun `al abandonar se oculta la pausa sin finalizar el simulacro`() = runTest {
        val vm = viewModel(
            listOf(pregunta("q1"), pregunta("q2")),
            sahConSeleccion(listOf("q1", "q2"))
        )

        vm.onAppBackgrounded()
        vm.onAppResumed()
        assertTrue(vm.uiState.value.showResumeOverlay)

        vm.onAbandonar()

        assertFalse(vm.uiState.value.showResumeOverlay)
        assertFalse(vm.uiState.value.isFinished)
    }

    @Test
    fun `un simulacro finalizado no muestra la pausa`() = runTest {
        val vm = viewModel(
            listOf(pregunta("q1"), pregunta("q2")),
            sahConSeleccion(listOf("q1", "q2"))
        )

        vm.onOptionSelected("A")
        vm.onNextOrFinish()
        vm.onOptionSelected("A")
        vm.onNextOrFinish() // última → finaliza

        assertTrue(vm.uiState.value.isFinished)

        vm.onAppBackgrounded()
        vm.onAppResumed()
        assertFalse(vm.uiState.value.showResumeOverlay)
    }

    @Test
    fun `reportar una pregunta la marca como reportada y confirma el envio`() = runTest {
        val reporteRepository = mockk<ReportePreguntaRepository> {
            coEvery { getReportadasDeSesion(any()) } returns emptySet()
            coEvery { guardar(any()) } returns
                ReportePreguntaRepository.ResultadoReporte.Guardado(sincronizada = false)
        }
        val vm = viewModel(
            listOf(pregunta("q1")),
            sahConSeleccion(listOf("q1")),
            reporteRepository
        )

        vm.onReportarClick()
        assertTrue(vm.reporteEstado.value.visible)

        vm.onReporteEnviar(MotivoReporte.ENUNCIADO_CONFUSO, "  no se entiende  ")

        assertTrue("q1" in vm.reportadas.value)
        assertNotNull(vm.reporteEstado.value.mensaje)
        assertFalse(vm.reporteEstado.value.enviando)
    }

    @Test
    fun `la prueba corta finaliza al superar sus 30 minutos`() = runTest {
        val sah = SavedStateHandle(
            mapOf(
                "selection_ids_json" to Gson().toJson(listOf("q1", "q2")),
                // Arrancó hace 31 minutos (> 30 del límite de la prueba).
                "start_time_ms" to (System.currentTimeMillis() - 31L * 60 * 1000)
            )
        )
        val vm = viewModel(
            listOf(pregunta("q1"), pregunta("q2")),
            sah,
            tipo = TipoSimulacro.PRUEBA
        )

        assertTrue(vm.uiState.value.isFinished)
    }

    @Test
    fun `la prueba corta no finaliza antes de sus 30 minutos`() = runTest {
        val sah = SavedStateHandle(
            mapOf(
                "selection_ids_json" to Gson().toJson(listOf("q1", "q2")),
                // Arrancó hace 29 minutos: aún tiene tiempo.
                "start_time_ms" to (System.currentTimeMillis() - 29L * 60 * 1000)
            )
        )
        val vm = viewModel(
            listOf(pregunta("q1"), pregunta("q2")),
            sah,
            tipo = TipoSimulacro.PRUEBA
        )

        assertFalse(vm.uiState.value.isFinished)
    }

    @Test
    fun `la prueba excluye solo preguntas de pruebas recientes`() = runTest {
        val banco = (1..20).flatMap { i ->
            listOf(
                pregunta("rl_$i", area = "razonamiento_logico"),
                pregunta("cl_$i", area = "competencia_lectora")
            )
        }
        val simulationRepo = mockk<SimulationRepository>(relaxed = true)
        val pruebaRepo = mockk<PruebaRepository>(relaxed = true) {
            coEvery { getQuestionIdsFromRecentPruebas(any()) } returns emptySet()
        }

        val vm = viewModel(
            preguntas = banco,
            sah = SavedStateHandle(),
            tipo = TipoSimulacro.PRUEBA,
            selector = QuestionSelector(Random(1)),
            simulationRepository = simulationRepo,
            pruebaRepository = pruebaRepo,
            banco = banco
        )

        assertEquals(20, vm.uiState.value.questions.size)
        coVerify {
            pruebaRepo.getQuestionIdsFromRecentPruebas(QuestionSelector.SIMULACROS_RECIENTES)
        }
        coVerify(exactly = 0) { simulationRepo.getQuestionIdsFromRecentSimulacros(any()) }
    }

    @Test
    fun `si el tiempo se agoto durante la muerte del proceso se finaliza`() = runTest {
        val sah = SavedStateHandle(
            mapOf(
                "selection_ids_json" to Gson().toJson(listOf("q1", "q2")),
                // Arrancó hace 121 minutos (> 120 del límite).
                "start_time_ms" to (System.currentTimeMillis() - 121L * 60 * 1000)
            )
        )
        val vm = viewModel(listOf(pregunta("q1"), pregunta("q2")), sah)

        assertTrue(vm.uiState.value.isFinished)
        assertNotNull(sah.get<Int>("simulation_score"))
    }
}
