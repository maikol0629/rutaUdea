package com.udea.rutaudea.data.mapper

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.udea.rutaudea.domain.model.User

/**
 * Conversión entre el documento `users/{uid}` de Firestore y el modelo de
 * dominio [User]. Mantiene los nombres de campo en un único lugar.
 */
object UserMapper {

    /** Mapea un snapshot existente. */
    fun fromDocument(snapshot: DocumentSnapshot): User =
        fromMap(snapshot.id, snapshot.data.orEmpty())

    /** Mapea un mapa de Firestore con el `uid` del documento. */
    fun fromMap(uid: String, data: Map<String, Any?>): User = User(
        uid = uid,
        nombre = data.texto(CAMPO_NOMBRE),
        apellido = data.texto(CAMPO_APELLIDO),
        correo = data.texto(CAMPO_CORREO),
        rol = data.texto(CAMPO_ROL).ifBlank { User.ROL_ASPIRANTE },
        fechaCreacion = data.millis(CAMPO_FECHA_CREACION),
        estado = data.texto(CAMPO_ESTADO).ifBlank { User.ESTADO_ACTIVO },
        telefono = data.texto(CAMPO_TELEFONO),
        ciudad = data.texto(CAMPO_CIUDAD),
        institucion = data.texto(CAMPO_INSTITUCION),
        tipoInstitucion = data.texto(CAMPO_TIPO_INSTITUCION),
        gradoSemestre = data.texto(CAMPO_GRADO_SEMESTRE),
        fechaExamen = data.millis(CAMPO_FECHA_EXAMEN),
        fotoUrl = data.texto(CAMPO_FOTO_URL),
        proveedor = data.texto(CAMPO_PROVEEDOR).ifBlank { User.PROVEEDOR_EMAIL },
        // Los usuarios antiguos no tienen el campo: se asumen completos.
        onboardingCompletado = data[CAMPO_ONBOARDING] as? Boolean ?: true
    )

    /** Documento completo para crear un perfil nuevo. */
    fun toMap(user: User): Map<String, Any?> = mapOf(
        CAMPO_NOMBRE to user.nombre,
        CAMPO_APELLIDO to user.apellido,
        CAMPO_CORREO to user.correo,
        CAMPO_ROL to user.rol,
        CAMPO_FECHA_CREACION to (user.fechaCreacion ?: System.currentTimeMillis()),
        CAMPO_ESTADO to user.estado,
        CAMPO_TELEFONO to user.telefono,
        CAMPO_CIUDAD to user.ciudad,
        CAMPO_INSTITUCION to user.institucion,
        CAMPO_TIPO_INSTITUCION to user.tipoInstitucion,
        CAMPO_GRADO_SEMESTRE to user.gradoSemestre,
        CAMPO_FECHA_EXAMEN to user.fechaExamen,
        CAMPO_FOTO_URL to user.fotoUrl,
        CAMPO_PROVEEDOR to user.proveedor,
        CAMPO_ONBOARDING to user.onboardingCompletado
    )

    /**
     * Campos que el usuario puede actualizar por su cuenta. Excluye
     * `correo`, `rol`, `estado` y `fechaCreacion` para que las reglas de
     * Firestore puedan impedir su modificación.
     */
    fun toProfileMap(user: User): Map<String, Any?> = mapOf(
        CAMPO_NOMBRE to user.nombre,
        CAMPO_APELLIDO to user.apellido,
        CAMPO_TELEFONO to user.telefono,
        CAMPO_CIUDAD to user.ciudad,
        CAMPO_INSTITUCION to user.institucion,
        CAMPO_TIPO_INSTITUCION to user.tipoInstitucion,
        CAMPO_GRADO_SEMESTRE to user.gradoSemestre,
        CAMPO_FECHA_EXAMEN to user.fechaExamen,
        CAMPO_FOTO_URL to user.fotoUrl,
        CAMPO_ONBOARDING to user.onboardingCompletado
    )

    private fun Map<String, Any?>.texto(campo: String): String = this[campo] as? String ?: ""

    private fun Map<String, Any?>.millis(campo: String): Long? = when (val valor = this[campo]) {
        is Number -> valor.toLong()
        is Timestamp -> valor.toDate().time
        else -> null
    }

    const val CAMPO_NOMBRE = "nombre"
    const val CAMPO_APELLIDO = "apellido"
    const val CAMPO_CORREO = "correo"
    const val CAMPO_ROL = "rol"
    const val CAMPO_FECHA_CREACION = "fechaCreacion"
    const val CAMPO_ESTADO = "estado"
    const val CAMPO_TELEFONO = "telefono"
    const val CAMPO_CIUDAD = "ciudad"
    const val CAMPO_INSTITUCION = "institucion"
    const val CAMPO_TIPO_INSTITUCION = "tipoInstitucion"
    const val CAMPO_GRADO_SEMESTRE = "gradoSemestre"
    const val CAMPO_FECHA_EXAMEN = "fechaExamen"
    const val CAMPO_FOTO_URL = "fotoUrl"
    const val CAMPO_PROVEEDOR = "proveedor"
    const val CAMPO_ONBOARDING = "onboardingCompletado"
}
