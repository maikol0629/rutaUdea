package com.udea.rutaudea.domain.model

/**
 * Modelo de dominio de un usuario, independiente de la capa de datos.
 * Refleja el documento `users/{uid}` de Firestore (plan, sección 7).
 */
data class User(
    val uid: String,
    val nombre: String = "",
    val correo: String = "",
    val rol: String = ROL_ASPIRANTE,
    val fechaCreacion: Long? = null,
    val estado: String = "activo"
) {
    fun esAdministrador(): Boolean = rol == ROL_ADMIN

    companion object {
        const val ROL_ASPIRANTE = "aspirante"
        const val ROL_ADMIN = "admin"
    }
}
