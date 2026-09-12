package com.udea.rutaudea.data.source.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.udea.rutaudea.data.local.entity.QuestionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Carga el banco de preguntas desde un archivo JSONL empaquetado
 * como asset de la app y lo convierte en entidades Room.
 *
 * Estructura esperada por línea (JSONL):
 * {
 *   "id": "...",
 *   "area": "razonamiento_logico" | "competencia_lectora",
 *   "subtema": "...",
 *   "dificultad": "baja" | "media" | "alta",
 *   "texto_base": "...", "contexto": "...",
 *   "pregunta": "...",
 *   "opciones": { "A": "...", "B": "...", "C": "...", "D": "..." },
 *   "respuesta_correcta": "A",
 *   "explicacion": "...",
 *   "fuente": "...", "es_original": false, "verificada": false
 * }
 */
class QuestionJsonlLoader(private val context: Context, private val gson: Gson) {

    /**
     * Lee el asset [assetPath] y devuelve las entidades listas para Room.
     * Ejemplo: "questions/questions.jsonl"
     */
    suspend fun loadFromAsset(assetPath: String): List<QuestionEntity> =
        withContext(Dispatchers.IO) {
            val text = context.assets.open(assetPath).bufferedReader().use { it.readText() }
            parseJsonl(text)
        }

    /** Parsea texto JSONL (una línea = un objeto JSON) a entidades. */
    fun parseJsonl(text: String): List<QuestionEntity> {
        val result = mutableListOf<QuestionEntity>()
        text.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { line ->
                val json = gson.fromJson(line, JsonObject::class.java)
                if (json != null) result.add(parseObject(json))
            }
        return result
    }

    private fun parseObject(json: JsonObject): QuestionEntity {
        val opciones = json.getAsJsonObject("opciones")
        val id = json.get("id")?.asString ?: ""

        // El componente se deriva del área (RL -> razonamiento lógico, CL -> lectura crítica)
        val area = json.get("area")?.asString ?: ""
        val componente = when (area) {
            "razonamiento_logico" -> "Razonamiento Lógico"
            "competencia_lectora" -> "Competencia Lectora"
            else -> null
        }

        return QuestionEntity(
            id = id,
            area = area,
            componente = componente,
            subtema = json.get("subtema")?.asString ?: "",
            competencia = json.get("competencia")?.asString,
            tipoTexto = json.get("tipo_texto")?.asString,
            dificultad = json.get("dificultad")?.asString ?: "media",
            textoBase = json.get("texto_base")?.asString,
            contexto = json.get("contexto")?.asString,
            pregunta = json.get("pregunta")?.asString ?: "",
            opcionA = opciones?.get("A")?.asString ?: "",
            opcionB = opciones?.get("B")?.asString ?: "",
            opcionC = opciones?.get("C")?.asString ?: "",
            opcionD = opciones?.get("D")?.asString ?: "",
            respuestaCorrecta = json.get("respuesta_correcta")?.asString ?: "",
            explicacion = json.get("explicacion")?.asString,
            fuente = json.get("fuente")?.asString,
            tipoFuente = json.get("tipo_fuente")?.asString,
            esOriginal = json.get("es_original")?.asBoolean ?: false,
            verificada = json.get("verificada")?.asBoolean ?: false,
            estado = "aprobado"
        )
    }
}