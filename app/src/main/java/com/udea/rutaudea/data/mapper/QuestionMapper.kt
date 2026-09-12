package com.udea.rutaudea.data.mapper

import com.udea.rutaudea.data.local.entity.QuestionEntity
import com.udea.rutaudea.domain.model.Question

/**
 * Conversiones entre la capa de datos (Room) y el modelo de dominio.
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
        pregunta = entity.pregunta,
        opciones = listOf(
            entity.opcionA,
            entity.opcionB,
            entity.opcionC,
            entity.opcionD
        ),
        respuestaCorrecta = entity.respuestaCorrecta,
        explicacion = entity.explicacion,
        esOriginal = entity.esOriginal
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
        pregunta = question.pregunta,
        opcionA = question.opciones.getOrElse(0) { "" },
        opcionB = question.opciones.getOrElse(1) { "" },
        opcionC = question.opciones.getOrElse(2) { "" },
        opcionD = question.opciones.getOrElse(3) { "" },
        respuestaCorrecta = question.respuestaCorrecta,
        explicacion = question.explicacion,
        esOriginal = question.esOriginal
    )

    fun listToDomain(entities: List<QuestionEntity>): List<Question> =
        entities.map { entityToDomain(it) }
}