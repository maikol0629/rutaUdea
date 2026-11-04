package com.udea.rutaudea.domain.model

/**
 * Tipo de sesión de simulacro.
 *
 * - [COMPLETO]: examen real (40 RL + 40 CL, 80 preguntas).
 * - [PRUEBA]: versión corta "Pruébate" (10 RL + 10 CL, 20 preguntas) con
 *   las mismas reglas que el simulacro completo.
 *
 * La [clave] es el valor estable que se persiste en Room/Firestore; la
 * duración depende de la configuración publicada en el panel.
 */
enum class TipoSimulacro(
    val clave: String,
    val preguntasPorArea: Int,
    val totalPreguntas: Int
) {
    COMPLETO("completo", 40, 80),
    PRUEBA("prueba", 10, 20);

    /** Duración de la sesión según la configuración vigente. */
    fun duracionMs(config: SimulacroConfig): Long = when (this) {
        COMPLETO -> config.duracionMs
        PRUEBA -> config.duracionPruebaMs
    }

    companion object {
        /** Convierte la clave persistida al tipo; degrada a [COMPLETO]. */
        fun fromClave(clave: String?): TipoSimulacro =
            entries.firstOrNull { it.clave == clave } ?: COMPLETO
    }
}
