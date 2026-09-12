package com.udea.rutaudea.domain.model

/**
 * Modelo de dominio de una pregunta, independiente de la capa de datos.
 */
data class Question(
    val id: String,
    val area: String,
    val componente: String? = null,
    val subtema: String,
    val competencia: String? = null,
    val tipoTexto: String? = null,
    val dificultad: String,
    val textoBase: String? = null,
    val contexto: String? = null,
    val pregunta: String,
    val opciones: List<String>, // [A, B, C, D]
    val respuestaCorrecta: String, // "A", "B", "C" o "D"
    val explicacion: String? = null,
    val esOriginal: Boolean = false
) {
    /** Devuelve la opción correcta por índice. */
    fun respuestaCorrectaTexto(): String? =
        when (respuestaCorrecta) {
            "A" -> opciones.getOrNull(0)
            "B" -> opciones.getOrNull(1)
            "C" -> opciones.getOrNull(2)
            "D" -> opciones.getOrNull(3)
            else -> null
        }

    /** Devuelve el texto de una opción según su letra. */
    fun opcionPorLetra(letra: String): String? =
        when (letra) {
            "A" -> opciones.getOrNull(0)
            "B" -> opciones.getOrNull(1)
            "C" -> opciones.getOrNull(2)
            "D" -> opciones.getOrNull(3)
            else -> null
        }
}