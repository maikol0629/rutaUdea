package com.udea.rutaudea.domain.model

/**
 * Datos que el usuario diligencia en el onboarding y que luego puede
 * editar desde su perfil.
 *
 * La validación es pura (sin dependencias de Android) para poder probarla
 * en tests unitarios. Cuando [tipoInstitucion] es [TIPO_OTRA] la institución
 * es opcional y no se exige grado ni semestre; el grado/semestre se limpia
 * al guardar.
 */
data class DatosPerfil(
    val nombre: String = "",
    val apellido: String = "",
    val tipoInstitucion: String = "",
    val institucion: String = "",
    val gradoSemestre: String = "",
    val telefono: String = "",
    val ciudad: String = "",
    val fechaExamen: Long? = null
) {

    /** `true` si el tipo de institución exige grado o semestre. */
    val exigeGradoSemestre: Boolean
        get() = tipoInstitucion in listOf(TIPO_COLEGIO, TIPO_UNIVERSIDAD)

    /**
     * Errores de validación en español, uno por campo inválido. Una lista
     * vacía significa que el perfil está completo.
     */
    fun validar(): List<String> = buildList {
        if (nombre.trim().length < 2) add(ERROR_NOMBRE)
        if (apellido.trim().length < 2) add(ERROR_APELLIDO)
        if (tipoInstitucion !in TIPOS_INSTITUCION) add(ERROR_TIPO_INSTITUCION)
        // Con "Otra" el nombre de la institución es opcional.
        if (tipoInstitucion != TIPO_OTRA && institucion.trim().length < 2) {
            add(ERROR_INSTITUCION)
        }
        if (exigeGradoSemestre && gradoSemestre.isBlank()) add(ERROR_GRADO)
        if (telefono.isNotBlank() && !TELEFONO_REGEX.matches(telefono.trim())) {
            add(ERROR_TELEFONO)
        }
        if (ciudad.trim().length > 80) add(ERROR_CIUDAD)
        if (fechaExamen != null && fechaExamen <= 0) add(ERROR_FECHA_EXAMEN)
    }

    /** `true` si todos los campos obligatorios están completos. */
    fun esValido(): Boolean = validar().isEmpty()

    /** Datos normalizados para persistir en Firestore. */
    fun aUsuario(base: User): User = base.copy(
        nombre = nombre.trim(),
        apellido = apellido.trim(),
        tipoInstitucion = tipoInstitucion,
        institucion = institucion.trim(),
        gradoSemestre = if (exigeGradoSemestre) gradoSemestre.trim() else "",
        telefono = telefono.trim(),
        ciudad = ciudad.trim(),
        fechaExamen = fechaExamen,
        onboardingCompletado = true
    )

    companion object {
        const val TIPO_COLEGIO = EncuestaSatisfaccion.TIPO_COLEGIO
        const val TIPO_UNIVERSIDAD = EncuestaSatisfaccion.TIPO_UNIVERSIDAD
        const val TIPO_OTRA = EncuestaSatisfaccion.TIPO_OTRA

        val TIPOS_INSTITUCION = EncuestaSatisfaccion.TIPOS_INSTITUCION
        val GRADOS = EncuestaSatisfaccion.GRADOS
        val SEMESTRES = EncuestaSatisfaccion.SEMESTRES

        const val ERROR_NOMBRE = "Ingresa tu nombre"
        const val ERROR_APELLIDO = "Ingresa tu apellido"
        const val ERROR_TIPO_INSTITUCION = "Elige tu tipo de institución"
        const val ERROR_INSTITUCION = "Ingresa el nombre de tu institución"
        const val ERROR_GRADO = "Elige tu grado o semestre"
        const val ERROR_TELEFONO = "Ingresa un teléfono válido (7 a 15 dígitos)"
        const val ERROR_CIUDAD = "El nombre de la ciudad es demasiado largo"
        const val ERROR_FECHA_EXAMEN = "La fecha del examen no es válida"

        /** Teléfono colombiano/internacional: dígitos, espacios, guiones y +. */
        private val TELEFONO_REGEX = Regex("^\\+?[0-9\\s-]{7,20}$")

        /** Opciones de grado/semestre según el tipo de institución. */
        fun opcionesGrado(tipoInstitucion: String): List<String> = when (tipoInstitucion) {
            TIPO_COLEGIO -> GRADOS
            TIPO_UNIVERSIDAD -> SEMESTRES
            else -> emptyList()
        }

        /** Construye los datos de perfil a partir del usuario autenticado. */
        fun desdeUsuario(user: User): DatosPerfil = DatosPerfil(
            nombre = user.nombre,
            apellido = user.apellido,
            tipoInstitucion = user.tipoInstitucion,
            institucion = user.institucion,
            gradoSemestre = user.gradoSemestre,
            telefono = user.telefono,
            ciudad = user.ciudad,
            fechaExamen = user.fechaExamen
        )
    }
}

/**
 * Validación de correo electrónico independiente de `android.util.Patterns`
 * para que los ViewModels se puedan probar en la JVM.
 */
object ValidadorCorreo {
    private val REGEX = Regex("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")

    fun esValido(correo: String): Boolean = REGEX.matches(correo.trim())
}
