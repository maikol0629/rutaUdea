package com.udea.rutaudea.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

/**
 * Detalle por pregunta de un simulacro (cache offline de
 * `simulationQuestions`). Clave compuesta por sesión + pregunta.
 */
@Entity(
    tableName = "simulation_questions_cache",
    primaryKeys = ["session_id", "question_id"],
    indices = [Index(value = ["session_id"]), Index(value = ["subtema"])]
)
data class SimulationQuestionEntity(
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "question_id") val questionId: String,
    @ColumnInfo(name = "indice") val indice: Int,
    @ColumnInfo(name = "area") val area: String,
    @ColumnInfo(name = "subtema") val subtema: String,
    @ColumnInfo(name = "dificultad") val dificultad: String,
    @ColumnInfo(name = "respuesta_usuario") val respuestaUsuario: String,
    @ColumnInfo(name = "respuesta_correcta") val respuestaCorrecta: String,
    @ColumnInfo(name = "es_correcta") val esCorrecta: Boolean
)
