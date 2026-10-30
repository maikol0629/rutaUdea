package com.udea.rutaudea.domain.model

/**
 * Modelo de dominio de una pregunta, independiente de la capa de datos.
 *
 * Refleja el contrato v2 (`migracion/01_esquema/contrato_datos.md`):
 * incluye estado, procedencia, contexto e imágenes con rol. Los campos
 * nuevos tienen valores por defecto para no romper a los consumidores que
 * solo leen lo esencial.
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
    val contextoId: String? = null,
    val pregunta: String,
    val opciones: List<String>, // [A, B, C, D]
    val respuestaCorrecta: String, // "A", "B", "C" o "D"
    val explicacion: String? = null,
    val esOriginal: Boolean = false,
    /** URL pública de la figura principal (compatibilidad con el banco viejo). */
    val imagenUrl: String? = null,
    /** Estado del contrato: `aprobado`, `aprobado_con_imagen`, `pendiente`, `deshabilitado`. */
    val estado: String = ESTADO_PENDIENTE,
    val fuente: String? = null,
    val tipoFuente: String? = null,
    val verificada: Boolean = false,
    /** Lista completa de imágenes (contexto y pregunta) del contrato v2. */
    val imagenes: List<ImagenPregunta> = emptyList(),
    val opcionesEnImagen: Boolean = false,
    val requiereImagen: Boolean = false,
    /** Versión del banco que originó la fila (D-07). */
    val bancoVersion: String? = null,
    val tarea: String? = null,
    val numero: Int? = null,
    val confianzaModelo: String? = null,
    val revisadoPor: String? = null,
    val fechaRevision: String? = null,
    val notas: String? = null
) {
    /**
     * Imágenes que la UI puede mostrar, en orden y sin duplicados.
     * Si el banco solo trae `imagen_url` (compatibilidad) se usa como
     * imagen de la pregunta.
     */
    fun imagenesVisibles(): List<ImagenPregunta> {
        val deLista = imagenes
            .filter { it.esVisible() }
            .sortedWith(compareBy({ if (it.rol == ImagenPregunta.ROL_CONTEXTO) 0 else 1 }, { it.orden }))
        if (deLista.isNotEmpty()) return deLista
        val url = imagenUrl?.takeIf { it.isNotBlank() } ?: return emptyList()
        return listOf(
            ImagenPregunta(url = url, rol = ImagenPregunta.ROL_PREGUNTA, orden = 1)
        )
    }

    fun imagenesDeContexto(): List<ImagenPregunta> =
        imagenesVisibles().filter { it.rol == ImagenPregunta.ROL_CONTEXTO }

    fun imagenesDePregunta(): List<ImagenPregunta> =
        imagenesVisibles().filter { it.rol != ImagenPregunta.ROL_CONTEXTO }

    /** True si la pregunta necesita mostrar alguna figura. */
    fun tieneImagen(): Boolean = imagenesVisibles().isNotEmpty()

    /** True si el estado permite mostrarla en práctica o simulacro. */
    fun esPublicable(): Boolean = estado == ESTADO_APROBADO || estado == ESTADO_APROBADO_CON_IMAGEN

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

    companion object {
        const val ESTADO_APROBADO = "aprobado"
        const val ESTADO_APROBADO_CON_IMAGEN = "aprobado_con_imagen"
        const val ESTADO_PENDIENTE = "pendiente"
        const val ESTADO_DESHABILITADO = "deshabilitado"
        val ESTADOS_PUBLICABLES = setOf(ESTADO_APROBADO, ESTADO_APROBADO_CON_IMAGEN)
        val ESTADOS_VALIDOS = setOf(
            ESTADO_APROBADO,
            ESTADO_APROBADO_CON_IMAGEN,
            ESTADO_PENDIENTE,
            ESTADO_DESHABILITADO
        )
    }
}
