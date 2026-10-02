package com.udea.rutaudea.domain.model

/**
 * Contenido educativo de un componente del examen (módulo Info,
 * plan §10 y §2: "Consultar información educativa sobre componentes
 * y subtemas").
 *
 * Los componentes coinciden con los subtemas oficiales del banco
 * (RL01–RL18 y CL01–CL14).
 */
data class TemaEducativo(
    /** Código del componente ("RL01", "CL02"...). */
    val id: String,
    /** Nombre completo con código ("RL01 Proporcionalidad"). */
    val nombre: String,
    /** Área: "razonamiento_logico" o "competencia_lectora". */
    val area: String,
    val descripcion: String,
    val estrategia: String,
    val erroresComunes: String
) {
    companion object {
        const val AREA_RL = "razonamiento_logico"
        const val AREA_CL = "competencia_lectora"
    }
}
