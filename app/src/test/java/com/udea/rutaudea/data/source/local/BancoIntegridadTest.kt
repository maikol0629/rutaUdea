package com.udea.rutaudea.data.source.local

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Integridad del banco empaquetado (`assets/questions/questions.jsonl`).
 *
 * Fase 7: el asset es el JSONL canónico del ETL v2 (`udea-2026-10-v1`,
 * schema 2). Estos tests protegen el contrato antes de publicar:
 * - 400 preguntas con IDs únicos y sin IDs del banco viejo,
 * - estados esperados (269 aprobado / 127 aprobado_con_imagen / 3
 *   pendiente / 1 deshabilitado),
 * - toda fila aprobada trae subtema, dificultad, verificación, 4 opciones,
 *   respuesta A–D e imagen con URL cuando la requiere,
 * - 11 casos de opciones en imagen y 21 preguntas con más de una figura,
 * - cobertura del simulacro (≥40 aprobadas por área).
 *
 * Los tests unitarios corren con el directorio del módulo como cwd.
 */
class BancoIntegridadTest {

    private val archivo = File("src/main/assets/questions/questions.jsonl")

    private val gson = Gson()

    private val preguntas: List<JsonObject> by lazy {
        archivo.readLines()
            .filter { it.isNotBlank() }
            .map { gson.fromJson(it, JsonObject::class.java) }
    }

    private fun JsonObject.texto(campo: String): String? =
        get(campo)?.takeIf { !it.isJsonNull }?.asString

    private fun JsonObject.estado(): String = texto("estado") ?: "pendiente"

    private fun JsonObject.esPublicable(): Boolean =
        estado() == "aprobado" || estado() == "aprobado_con_imagen"

    @Test
    fun banco_tiene400PreguntasConIdsUnicos() {
        assertEquals(400, preguntas.size)
        val ids = preguntas.map { it.get("id").asString }
        assertEquals(400, ids.toSet().size)
    }

    @Test
    fun banco_idsSonDelContratoV2() {
        val invalidos = preguntas.map { it.get("id").asString }
            .filterNot { it.matches(Regex("T[1-7]-P\\d+[b]?")) }
        assertTrue(
            "IDs que no cumplen T*-P*: $invalidos",
            invalidos.isEmpty()
        )
    }

    @Test
    fun banco_estadosEsperados() {
        val porEstado = preguntas.groupingBy { it.estado() }.eachCount()
        assertEquals(269, porEstado["aprobado"])
        assertEquals(127, porEstado["aprobado_con_imagen"])
        assertEquals(3, porEstado["pendiente"])
        assertEquals(1, porEstado["deshabilitado"])
    }

    @Test
    fun banco_pendientesYDeshabilitadaEsperadas() {
        val pendientes = preguntas.filter { it.estado() == "pendiente" }
            .map { it.get("id").asString }.toSet()
        val deshabilitadas = preguntas.filter { it.estado() == "deshabilitado" }
            .map { it.get("id").asString }.toSet()
        assertEquals(setOf("T3-P10", "T4-P30", "T6-P76"), pendientes)
        assertEquals(setOf("T6-P21"), deshabilitadas)
    }

    @Test
    fun banco_aprobadasCumplenElContrato() {
        val letras = listOf("A", "B", "C", "D")
        val invalidas = preguntas.filter { it.esPublicable() }.filter { p ->
            val opciones = p.getAsJsonObject("opciones")
            val imagenes = p.getAsJsonArray("imagenes")
            val necesitaImagen = p.get("requiere_imagen")?.asBoolean == true ||
                p.estado() == "aprobado_con_imagen"
            val tieneImagen = imagenes != null &&
                imagenes.any { img -> img.asJsonObject.texto("url")?.isNotBlank() == true }

            p.texto("subtema").isNullOrBlank() ||
                p.texto("dificultad").isNullOrBlank() ||
                p.get("verificada")?.asBoolean != true ||
                p.texto("respuesta_correcta") !in letras ||
                p.texto("pregunta").isNullOrBlank() ||
                opciones == null ||
                letras.any { opciones.texto(it).isNullOrBlank() } ||
                (necesitaImagen && !tieneImagen)
        }
        assertTrue(
            "aprobadas que incumplen el contrato: ${invalidas.map { it.get("id").asString }}",
            invalidas.isEmpty()
        )
    }

    @Test
    fun banco_versionYEsquemaUnicos() {
        val versiones = preguntas.mapNotNull { it.texto("bank_version") }.toSet()
        val esquemas = preguntas.mapNotNull { it.get("schema_version")?.asInt }.toSet()
        assertEquals(setOf("udea-2026-10-v1"), versiones)
        assertEquals(setOf(2), esquemas)
    }

    @Test
    fun banco_coberturaDelSimulacro() {
        val publicables = preguntas.filter { it.esPublicable() }
        val rl = publicables.count { it.get("area").asString == "razonamiento_logico" }
        val cl = publicables.count { it.get("area").asString == "competencia_lectora" }
        assertEquals(200, preguntas.count { it.get("area").asString == "razonamiento_logico" })
        assertEquals(200, preguntas.count { it.get("area").asString == "competencia_lectora" })
        assertTrue("RL aprobadas: $rl", rl >= 40)
        assertTrue("CL aprobadas: $cl", cl >= 40)
    }

    @Test
    fun banco_imagenesPublicadasConUrl() {
        val imagenes = preguntas.flatMap { p ->
            p.getAsJsonArray("imagenes")?.map { it.asJsonObject } ?: emptyList()
        }
        assertEquals(150, imagenes.size)
        val sinUrl = imagenes.filter { it.texto("url").isNullOrBlank() }
        assertTrue("imágenes sin URL: ${sinUrl.size}", sinUrl.isEmpty())
        assertTrue(
            "imágenes fuera de Firebase Hosting",
            imagenes.all { it.texto("url")?.startsWith("https://") == true }
        )
        val roles = imagenes.groupingBy { it.texto("rol") }.eachCount()
        assertEquals(88, roles["contexto"])
        assertEquals(62, roles["pregunta"])
    }

    @Test
    fun banco_casosEspecialesDeUi() {
        val conMultiples = preguntas.count { (it.getAsJsonArray("imagenes")?.size() ?: 0) > 1 }
        val conOpcionesEnImagen = preguntas.count {
            it.get("opciones_en_imagen")?.asBoolean == true
        }
        assertEquals(21, conMultiples)
        assertEquals(11, conOpcionesEnImagen)
        // T2-P35 necesita tres figuras: dos del contexto T2-C09 y una propia.
        val t2p35 = preguntas.first { it.get("id").asString == "T2-P35" }
        assertEquals(3, t2p35.getAsJsonArray("imagenes").size())
    }

    @Test
    fun banco_contextoIdsReferencianFormatosValidos() {
        val idsContexto = preguntas
            .flatMap { p ->
                p.getAsJsonArray("contexto_ids")?.map { it.asString } ?: emptyList()
            }
        assertTrue(idsContexto.isNotEmpty())
        val invalidos = idsContexto.filterNot { it.matches(Regex("T[1-7]-C\\d+")) }
        assertTrue("contexto_ids inválidos: $invalidos", invalidos.isEmpty())
    }
}
