package com.udea.rutaudea.domain.model

/**
 * Respuesta del cuestionario de satisfacción de la prueba piloto.
 *
 * Una sola respuesta por usuario (upsert por `uid`): si el usuario vuelve a
 * enviar, se actualiza su documento. Las preguntas abiertas son opcionales;
 * todo lo demás es obligatorio.
 */
data class EncuestaSatisfaccion(
    // ----- A. Datos del participante -----
    val tipoInstitucion: String = "",
    val nombreInstitucion: String = "",
    /** Grado escolar (solo si [tipoInstitucion] es colegio). */
    val grado: String = "",
    /** Semestre universitario (solo si [tipoInstitucion] es universidad). */
    val semestre: String = "",
    /** ¿Ya presentó el examen de admisión real? */
    val presentoExamen: Boolean? = null,

    // ----- B. Satisfacción por módulo (Likert 1–5) -----
    val likertSimulacro: Int = 0,
    val likertPractica: Int = 0,
    val likertProgreso: Int = 0,
    val likertInfo: Int = 0,

    // ----- C. Calidad de las preguntas (Likert 1–5) -----
    val claridadRL: Int = 0,
    val claridadCL: Int = 0,
    val dificultadAdecuada: Int = 0,

    // ----- D. Global -----
    val facilidadUso: Int = 0,
    /** Probabilidad de recomendar (0–10). */
    val nps: Int = -1,

    // ----- E. Abiertas (opcionales) -----
    val gustoMas: String = "",
    val mejorar: String = "",

    val fechaMs: Long = 0L
) {
    /** `true` si la encuesta está completa y lista para enviarse. */
    fun esValida(): Boolean = validar().isEmpty()

    /**
     * Errores de validación en español, uno por campo incompleto. Vacío =
     * la encuesta está completa.
     */
    fun validar(): List<String> = buildList {
        if (tipoInstitucion !in TIPOS_INSTITUCION) add("Elige tu tipo de institución")
        if (nombreInstitucion.isBlank()) add("Escribe el nombre de tu colegio o institución")
        if (tipoInstitucion == TIPO_COLEGIO && grado !in GRADOS) add("Elige tu grado")
        if (tipoInstitucion == TIPO_UNIVERSIDAD && semestre !in SEMESTRES) add("Elige tu semestre")
        if (presentoExamen == null) add("Indica si ya presentaste el examen real")
        if (likertSimulacro !in 1..5) add("Califica el módulo Simulacro")
        if (likertPractica !in 1..5) add("Califica el módulo Práctica")
        if (likertProgreso !in 1..5) add("Califica el módulo Progreso")
        if (likertInfo !in 1..5) add("Califica el módulo Info")
        if (claridadRL !in 1..5) add("Califica la claridad de Razonamiento Lógico")
        if (claridadCL !in 1..5) add("Califica la claridad de Competencia Lectora")
        if (dificultadAdecuada !in 1..5) add("Califica el nivel de dificultad")
        if (facilidadUso !in 1..5) add("Califica la facilidad de uso")
        if (nps !in 0..10) add("Indica qué tanto recomendarías la app (0–10)")
    }

    companion object {
        const val TIPO_COLEGIO = "Colegio"
        const val TIPO_UNIVERSIDAD = "Universidad"
        const val TIPO_OTRA = "Otra"
        val TIPOS_INSTITUCION = listOf(TIPO_COLEGIO, TIPO_UNIVERSIDAD, TIPO_OTRA)

        val GRADOS = listOf("9°", "10°", "11°", "Otro")

        val SEMESTRES = (1..10).map { "$it°" } + "Otro"

        /** Etiquetas de la escala Likert 1–5. */
        val LIKERT_EMOJIS = listOf("😞", "🙁", "😐", "🙂", "🤩")
        val LIKERT_ETIQUETAS = listOf(
            "Muy en desacuerdo",
            "En desacuerdo",
            "Neutral",
            "De acuerdo",
            "Muy de acuerdo"
        )
    }
}
