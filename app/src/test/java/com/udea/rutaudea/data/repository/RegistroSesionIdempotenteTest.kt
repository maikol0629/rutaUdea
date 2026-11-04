package com.udea.rutaudea.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.udea.rutaudea.data.local.dao.ProgressDao
import com.udea.rutaudea.domain.model.Question
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Verifica que registrar una sesión sea idempotente: si el id ya existe
 * en Room (por ejemplo, al volver a entrar a Resultado), no se duplica el
 * registro ni se vuelve a subir a Firestore.
 */
class RegistroSesionIdempotenteTest {

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

    private fun authConSesion(uid: String = "u1"): FirebaseAuth {
        val user = mockk<FirebaseUser> {
            every { this@mockk.uid } returns uid
        }
        return mockk {
            every { currentUser } returns user
        }
    }

    @Before
    fun setUp() {
        // android.util.Log no existe en tests JVM: se mockea para las ramas
        // que registran fallos de red.
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
    fun `un simulacro ya registrado no se guarda de nuevo`() = runTest {
        val dao = mockk<ProgressDao>(relaxed = true) {
            coEvery { countSimulacrosById("s1") } returns 1
        }
        val firestore = mockk<FirebaseFirestore>(relaxed = true)
        val repository = SimulationRepository(authConSesion(), firestore, dao)

        val resultado = repository.saveSimulation(
            questions = listOf(pregunta),
            userAnswers = mapOf(0 to "A"),
            score = 1,
            total = 1,
            timeUsedMs = 1_000L,
            sessionId = "s1"
        )

        assertTrue(resultado.isSuccess)
        assertEquals("s1", resultado.getOrNull())
        coVerify(exactly = 0) { dao.upsertSimulacros(any()) }
        coVerify(exactly = 0) { dao.upsertSimulationQuestions(any()) }
        verify(exactly = 0) { firestore.collection(any<String>()) }
    }

    @Test
    fun `una prueba ya registrada no se guarda de nuevo`() = runTest {
        val dao = mockk<ProgressDao>(relaxed = true) {
            coEvery { countPruebasById("p1") } returns 1
        }
        val firestore = mockk<FirebaseFirestore>(relaxed = true)
        val repository = PruebaRepository(authConSesion(), firestore, dao)

        val resultado = repository.savePrueba(
            questions = listOf(pregunta),
            userAnswers = mapOf(0 to "A"),
            score = 1,
            total = 1,
            timeUsedMs = 1_000L,
            sessionId = "p1"
        )

        assertTrue(resultado.isSuccess)
        assertEquals("p1", resultado.getOrNull())
        coVerify(exactly = 0) { dao.upsertPruebas(any()) }
        coVerify(exactly = 0) { dao.upsertPruebaQuestions(any()) }
        verify(exactly = 0) { firestore.collection(any<String>()) }
    }

    @Test
    fun `una practica ya registrada no se guarda de nuevo`() = runTest {
        val dao = mockk<ProgressDao>(relaxed = true) {
            coEvery { countPracticasById("pr1") } returns 1
        }
        val firestore = mockk<FirebaseFirestore>(relaxed = true)
        val repository = PracticeRepository(authConSesion(), firestore, dao)

        val resultado = repository.savePracticeSession(
            area = "razonamiento_logico",
            subtema = "RL07 Sucesiones",
            questions = listOf(pregunta),
            userAnswers = mapOf(0 to "A"),
            score = 1,
            total = 1,
            timeUsedMs = 1_000L,
            sessionId = "pr1"
        )

        assertTrue(resultado.isSuccess)
        assertEquals("pr1", resultado.getOrNull())
        coVerify(exactly = 0) { dao.upsertPracticas(any()) }
        coVerify(exactly = 0) { dao.upsertPracticeQuestions(any()) }
        verify(exactly = 0) { firestore.collection(any<String>()) }
    }

    @Test
    fun `una sesion nueva si se guarda`() = runTest {
        val dao = mockk<ProgressDao>(relaxed = true) {
            coEvery { countSimulacrosById("s2") } returns 0
        }
        // Sin red: la subida falla, pero el guardado local debe ocurrir.
        val firestore = mockk<FirebaseFirestore> {
            every { collection(any<String>()) } throws RuntimeException("sin red")
        }
        val repository = SimulationRepository(authConSesion(), firestore, dao)

        val resultado = repository.saveSimulation(
            questions = listOf(pregunta),
            userAnswers = mapOf(0 to "A"),
            score = 1,
            total = 1,
            timeUsedMs = 1_000L,
            sessionId = "s2"
        )

        assertTrue(resultado.isSuccess)
        assertEquals("s2", resultado.getOrNull())
        coVerify(exactly = 1) { dao.upsertSimulacros(any()) }
        coVerify(exactly = 1) { dao.upsertSimulationQuestions(any()) }
    }
}
