package com.udea.rutaudea.data.source.local

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Integridad del banco empaquetado (`assets/questions/questions.jsonl`).
 * Protege contra regresiones de la limpieza del banco v3:
 * - sin frase plantilla repetida consecutivamente en los texto_base,
 * - exactamente las 12 preguntas RL rotas deshabilitadas,
 * - el banco aprobado sigue cubriendo el simulacro (≥40 por área).
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

    private fun JsonObject.textoBase(): String? =
        get("texto_base")?.takeIf { !it.isJsonNull }?.asString

    private fun JsonObject.estado(): String =
        get("estado")?.takeIf { !it.isJsonNull }?.asString ?: "aprobado"

    @Test
    fun banco_tiene200Preguntas() {
        assertEquals(200, preguntas.size)
    }

    @Test
    fun banco_sinFrasePlantillaRepetida() {
        val repetidas = preguntas.filter { p ->
            val tb = p.textoBase() ?: return@filter false
            FRASE_REGEX.containsMatchIn(tb)
        }
        assertTrue(
            "texto_base con la frase repetida consecutivamente: ${repetidas.map { it.get("id").asString }}",
            repetidas.isEmpty()
        )
    }

    @Test
    fun banco_exactamenteLas12RotasDeshabilitadas() {
        val deshabilitadas = preguntas.filter { it.estado() == "deshabilitado" }
            .map { it.get("id").asString }.toSet()
        assertEquals(DESHABILITADAS_ESPERADAS, deshabilitadas)
    }

    @Test
    fun banco_aprobadoCubreElSimulacro() {
        val rl = preguntas.count {
            it.get("area").asString == "razonamiento_logico" && it.estado() == "aprobado"
        }
        val cl = preguntas.count {
            it.get("area").asString == "competencia_lectora" && it.estado() == "aprobado"
        }
        assertTrue("RL aprobadas: $rl", rl >= 40)
        assertTrue("CL aprobadas: $cl", cl >= 40)
    }

    @Test
    fun banco_scrapingDeAlex52Truncado() {
        val p52 = preguntas.first { it.get("id").asString == "profe_alex_52" }
        assertTrue(
            "profe_alex_52 seguiría trayendo basura de scraping (${p52.get("pregunta").asString.length} chars)",
            p52.get("pregunta").asString.length < 1000
        )
    }

    companion object {
        /** La frase de relleno dos o más veces seguidas (tolerante a tildes). */
        private val FRASE_REGEX = Regex(
            "(?:El tema sigue siendo objeto de estudio y an[áa]lisis constante " +
                "en la comunidad acad[ée]mica y profesional\\.\\s*){2,}"
        )

        private val DESHABILITADAS_ESPERADAS = setOf(
            "profe_alex_12", "profe_alex_14", "profe_alex_15", "profe_alex_18",
            "profe_alex_19", "profe_alex_36", "profe_alex_51", "profe_alex_52",
            "RL-ORG-035", "RL-ORG-037", "RL-ORG-038", "RL-ORG-039"
        )
    }
}
