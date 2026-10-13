package com.udea.rutaudea.ui.screen.encuesta

import com.udea.rutaudea.data.repository.EncuestaRepository
import com.udea.rutaudea.domain.model.EncuestaSatisfaccion
import io.mockk.coEvery
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests del ViewModel de la encuesta: precarga la respuesta guardada,
 * valida antes de enviar y refleja el estado de sincronización.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EncuestaViewModelTest {

    private fun completa(): EncuestaSatisfaccion = EncuestaSatisfaccion(
        tipoInstitucion = "Colegio",
        nombreInstitucion = "I. E. Ejemplo",
        grado = "10°",
        presentoExamen = false,
        likertSimulacro = 4,
        likertPractica = 4,
        likertProgreso = 4,
        likertInfo = 4,
        claridadRL = 4,
        claridadCL = 4,
        dificultadAdecuada = 4,
        facilidadUso = 4,
        nps = 8
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun repository(
        previa: EncuestaSatisfaccion? = null,
        guardado: EncuestaRepository.ResultadoGuardado =
            EncuestaRepository.ResultadoGuardado.Guardada(sincronizada = true)
    ): EncuestaRepository = mockk {
        coEvery { cargar() } returns previa
        coEvery { guardar(any()) } returns guardado
    }

    @Test
    fun `precarga la respuesta guardada para actualizar`() = runTest {
        val vm = EncuestaViewModel(repository(previa = completa()))

        assertFalse(vm.uiState.value.cargando)
        assertTrue(vm.uiState.value.yaRespondio)
        assertEquals("I. E. Ejemplo", vm.uiState.value.encuesta.nombreInstitucion)
    }

    @Test
    fun `sin respuesta previa empieza vacia`() = runTest {
        val vm = EncuestaViewModel(repository())

        assertFalse(vm.uiState.value.yaRespondio)
        assertEquals("", vm.uiState.value.encuesta.nombreInstitucion)
    }

    @Test
    fun `enviar incompleta muestra errores sin llamar al repositorio`() = runTest {
        val repo = repository()
        val vm = EncuestaViewModel(repo)

        vm.enviar()

        assertFalse(vm.uiState.value.enviada)
        assertFalse(vm.uiState.value.errores.isEmpty())
        io.mockk.coVerify(exactly = 0) { repo.guardar(any()) }
    }

    @Test
    fun `enviar completa marca enviada y sincronizada`() = runTest {
        val vm = EncuestaViewModel(repository())

        vm.actualizar(completa())
        vm.enviar()

        assertTrue(vm.uiState.value.enviada)
        assertTrue(vm.uiState.value.sincronizada)
        assertTrue(vm.uiState.value.yaRespondio)
    }

    @Test
    fun `enviar sin red marca enviada pero pendiente de sincronizar`() = runTest {
        val vm = EncuestaViewModel(
            repository(
                guardado = EncuestaRepository.ResultadoGuardado.Guardada(sincronizada = false)
            )
        )

        vm.actualizar(completa())
        vm.enviar()

        assertTrue(vm.uiState.value.enviada)
        assertFalse(vm.uiState.value.sincronizada)
    }

    @Test
    fun `actualizar limpia los errores`() = runTest {
        val vm = EncuestaViewModel(repository())

        vm.enviar()
        assertFalse(vm.uiState.value.errores.isEmpty())

        vm.actualizar(completa())
        assertTrue(vm.uiState.value.errores.isEmpty())
    }
}
