package com.udea.rutaudea.data.sync

import com.udea.rutaudea.data.local.entity.QuestionEntity
import com.udea.rutaudea.data.mapper.QuestionMapper
import com.udea.rutaudea.domain.model.ImagenPregunta
import com.udea.rutaudea.domain.model.Question

/**
 * Convierte un documento de `questions_v2` (o del banco viejo `questions`)
 * en una entidad Room.
 *
 * Es una función pura sobre `Map` para poder testearla sin Firestore. El
 * contrato v2 guarda `opciones` como objeto `{A,B,C,D}`; se acepta también
 * la lista del banco viejo.
 */
object PreguntaFirestoreMapper {

    fun desdeDocumento(id: String, data: Map<String, Any?>?): QuestionEntity? {
        if (data == null || id.isBlank()) return null
        val area = data["area"] as? String ?: return null
        if (area !in AREAS) return null
        val pregunta = (data["pregunta"] as? String)?.takeIf { it.isNotBlank() } ?: return null

        val opciones = parseOpciones(data["opciones"])
        val estado = (data["estado"] as? String)?.lowercase() ?: Question.ESTADO_PENDIENTE
        if (estado !in Question.ESTADOS_VALIDOS) return null
        val respuesta = (data["respuesta_correcta"] as? String)?.uppercase().orEmpty()

        val imagenes = parseImagenes(data["imagenes"])
        val imagenUrl = (data["imagen_url"] as? String)?.takeIf { it.isNotBlank() }
        val requiereImagen = (data["requiere_imagen"] as? Boolean)
            ?: imagenes.any { it.esVisible() }

        return QuestionEntity(
            id = id,
            area = area,
            componente = data["componente"] as? String ?: when (area) {
                "razonamiento_logico" -> "Razonamiento Lógico"
                "competencia_lectora" -> "Competencia Lectora"
                else -> null
            },
            subtema = (data["subtema"] as? String).orEmpty(),
            competencia = data["competencia"] as? String,
            tipoTexto = data["tipo_texto"] as? String,
            dificultad = (data["dificultad"] as? String)?.lowercase().orEmpty(),
            textoBase = data["texto_base"] as? String,
            contexto = data["contexto"] as? String,
            contextoId = (data["contexto_ids"] as? List<*>)?.firstOrNull() as? String,
            pregunta = pregunta,
            opcionA = opciones[0],
            opcionB = opciones[1],
            opcionC = opciones[2],
            opcionD = opciones[3],
            respuestaCorrecta = respuesta,
            explicacion = data["explicacion"] as? String,
            fuente = data["fuente"] as? String,
            tipoFuente = data["tipo_fuente"] as? String,
            esOriginal = data["es_original"] as? Boolean ?: false,
            verificada = data["verificada"] as? Boolean ?: false,
            estado = estado,
            imagenUrl = imagenUrl,
            imagenesJson = QuestionMapper.imagenesAJson(imagenes),
            opcionesEnImagen = data["opciones_en_imagen"] as? Boolean ?: false,
            requiereImagen = requiereImagen,
            bancoVersion = (data["bank_version"] as? String)
                ?: (data["banco_version"] as? String)
                ?: "",
            tarea = data["tarea"] as? String,
            numero = (data["numero"] as? Number)?.toInt(),
            confianzaModelo = data["confianza_modelo"] as? String,
            revisadoPor = data["revisado_por"] as? String,
            fechaRevision = data["fecha_revision"] as? String,
            notas = data["notas"] as? String
        )
    }

    private fun parseOpciones(raw: Any?): List<String> {
        if (raw is Map<*, *>) {
            return LETRAS.map { (raw[it] as? String).orEmpty() }
        }
        if (raw is List<*>) {
            return (0 until 4).map { (raw.getOrNull(it) as? String).orEmpty() }
        }
        return listOf("", "", "", "")
    }

    private fun parseImagenes(raw: Any?): List<ImagenPregunta> {
        if (raw !is List<*>) return emptyList()
        return raw.mapNotNull { item ->
            val map = item as? Map<*, *> ?: return@mapNotNull null
            val url = (map["url"] as? String)?.takeIf { it.isNotBlank() }
            ImagenPregunta(
                url = url,
                rol = (map["rol"] as? String)
                    ?.takeIf { it == ImagenPregunta.ROL_CONTEXTO }
                    ?: ImagenPregunta.ROL_PREGUNTA,
                orden = (map["orden"] as? Number)?.toInt() ?: 1,
                alt = map["alt"] as? String,
                pathOrigen = map["path_origen"] as? String
            )
        }
    }

    private val AREAS = setOf("razonamiento_logico", "competencia_lectora")
    private val LETRAS = listOf("A", "B", "C", "D")
}
