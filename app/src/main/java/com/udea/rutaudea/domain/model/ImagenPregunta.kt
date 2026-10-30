package com.udea.rutaudea.domain.model

/**
 * Imagen asociada a una pregunta o a su contexto (contrato v2, D-05).
 *
 * Una pregunta puede necesitar varias imágenes: la figura del contexto
 * compartido y la figura propia de la pregunta. El campo [rol] indica cuál
 * es cuál y [orden] mantiene el orden de aparición dentro del mismo rol.
 */
data class ImagenPregunta(
    val url: String? = null,
    val rol: String = ROL_PREGUNTA,
    val orden: Int = 1,
    val alt: String? = null,
    val pathOrigen: String? = null
) {
    /** Una imagen sin URL publicada no se puede renderizar. */
    fun esVisible(): Boolean = !url.isNullOrBlank()

    companion object {
        const val ROL_CONTEXTO = "contexto"
        const val ROL_PREGUNTA = "pregunta"
    }
}
