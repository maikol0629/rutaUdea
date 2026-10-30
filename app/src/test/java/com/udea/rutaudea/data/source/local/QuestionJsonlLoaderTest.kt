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
        assertEquals("pendiente", e.estado)
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
    fun parseJsonl_estadoDelJsonSeRespeta() {
        val jsonl = """
            {"id": "off-1", "area": "razonamiento_logico", "subtema": "RL01", "dificultad": "baja", "pregunta": "p", "opciones": {"A": "1", "B": "2", "C": "3", "D": "4"}, "respuesta_correcta": "A", "verificada": false, "estado": "deshabilitado"}
            {"id": "on-1", "area": "competencia_lectora", "subtema": "CL01", "dificultad": "baja", "pregunta": "p", "opciones": {"A": "1", "B": "2", "C": "3", "D": "4"}, "respuesta_correcta": "A", "verificada": true, "estado": "aprobado"}
        """.trimIndent()

        val entities = loader.parseJsonl(jsonl)

        assertEquals("deshabilitado", entities[0].estado)
        assertEquals("aprobado", entities[1].estado)
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
        assertEquals("pendiente", e.estado)
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

    @Test
    fun parseJsonl_imagenUrlSeMapeaYNuloPorDefecto() {
        val jsonl = """
            {"id": "img-1", "area": "razonamiento_logico", "subtema": "RL01", "dificultad": "media", "pregunta": "p", "opciones": {"A": "1", "B": "2", "C": "3", "D": "4"}, "respuesta_correcta": "A", "imagen_url": "https://storage/fig.png"}
            {"id": "txt-1", "area": "razonamiento_logico", "subtema": "RL01", "dificultad": "media", "pregunta": "p", "opciones": {"A": "1", "B": "2", "C": "3", "D": "4"}, "respuesta_correcta": "A"}
        """.trimIndent()

        val entities = loader.parseJsonl(jsonl)

        assertEquals("https://storage/fig.png", entities[0].imagenUrl)
        assertNull(entities[1].imagenUrl)
    }

    // ----- Contrato v2 (Fase 7) -----

    @Test
    fun parseJsonl_contratoV2MapeaImagenesContextoYMetadata() {
        val jsonl = """
            {"id": "T2-P01", "bank_version": "udea-2026-10-v1", "schema_version": 2, "area": "razonamiento_logico", "componente": "Razonamiento Lógico", "subtema": "RL15 Gráficas y tablas", "dificultad": "media", "tipo": "grupo", "tarea": "T2", "numero": 1, "contexto_ids": ["T2-C01"], "contexto": "Tabla de eliminatoria", "pregunta": "¿Cuántos partidos?", "opciones": {"A": "3", "B": "4", "C": "5", "D": "6"}, "respuesta_correcta": "D", "explicacion": "C(4,2)=6", "imagen_url": "https://host/T2-C01.png", "imagenes": [{"url": "https://host/T2-C01.png", "rol": "contexto", "orden": 1, "alt": "Tabla", "path_origen": "imagenes/T2/T2-C01.png"}], "requiere_imagen": true, "opciones_en_imagen": false, "estado": "aprobado_con_imagen", "verificada": true, "fuente": "pdf", "tipo_fuente": "original", "confianza_modelo": "alta", "revisado_por": "Michael", "fecha_revision": "2026-10-22", "notas": "ok"}
        """.trimIndent()

        val e = loader.parseJsonl(jsonl).single()

        assertEquals("udea-2026-10-v1", e.bancoVersion)
        assertEquals("T2-C01", e.contextoId)
        assertEquals("aprobado_con_imagen", e.estado)
        assertTrue(e.requiereImagen)
        assertFalse(e.opcionesEnImagen)
        assertEquals("Michael", e.revisadoPor)
        assertEquals("2026-10-22", e.fechaRevision)
        assertEquals("alta", e.confianzaModelo)
        assertEquals("T2", e.tarea)
        assertEquals(1, e.numero)
        assertEquals("D", e.respuestaCorrecta)
        assertTrue(e.imagenesJson.contains("\"rol\":\"contexto\""))
    }

    @Test
    fun parseJsonl_opcionesComoListaEsCompatible() {
        val jsonl = """
            {"id": "list-1", "area": "razonamiento_logico", "subtema": "RL01", "dificultad": "baja", "pregunta": "p", "opciones": ["1", "2", "3", "4"], "respuesta_correcta": "B", "estado": "aprobado", "verificada": true}
        """.trimIndent()

        val e = loader.parseJsonl(jsonl).single()
        assertEquals("1", e.opcionA)
        assertEquals("4", e.opcionD)
    }

    @Test
    fun parseJsonl_aprobadaSinVerificarSeDescarta() {
        val jsonl = """
            {"id": "bad-1", "area": "razonamiento_logico", "subtema": "RL01", "dificultad": "baja", "pregunta": "p", "opciones": {"A": "1", "B": "2", "C": "3", "D": "4"}, "respuesta_correcta": "A", "estado": "aprobado", "verificada": false}
        """.trimIndent()

        val reporte = loader.parseJsonlConReporte(jsonl)
        assertTrue(reporte.entidades.isEmpty())
        assertEquals(1, reporte.descartadas.size)
        assertEquals("aprobada_sin_verificar", reporte.descartadas.single().motivo)
    }

    @Test
    fun parseJsonl_aprobadaConImagenSinUrlSeDescarta() {
        val jsonl = """
            {"id": "bad-2", "area": "razonamiento_logico", "subtema": "RL01", "dificultad": "baja", "pregunta": "p", "opciones": {"A": "1", "B": "2", "C": "3", "D": "4"}, "respuesta_correcta": "A", "estado": "aprobado_con_imagen", "verificada": true, "imagenes": [{"rol": "pregunta", "orden": 1}]}
        """.trimIndent()

        val reporte = loader.parseJsonlConReporte(jsonl)
        assertTrue(reporte.entidades.isEmpty())
        assertEquals("aprobada_sin_imagen", reporte.descartadas.single().motivo)
    }

    @Test
    fun parseJsonl_idsDuplicadosSeDescartan() {
        val fila = """{"id": "dup-1", "area": "razonamiento_logico", "subtema": "RL01", "dificultad": "baja", "pregunta": "p", "opciones": {"A": "1", "B": "2", "C": "3", "D": "4"}, "respuesta_correcta": "A"}"""
        val reporte = loader.parseJsonlConReporte("$fila\n$fila")

        assertEquals(1, reporte.entidades.size)
        assertEquals(1, reporte.descartadas.size)
        assertEquals("id_duplicado", reporte.descartadas.single().motivo)
    }

    @Test
    fun parseJsonl_estadoDesconocidoSeDescarta() {
        val jsonl = """
            {"id": "x-1", "area": "razonamiento_logico", "subtema": "RL01", "dificultad": "baja", "pregunta": "p", "opciones": {"A": "1", "B": "2", "C": "3", "D": "4"}, "respuesta_correcta": "A", "estado": "publicado"}
        """.trimIndent()

        val reporte = loader.parseJsonlConReporte(jsonl)
        assertTrue(reporte.entidades.isEmpty())
        assertEquals("estado_invalido", reporte.descartadas.single().motivo)
    }

    @Test
    fun parseJsonl_sinEstadoQuedaPendienteNuncaAprobada() {
        val jsonl = """
            {"id": "p-1", "area": "competencia_lectora", "subtema": "CL01", "dificultad": "media", "pregunta": "p", "opciones": {"A": "1", "B": "2", "C": "3", "D": "4"}, "respuesta_correcta": "A", "verificada": true}
        """.trimIndent()

        val e = loader.parseJsonl(jsonl).single()
        assertEquals("pendiente", e.estado)
    }
}
