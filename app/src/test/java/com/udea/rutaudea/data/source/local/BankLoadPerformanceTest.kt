package com.udea.rutaudea.data.source.local

import com.google.gson.Gson
import com.udea.rutaudea.data.mapper.QuestionMapper
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Fase 8 — prueba de rendimiento de la carga del banco.
 *
 * Mide el camino crítico que ocurre en el dispositivo antes de mostrar la
 * primera pregunta: leer el asset JSONL, parsearlo al contrato v2 y mapearlo
 * al modelo de dominio. El umbral del plan es < 3 s para 400 preguntas.
 *
 * No sustituye la medición en dispositivo (la app agrega inserción en Room y
 * dibujo de Compose); sirve como regresión automática del parseo y del mapper.
 */
class BankLoadPerformanceTest {

    private val archivo = File("src/main/assets/questions/questions.jsonl")
    private val gson = Gson()
    private val loader = QuestionJsonlLoader(mockk(relaxed = true), gson)

    @Test
    fun cargaDe400PreguntasEnMenosDe3Segundos() {
        val texto = archivo.readText()
        // Calentamiento para no medir la inicialización de Gson/clases.
        loader.parseJsonl(texto.take(20_000))

        val inicio = System.nanoTime()
        val entidades = loader.parseJsonl(texto)
        val dominio = QuestionMapper.listToDomain(entidades)
        val transcurridoMs = (System.nanoTime() - inicio) / 1_000_000

        assertEquals(400, entidades.size)
        assertEquals(400, dominio.size)
        assertTrue(
            "La carga de 400 preguntas tardó ${transcurridoMs} ms (umbral 3000 ms)",
            transcurridoMs < 3_000
        )
        println("QA rendimiento: 400 preguntas parseadas y mapeadas en ${transcurridoMs} ms")
    }
}
