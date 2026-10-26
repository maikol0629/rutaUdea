package com.udea.rutaudea.domain.model

/**
 * Modelo de dominio de un usuario, independiente de la capa de datos.
 * Refleja el documento `users/{uid}` de Firestore (plan, sección 7).
 *
 * [onboardingCompletado] se mantiene en `true` por defecto para que los
 * usuarios creados antes de esta funcionalidad (sin el campo en Firestore)
 * puedan seguir usando la app sin pasar por el onboarding.
 */
data class User(
    val uid: String,
    val nombre: String = "",
    val apellido: String = "",
    val correo: String = "",
    val rol: String = ROL_ASPIRANTE,
    val fechaCreacion: Long? = null,
    val estado: String = ESTADO_ACTIVO,
    val telefono: String = "",
    val ciudad: String = "",
    val institucion: String = "",
    val tipoInstitucion: String = "",
    val gradoSemestre: String = "",
    val fechaExamen: Long? = null,
    val fotoUrl: String = "",
    val proveedor: String = PROVEEDOR_EMAIL,
    val onboardingCompletado: Boolean = true
) {
    /** `true` si el usuario tiene permisos de administrador. */
    fun esAdministrador(): Boolean = rol == ROL_ADMIN

    /** `true` si la cuenta está habilitada para usar la app. */
    fun estaActivo(): Boolean = estado == ESTADO_ACTIVO

    /** Nombre y apellido unidos, sin espacios sobrantes. */
    val nombreCompleto: String
        get() = listOf(nombre.trim(), apellido.trim())
            .filter { it.isNotEmpty() }
            .joinToString(" ")

    /** `true` si el usuario aún debe diligenciar el onboarding. */
    val requiereOnboarding: Boolean
        get() = !onboardingCompletado

    companion object {
        const val ROL_ASPIRANTE = "aspirante"
        const val ROL_ADMIN = "admin"

        const val ESTADO_ACTIVO = "activo"

        const val PROVEEDOR_EMAIL = "email"
        const val PROVEEDOR_GOOGLE = "google"
    }
}
