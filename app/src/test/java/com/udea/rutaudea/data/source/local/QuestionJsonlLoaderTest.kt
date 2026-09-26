package com.udea.rutaudea.data.source.local

import com.google.gson.Gson
import com.udea.rutaudea.data.local.entity.QuestionEntity
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import org.junit.Assert.*

class QuestionJsonlLoaderTest {

    private val gson = Gson()
    private val mockContext = mockk<android.content.Context>(relaxed = true)
    private val loader = QuestionJsonlLoader(mockContext, gson)

    private val sampleJsonl = """
        {"id": "test-1", "area": "razonamiento_logico", "subtema": "Series", "competencia": "Razonamiento numerico", "dificultad": "baja", "contexto": "", "pregunta": "1 - 2 + 3 - 4", "opciones": {"A": "50", "B": "0", "C": "-20", "D": "-50"}, "respuesta_correcta": "D", "explicacion": "50 pares suman -1", "fuente": "test", "tipo_fuente": "test", "anio": null, "pagina": null, "licencia": "", "es_original": false, "verificada": true, "duplicado": false}
        {"id": "test-2", "area": "competencia_lectora", "subtema": "CL02 Inferencia", "tipo_texto": "argumentativo", "dificultad": "media", "texto_base": "Texto base de prueba", "contexto": "Contexto compartido", "pregunta": "¿Qué se infiere?", "opciones": {"A": "Opción A", "B": "Opción B", "C": "Opción C", "D": "Opción D"}, "respuesta_correcta": "A", "explicacion": "Inferencia correcta", "fuente": "generada", "tipo_fuente": "original", "anio": null, "pagina": null, "licencia": "", "es_original": true, "verificada": false, "duplicado": false}
    """.trimIndent()

    @Test
    fun parseJsonl_parsesTwoQuestionsCorrectly() {
        val entities = loader.parseJsonl(sampleJsonl)

        assertEquals(2, entities.size)
    }

    @Test
    fun parseJsonl_firstQuestionHasCorrectFields() {
        val entities = loader.parseJsonl(sampleJsonl)
        val e = entities[0]

        assertEquals("test-1", e.id)
        assertEquals("razonamiento_logico", e.area)
        assertEquals("Razonamiento Lógico", e.componente)
        assertEquals("Series", e.subtema)
        assertEquals("Razonamiento numerico", e.competencia)
        assertEquals("baja", e.dificultad)
        assertEquals("1 - 2 + 3 - 4", e.pregunta)
        assertEquals("50", e.opcionA)
        assertEquals("0", e.opcionB)
        assertEquals("-20", e.opcionC)
        assertEquals("-50", e.opcionD)
        assertEquals("D", e.respuestaCorrecta)
        assertEquals("50 pares suman -1", e.explicacion)
        assertEquals("aprobado", e.estado)
        assertFalse(e.esOriginal)
        assertTrue(e.verificada)
    }

    @Test
    fun parseJsonl_secondQuestionHasContextFields() {
        val entities = loader.parseJsonl(sampleJsonl)
        val e = entities[1]

        assertEquals("test-2", e.id)
        assertEquals("competencia_lectora", e.area)
        assertEquals("Competencia Lectora", e.componente)
        assertEquals("CL02 Inferencia", e.subtema)
        assertEquals("argumentativo", e.tipoTexto)
        assertEquals("media", e.dificultad)
        assertEquals("Texto base de prueba", e.textoBase)
        assertEquals("Contexto compartido", e.contexto)
        assertEquals("¿Qué se infiere?", e.pregunta)
        assertEquals("Opción A", e.opcionA)
        assertEquals("A", e.respuestaCorrecta)
        assertTrue(e.esOriginal)
        assertFalse(e.verificada)
    }

    @Test
    fun parseJsonl_emptyLinesAreIgnored() {
        val jsonlWithEmptyLines = """
            {"id": "a", "area": "razonamiento_logico", "subtema": "t1", "dificultad": "baja", "pregunta": "p1", "opciones": {"A": "1", "B": "2", "C": "3", "D": "4"}, "respuesta_correcta": "A", "explicacion": "", "fuente": "", "tipo_fuente": "", "es_original": false, "verificada": false, "duplicado": false}

            {"id": "b", "area": "razonamiento_logico", "subtema": "t2", "dificultad": "baja", "pregunta": "p2", "opciones": {"A": "1", "B": "2", "C": "3", "D": "4"}, "respuesta_correcta": "B", "explicacion": "", "fuente": "", "tipo_fuente": "", "es_original": false, "verificada": false, "duplicado": false}
        """.trimIndent()

        val entities = loader.parseJsonl(jsonlWithEmptyLines)
        assertEquals(2, entities.size)
    }

    @Test
    fun parseJsonl_missingOptionalFieldsUsesDefaults() {
        val minimalJsonl = """
            {"id": "min-1", "area": "razonamiento_logico", "subtema": "Test", "dificultad": "baja", "pregunta": "Test?", "opciones": {"A": "1", "B": "2", "C": "3", "D": "4"}, "respuesta_correcta": "A"}
        """.trimIndent()

        val entities = loader.parseJsonl(minimalJsonl)
        val e = entities[0]

        assertEquals("min-1", e.id)
        assertEquals("razonamiento_logico", e.area)
        assertEquals("Test", e.subtema)
        assertEquals("baja", e.dificultad)
        assertEquals("Test?", e.pregunta)
        assertEquals("A", e.respuestaCorrecta)
        assertEquals("aprobado", e.estado)
        assertFalse(e.esOriginal)
        assertFalse(e.verificada)
        assertNull(e.competencia)
        assertNull(e.tipoTexto)
        assertNull(e.textoBase)
        assertNull(e.contexto)
        assertNull(e.contextoId)
        assertNull(e.explicacion)
        assertNull(e.fuente)
        assertNull(e.tipoFuente)
    }
}