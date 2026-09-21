package com.udea.rutaudea.data.source.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonNull
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

    private fun JsonElement.getStringOrNull(): String? =
        if (this == null || this is JsonNull) null else asString

    private fun JsonElement.getBooleanOrNull(): Boolean? =
        if (this == null || this is JsonNull) null else asBoolean

    private fun parseObject(json: JsonObject): QuestionEntity {
        val opciones = json.getAsJsonObject("opciones")
        val id = json.get("id")?.getStringOrNull() ?: ""

        // El componente se deriva del área (RL -> razonamiento lógico, CL -> lectura crítica)
        val area = json.get("area")?.getStringOrNull() ?: ""
        val componente = when (area) {
            "razonamiento_logico" -> "Razonamiento Lógico"
            "competencia_lectora" -> "Competencia Lectora"
            else -> null
        }

        return QuestionEntity(
            id = id,
            area = area,
            componente = componente,
            subtema = json.get("subtema")?.getStringOrNull() ?: "",
            competencia = json.get("competencia")?.getStringOrNull(),
            tipoTexto = json.get("tipo_texto")?.getStringOrNull(),
            dificultad = json.get("dificultad")?.getStringOrNull() ?: "media",
            textoBase = json.get("texto_base")?.getStringOrNull(),
            contexto = json.get("contexto")?.getStringOrNull(),
            pregunta = json.get("pregunta")?.getStringOrNull() ?: "",
            opcionA = opciones?.get("A")?.getStringOrNull() ?: "",
            opcionB = opciones?.get("B")?.getStringOrNull() ?: "",
            opcionC = opciones?.get("C")?.getStringOrNull() ?: "",
            opcionD = opciones?.get("D")?.getStringOrNull() ?: "",
            respuestaCorrecta = json.get("respuesta_correcta")?.getStringOrNull() ?: "",
            explicacion = json.get("explicacion")?.getStringOrNull(),
            fuente = json.get("fuente")?.getStringOrNull(),
            tipoFuente = json.get("tipo_fuente")?.getStringOrNull(),
            esOriginal = json.get("es_original")?.getBooleanOrNull() ?: false,
            verificada = json.get("verificada")?.getBooleanOrNull() ?: false,
            estado = "aprobado"
        )
    }
}