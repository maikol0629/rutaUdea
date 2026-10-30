package com.udea.rutaudea.data.mapper

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.udea.rutaudea.data.local.entity.QuestionEntity
import com.udea.rutaudea.domain.model.ImagenPregunta
import com.udea.rutaudea.domain.model.Question

/**
 * Conversiones entre la capa de datos (Room) y el modelo de dominio.
 *
 * Regla de la Fase 7: **no se pierde ningún campo**. Antes de esta fase el
 * mapper forzaba `estado = "aprobado"` en `domainToEntity`; ahora se copian
 * estado, procedencia, verificación, contexto, versión e imágenes.
 */
object QuestionMapper {

    fun entityToDomain(entity: QuestionEntity): Question = Question(
        id = entity.id,
        area = entity.area,
        componente = entity.componente,
        subtema = entity.subtema,
        competencia = entity.competencia,
        tipoTexto = entity.tipoTexto,
        dificultad = entity.dificultad,
        textoBase = entity.textoBase,
        contexto = entity.contexto,
        contextoId = entity.contextoId,
        pregunta = entity.pregunta,
        opciones = listOf(
            entity.opcionA,
            entity.opcionB,
            entity.opcionC,
            entity.opcionD
        ),
        respuestaCorrecta = entity.respuestaCorrecta,
        explicacion = entity.explicacion,
        esOriginal = entity.esOriginal,
        imagenUrl = entity.imagenUrl,
        estado = entity.estado,
        fuente = entity.fuente,
        tipoFuente = entity.tipoFuente,
        verificada = entity.verificada,
        imagenes = imagenesDesdeJson(entity.imagenesJson),
        opcionesEnImagen = entity.opcionesEnImagen,
        requiereImagen = entity.requiereImagen,
        bancoVersion = entity.bancoVersion.takeIf { it.isNotBlank() },
        tarea = entity.tarea,
        numero = entity.numero,
        confianzaModelo = entity.confianzaModelo,
        revisadoPor = entity.revisadoPor,
        fechaRevision = entity.fechaRevision,
        notas = entity.notas
    )

    fun domainToEntity(question: Question): QuestionEntity = QuestionEntity(
        id = question.id,
        area = question.area,
        componente = question.componente,
        subtema = question.subtema,
        competencia = question.competencia,
        tipoTexto = question.tipoTexto,
        dificultad = question.dificultad,
        textoBase = question.textoBase,
        contexto = question.contexto,
        contextoId = question.contextoId,
        pregunta = question.pregunta,
        opcionA = question.opciones.getOrElse(0) { "" },
        opcionB = question.opciones.getOrElse(1) { "" },
        opcionC = question.opciones.getOrElse(2) { "" },
        opcionD = question.opciones.getOrElse(3) { "" },
        respuestaCorrecta = question.respuestaCorrecta,
        explicacion = question.explicacion,
        esOriginal = question.esOriginal,
        imagenUrl = question.imagenUrl,
        estado = question.estado,
        fuente = question.fuente,
        tipoFuente = question.tipoFuente,
        verificada = question.verificada,
        imagenesJson = imagenesAJson(question.imagenes),
        opcionesEnImagen = question.opcionesEnImagen,
        requiereImagen = question.requiereImagen,
        bancoVersion = question.bancoVersion.orEmpty(),
        tarea = question.tarea,
        numero = question.numero,
        confianzaModelo = question.confianzaModelo,
        revisadoPor = question.revisadoPor,
        fechaRevision = question.fechaRevision,
        notas = question.notas
    )

    fun listToDomain(entities: List<QuestionEntity>): List<Question> =
        entities.map { entityToDomain(it) }

    // ----- Serialización de imágenes (JSON en una columna de Room) -----

    /** Lee la lista de imágenes tolerando JSON vacío, nulo o malformado. */
    fun imagenesDesdeJson(json: String?): List<ImagenPregunta> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val raiz = JsonParser.parseString(json)
            if (!raiz.isJsonArray) return emptyList()
            raiz.asJsonArray.mapNotNull { elemento ->
                if (!elemento.isJsonObject) return@mapNotNull null
                val obj = elemento.asJsonObject
                val url = obj.stringOrNull("url")
                ImagenPregunta(
                    url = url?.takeIf { it.isNotBlank() },
                    rol = obj.stringOrNull("rol")
                        ?.takeIf { it == ImagenPregunta.ROL_CONTEXTO }
                        ?: ImagenPregunta.ROL_PREGUNTA,
                    orden = obj.get("orden")?.takeIf { it.isJsonPrimitive }?.asInt ?: 1,
                    alt = obj.stringOrNull("alt"),
                    pathOrigen = obj.stringOrNull("path_origen")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Serializa la lista de imágenes; `[]` cuando no hay ninguna. */
    fun imagenesAJson(imagenes: List<ImagenPregunta>): String {
        if (imagenes.isEmpty()) return "[]"
        val array = JsonArray()
        imagenes.forEach { imagen ->
            array.add(JsonObject().apply {
                addProperty("url", imagen.url)
                addProperty("rol", imagen.rol)
                addProperty("orden", imagen.orden)
                addProperty("alt", imagen.alt)
                addProperty("path_origen", imagen.pathOrigen)
            })
        }
        return array.toString()
    }

    private fun JsonObject.stringOrNull(campo: String): String? =
        get(campo)?.takeIf { it.isJsonPrimitive }?.asString
}
