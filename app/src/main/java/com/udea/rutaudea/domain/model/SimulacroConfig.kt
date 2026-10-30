package com.udea.rutaudea.domain.model

/**
 * Configuración del simulacro publicada desde el panel en
 * `settings/config` del Firestore.
 *
 * Todos los campos tienen valores por defecto seguros para que la app
 * funcione aunque no haya configuración publicada o falle la lectura.
 */
data class SimulacroConfig(
    /** Duración del simulacro en minutos (panel: tiempo_simulacro_minutos). */
    val tiempoSimulacroMinutos: Int = 120,
    /** Tiempo máximo de inactividad antes de pausar (minutos). */
    val inactividadMaxMinutos: Int = 30,
    /** Política de exclusión de preguntas repetidas. */
    val politicaRepeticion: String = "ultimos_3",
    /** Distribución porcentual de dificultad {baja, media, alta}. */
    val distribucionDificultad: DistribucionDificultad = DistribucionDificultad(25, 50, 25)
) {
    /** Duración en milisegundos para el cronómetro. */
    val duracionMs: Long get() = tiempoSimulacroMinutos.toLong() * 60L * 1000L

    /** True si la distribución suma 100 (válida para cuotas porcentuales). */
    val distribucionValida: Boolean get() =
        distribucionDificultad.baja + distribucionDificultad.media + distribucionDificultad.alta == 100
}

/** Distribución porcentual de dificultad del simulacro. */
data class DistribucionDificultad(
    val baja: Int,
    val media: Int,
    val alta: Int
)
