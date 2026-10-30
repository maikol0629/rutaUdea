package com.udea.rutaudea.data.source.local

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.udea.rutaudea.data.local.entity.QuestionEntity
import com.udea.rutaudea.data.mapper.QuestionMapper
import com.udea.rutaudea.domain.model.ImagenPregunta
import com.udea.rutaudea.domain.model.Question
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fila del asset que no se insertó en Room y el motivo.
 * Se registra para no descartar datos en silencio (contrato §1.2).
 */
data class FilaDescartada(
    val linea: Int,
    val id: String?,
    val motivo: String
)

/** Resultado de parsear un JSONL: filas válidas y filas descartadas. */
data class ReporteCarga(
    val entidades: List<QuestionEntity>,
    val descartadas: List<FilaDescartada>
) {
    val totalLineas: Int get() = entidades.size + descartadas.size
}

/**
 * Carga el banco de preguntas desde un archivo JSONL empaquetado
 * como asset de la app y lo convierte en entidades Room.
 *
 * Acepta el contrato v2 (`migracion/01_esquema/contrato_datos.md`) y
 * mantiene compatibilidad con el banco viejo:
 * - `opciones` como objeto `{A,B,C,D}` (v2) o como lista `[A,B,C,D]`.
 * - `imagen_url` única e `imagenes[]` con rol/orden/alt.
 * - `contexto_ids[]` además de `contexto`/`texto_base`.
 *
 * Validación (Fase 7): las filas aprobadas deben traer subtema,
 * dificultad, verificación, 4 opciones, respuesta A–D e imágenes con URL
 * cuando las requieran. Las filas inválidas se descartan y se reportan;
 * nunca se promueven a `aprobado` por defecto.
 */
class QuestionJsonlLoader(private val context: Context, private val gson: Gson) {

    /**
     * Lee el asset [assetPath] y devuelve las entidades listas para Room.
     * Ejemplo: "questions/questions.jsonl"
     */
    suspend fun loadFromAsset(assetPath: String): List<QuestionEntity> =
        withContext(Dispatchers.IO) {
            val text = context.assets.open(assetPath).bufferedReader().use { it.readText() }
            val reporte = parseJsonlConReporte(text)
            if (reporte.descartadas.isNotEmpty()) {
                Log.w(
                    TAG,
                    "Asset $assetPath: ${reporte.entidades.size} válidas, " +
                        "${reporte.descartadas.size} descartadas. " +
                        reporte.descartadas.take(10).joinToString { "${it.id ?: "línea ${it.linea}"}: ${it.motivo}" }
                )
            }
            reporte.entidades
        }

    /** Parsea texto JSONL (una línea = un objeto JSON) a entidades. */
    fun parseJsonl(text: String): List<QuestionEntity> = parseJsonlConReporte(text).entidades

    /** Parsea el JSONL y devuelve también las filas descartadas con su motivo. */
    fun parseJsonlConReporte(text: String): ReporteCarga {
        val entidades = mutableListOf<QuestionEntity>()
        val descartadas = mutableListOf<FilaDescartada>()
        val idsVistos = mutableSetOf<String>()

        text.lineSequence().forEachIndexed { index, raw ->
            val linea = index + 1
            val limpia = raw.trim()
            if (limpia.isEmpty()) return@forEachIndexed

            val json = try {
                gson.fromJson(limpia, JsonObject::class.java)
            } catch (_: Exception) {
                null
            }
            if (json == null) {
                descartadas += FilaDescartada(linea, null, "json_invalido")
                return@forEachIndexed
            }

            val id = json.stringOrNull("id")
            val (entidad, motivo) = parseObject(json)
            when {
                entidad == null -> descartadas += FilaDescartada(linea, id, motivo ?: "invalida")
                entidad.id in idsVistos -> descartadas += FilaDescartada(linea, entidad.id, "id_duplicado")
                else -> {
                    idsVistos += entidad.id
                    entidades += entidad
                }
            }
        }
        return ReporteCarga(entidades, descartadas)
    }

    // El receptor es un `JsonElement` no nulo: solo hay que comprobar si es
    // el centinela `JsonNull` (de `get()` sobre una clave ausente ya se
    // encarga el `?.` en cada llamada).
    private fun JsonElement.getStringOrNull(): String? =
        if (isJsonNull) null else asString

    private fun JsonElement.getBooleanOrNull(): Boolean? =
        if (isJsonNull) null else asBoolean

    private fun JsonObject.stringOrNull(campo: String): String? =
        get(campo)?.getStringOrNull()?.takeIf { it.isNotBlank() }

    private fun JsonObject.booleanOrNull(campo: String): Boolean? =
        get(campo)?.takeIf { !it.isJsonNull }?.getBooleanOrNull()

    private fun JsonObject.intOrNull(campo: String): Int? =
        get(campo)?.takeIf { it.isJsonPrimitive }?.asInt

    private fun JsonObject.stringList(campo: String): List<String> {
        val elemento = get(campo) ?: return emptyList()
        if (!elemento.isJsonArray) return emptyList()
        return elemento.asJsonArray.mapNotNull { it.getStringOrNull()?.takeIf(String::isNotBlank) }
    }

    /**
     * Convierte una fila JSON en entidad y valida el contrato.
     * Devuelve `(null, motivo)` cuando la fila no se puede usar.
     */
    private fun parseObject(json: JsonObject): Pair<QuestionEntity?, String?> {
        val id = json.stringOrNull("id") ?: return null to "sin_id"
        val area = json.stringOrNull("area") ?: return null to "sin_area"
        if (area !in AREAS) return null to "area_invalida"

        val pregunta = json.stringOrNull("pregunta") ?: return null to "sin_enunciado"
        val estado = (json.stringOrNull("estado") ?: Question.ESTADO_PENDIENTE).lowercase()
        if (estado !in Question.ESTADOS_VALIDOS) return null to "estado_invalido"

        val opciones = parseOpciones(json.get("opciones"))
        val respuesta = json.stringOrNull("respuesta_correcta")?.uppercase()
        val subtema = json.stringOrNull("subtema").orEmpty()
        val dificultad = json.stringOrNull("dificultad").orEmpty()
        val imagenes = parseImagenes(json)
        val imagenUrl = json.stringOrNull("imagen_url")
        val requiereImagen = json.booleanOrNull("requiere_imagen")
            ?: imagenes.any { it.esVisible() || it.rol == ImagenPregunta.ROL_CONTEXTO }
        val publicable = estado == Question.ESTADO_APROBADO ||
            estado == Question.ESTADO_APROBADO_CON_IMAGEN

        if (publicable) {
            if (subtema.isBlank()) return null to "aprobada_sin_subtema"
            if (dificultad.isBlank()) return null to "aprobada_sin_dificultad"
            if (dificultad.lowercase() !in DIFICULTADES) return null to "dificultad_invalida"
            if (respuesta !in LETRAS) return null to "aprobada_sin_respuesta"
            if (opciones.any { it.isBlank() }) return null to "aprobada_sin_opciones"
            if (json.booleanOrNull("verificada") != true) return null to "aprobada_sin_verificar"
            val tieneUrl = imagenes.any { it.esVisible() } || !imagenUrl.isNullOrBlank()
            if ((requiereImagen || estado == Question.ESTADO_APROBADO_CON_IMAGEN) && !tieneUrl) {
                return null to "aprobada_sin_imagen"
            }
        }

        val entidad = QuestionEntity(
            id = id,
            area = area,
            componente = when (area) {
                "razonamiento_logico" -> "Razonamiento Lógico"
                "competencia_lectora" -> "Competencia Lectora"
                else -> null
            },
            subtema = subtema,
            competencia = json.stringOrNull("competencia"),
            tipoTexto = json.stringOrNull("tipo_texto"),
            dificultad = dificultad.lowercase(),
            textoBase = json.stringOrNull("texto_base"),
            contexto = json.stringOrNull("contexto"),
            contextoId = json.stringList("contexto_ids").firstOrNull(),
            pregunta = pregunta,
            opcionA = opciones[0],
            opcionB = opciones[1],
            opcionC = opciones[2],
            opcionD = opciones[3],
            respuestaCorrecta = respuesta.orEmpty(),
            explicacion = json.stringOrNull("explicacion"),
            fuente = json.stringOrNull("fuente"),
            tipoFuente = json.stringOrNull("tipo_fuente"),
            esOriginal = json.booleanOrNull("es_original") ?: false,
            verificada = json.booleanOrNull("verificada") ?: false,
            estado = estado,
            imagenUrl = imagenUrl,
            imagenesJson = QuestionMapper.imagenesAJson(imagenes),
            opcionesEnImagen = json.booleanOrNull("opciones_en_imagen") ?: false,
            requiereImagen = requiereImagen,
            bancoVersion = json.stringOrNull("bank_version").orEmpty(),
            tarea = json.stringOrNull("tarea"),
            numero = json.intOrNull("numero"),
            confianzaModelo = json.stringOrNull("confianza_modelo"),
            revisadoPor = json.stringOrNull("revisado_por"),
            fechaRevision = json.stringOrNull("fecha_revision"),
            notas = json.stringOrNull("notas")
        )
        return entidad to null
    }

    /** Acepta `{A,B,C,D}` (v2) o `[A,B,C,D]` (banco viejo). */
    private fun parseOpciones(elemento: JsonElement?): List<String> {
        if (elemento == null || elemento.isJsonNull) return listOf("", "", "", "")
        if (elemento.isJsonObject) {
            val obj = elemento.asJsonObject
            return LETRAS.map { letra -> obj.stringOrNull(letra).orEmpty() }
        }
        if (elemento.isJsonArray) {
            val array = elemento.asJsonArray
            return (0 until 4).map { i ->
                array.getOrNull(i)?.getStringOrNull().orEmpty()
            }
        }
        return listOf("", "", "", "")
    }

    private fun JsonArray.getOrNull(index: Int): JsonElement? =
        if (index in 0 until size()) get(index) else null

    /** Lee `imagenes[]`; si no existe, usa `imagen_url` como imagen principal. */
    private fun parseImagenes(json: JsonObject): List<ImagenPregunta> {
        val elemento = json.get("imagenes")
        val imagenes = mutableListOf<ImagenPregunta>()
        if (elemento != null && elemento.isJsonArray) {
            elemento.asJsonArray.forEach { item ->
                if (!item.isJsonObject) return@forEach
                val obj = item.asJsonObject
                val url = obj.stringOrNull("url")
                imagenes += ImagenPregunta(
                    url = url,
                    rol = obj.stringOrNull("rol")
                        ?.takeIf { it == ImagenPregunta.ROL_CONTEXTO }
                        ?: ImagenPregunta.ROL_PREGUNTA,
                    orden = obj.intOrNull("orden") ?: (imagenes.size + 1),
                    alt = obj.stringOrNull("alt"),
                    pathOrigen = obj.stringOrNull("path_origen")
                )
            }
        }
        if (imagenes.isEmpty()) {
            val url = json.stringOrNull("imagen_url")
            if (url != null) {
                imagenes += ImagenPregunta(url = url, rol = ImagenPregunta.ROL_PREGUNTA, orden = 1)
            }
        }
        return imagenes
    }

    companion object {
        private const val TAG = "QuestionJsonlLoader"
        private val AREAS = setOf("razonamiento_logico", "competencia_lectora")
        private val DIFICULTADES = setOf("baja", "media", "alta")
        private val LETRAS = listOf("A", "B", "C", "D")
    }
}
