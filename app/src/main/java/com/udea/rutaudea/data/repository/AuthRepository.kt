package com.udea.rutaudea.data.repository

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.udea.rutaudea.data.mapper.UserMapper
import com.udea.rutaudea.domain.model.DatosPerfil
import com.udea.rutaudea.domain.model.User
import kotlinx.coroutines.tasks.await

/**
 * Punto único de acceso a la autenticación (Firebase Auth, email/contraseña
 * y Google) y al perfil del usuario (colección `users` de Firestore).
 *
 * El perfil se crea de forma idempotente con `SetOptions.merge()` para no
 * sobrescribir datos si la escritura se reintenta o si el documento ya
 * existe (por ejemplo, al iniciar sesión con Google por segunda vez).
 */
class AuthRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    /** Usuario actualmente autenticado (desde el SDK de Firebase). */
    fun firebaseCurrentUser(): FirebaseUser? = auth.currentUser

    suspend fun isLoggedIn(): Boolean = auth.currentUser != null

    // ----- REGISTRO MANUAL -----

    /**
     * Registra un usuario con email/contraseña y crea su documento en
     * `users/{uid}` con rol aspirante y onboarding pendiente.
     */
    suspend fun register(
        nombre: String,
        apellido: String,
        correo: String,
        password: String
    ): Result<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(correo.trim(), password).await()
            val firebaseUser = result.user
                ?: return Result.failure(Exception("No se pudo crear la cuenta"))

            val nombreLimpio = nombre.trim()
            val apellidoLimpio = apellido.trim()
            actualizarNombreEnAuth(firebaseUser, nombreLimpio, apellidoLimpio)

            val user = User(
                uid = firebaseUser.uid,
                nombre = nombreLimpio,
                apellido = apellidoLimpio,
                correo = firebaseUser.email ?: correo.trim(),
                proveedor = User.PROVEEDOR_EMAIL,
                onboardingCompletado = false
            )
            crearPerfilSiNoExiste(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(mapearError(e, esRegistro = true))
        }
    }

    // ----- LOGIN MANUAL -----

    /** Inicia sesión con email/contraseña y carga el perfil desde Firestore. */
    suspend fun login(correo: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(correo.trim(), password).await()
            val firebaseUser = result.user
                ?: return Result.failure(Exception("No se pudo iniciar sesión"))
            val user = loadOrCreateProfile(firebaseUser)
            if (!user.estaActivo()) {
                auth.signOut()
                Result.failure(Exception("Tu cuenta está deshabilitada. Contacta al administrador"))
            } else {
                Result.success(user)
            }
        } catch (e: Exception) {
            Result.failure(mapearError(e, esRegistro = false))
        }
    }

    // ----- GOOGLE -----

    /**
     * Inicia sesión con una credencial de Google (ID token de Credential
     * Manager). Firebase crea la cuenta automáticamente la primera vez.
     *
     * Si el correo ya pertenece a una cuenta de email/contraseña, Firebase
     * lanza una colisión y se muestra un mensaje claro: no se vinculan
     * cuentas automáticamente para evitar secuestros de cuenta.
     */
    suspend fun signInWithGoogle(idToken: String): Result<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val firebaseUser = result.user
                ?: return Result.failure(Exception("No se pudo iniciar sesión con Google"))

            val existente = obtenerPerfil(firebaseUser.uid)
            if (existente != null) {
                if (!existente.estaActivo()) {
                    auth.signOut()
                    Result.failure(
                        Exception("Tu cuenta está deshabilitada. Contacta al administrador")
                    )
                } else {
                    Result.success(existente)
                }
            } else {
                val partes = separarNombre(firebaseUser.displayName)
                val user = User(
                    uid = firebaseUser.uid,
                    nombre = partes.first,
                    apellido = partes.second,
                    correo = firebaseUser.email.orEmpty(),
                    fotoUrl = firebaseUser.photoUrl?.toString().orEmpty(),
                    proveedor = User.PROVEEDOR_GOOGLE,
                    onboardingCompletado = false
                )
                crearPerfilSiNoExiste(user)
                Result.success(user)
            }
        } catch (e: Exception) {
            Result.failure(mapearError(e, esRegistro = true))
        }
    }

    // ----- SESIÓN -----

    /** Restaura la sesión persistida (si la hay) cargando el perfil. */
    suspend fun restoreSession(): User? {
        val firebaseUser = auth.currentUser ?: return null
        return try {
            val user = loadOrCreateProfile(firebaseUser)
            if (!user.estaActivo()) {
                auth.signOut()
                null
            } else {
                user
            }
        } catch (e: Exception) {
            // Sin red: se conserva la sesión y se asume que el onboarding
            // ya estaba completo para no bloquear la app.
            User(
                uid = firebaseUser.uid,
                nombre = firebaseUser.displayName.orEmpty(),
                correo = firebaseUser.email.orEmpty(),
                onboardingCompletado = true
            )
        }
    }

    fun logout() = auth.signOut()

    // ----- PERFIL / ONBOARDING -----

    /**
     * Guarda los datos del onboarding (o de la edición de perfil) y marca
     * el onboarding como completado.
     */
    suspend fun completeProfile(datos: DatosPerfil): Result<User> {
        return try {
            val firebaseUser = auth.currentUser
                ?: return Result.failure(Exception("No hay una sesión activa"))
            val errores = datos.validar()
            if (errores.isNotEmpty()) {
                return Result.failure(Exception(errores.first()))
            }

            val existente = obtenerPerfil(firebaseUser.uid)
            val actual = existente ?: User(
                uid = firebaseUser.uid,
                correo = firebaseUser.email.orEmpty(),
                fotoUrl = firebaseUser.photoUrl?.toString().orEmpty(),
                proveedor = proveedorDe(firebaseUser),
                onboardingCompletado = false
            )

            val actualizado = datos.aUsuario(actual).copy(
                correo = actual.correo.ifBlank { firebaseUser.email.orEmpty() },
                rol = actual.rol,
                fechaCreacion = actual.fechaCreacion ?: System.currentTimeMillis(),
                estado = actual.estado,
                proveedor = actual.proveedor,
                onboardingCompletado = true
            )

            // Si el documento no existe, se escribe completo para cumplir la
            // regla de creación (rol aspirante y correo del token); si ya
            // existe, solo se envían los campos editables del perfil.
            val datosDocumento = if (existente == null) {
                UserMapper.toMap(actualizado)
            } else {
                UserMapper.toProfileMap(actualizado)
            }
            firestore.collection(COLECCION_USERS)
                .document(firebaseUser.uid)
                .set(datosDocumento, SetOptions.merge())
                .await()

            actualizarNombreEnAuth(firebaseUser, actualizado.nombre, actualizado.apellido)
            Result.success(actualizado)
        } catch (e: Exception) {
            Result.failure(mapearError(e, esRegistro = false))
        }
    }

    /** Envía el correo de recuperación de contraseña. */
    suspend fun sendPasswordReset(correo: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(correo.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapearError(e, esRegistro = false))
        }
    }

    // ----- INTERNOS -----

    private suspend fun loadOrCreateProfile(firebaseUser: FirebaseUser): User {
        val existente = obtenerPerfil(firebaseUser.uid)
        if (existente != null) return existente

        val partes = separarNombre(firebaseUser.displayName)
        val user = User(
            uid = firebaseUser.uid,
            nombre = partes.first,
            apellido = partes.second,
            correo = firebaseUser.email.orEmpty(),
            fotoUrl = firebaseUser.photoUrl?.toString().orEmpty(),
            proveedor = proveedorDe(firebaseUser),
            // Documento ausente = cuenta nueva sin onboarding.
            onboardingCompletado = false
        )
        crearPerfilSiNoExiste(user)
        return user
    }

    private suspend fun obtenerPerfil(uid: String): User? {
        val doc: DocumentSnapshot = firestore.collection(COLECCION_USERS)
            .document(uid)
            .get()
            .await()
        return if (doc.exists()) UserMapper.fromDocument(doc) else null
    }

    /**
     * Crea el documento con merge para que un reintento no borre campos
     * agregados entre la lectura y la escritura.
     */
    private suspend fun crearPerfilSiNoExiste(user: User) {
        firestore.collection(COLECCION_USERS)
            .document(user.uid)
            .set(UserMapper.toMap(user), SetOptions.merge())
            .await()
    }

    private suspend fun actualizarNombreEnAuth(
        firebaseUser: FirebaseUser,
        nombre: String,
        apellido: String
    ) {
        val nombreCompleto = listOf(nombre.trim(), apellido.trim())
            .filter { it.isNotEmpty() }
            .joinToString(" ")
        if (nombreCompleto.isBlank() || firebaseUser.displayName == nombreCompleto) return
        runCatching {
            firebaseUser.updateProfile(
                UserProfileChangeRequest.Builder()
                    .setDisplayName(nombreCompleto)
                    .build()
            ).await()
        }
    }

    private fun proveedorDe(firebaseUser: FirebaseUser): String =
        if (firebaseUser.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }) {
            User.PROVEEDOR_GOOGLE
        } else {
            User.PROVEEDOR_EMAIL
        }

    /** El primer token es el nombre; el resto, el apellido. */
    private fun separarNombre(displayName: String?): Pair<String, String> {
        val partes = displayName.orEmpty().trim().split(Regex("\\s+"))
            .filter { it.isNotBlank() }
        return when {
            partes.isEmpty() -> "" to ""
            partes.size == 1 -> partes.first() to ""
            else -> partes.first() to partes.drop(1).joinToString(" ")
        }
    }

    // ----- ERRORES -----

    private fun mapearError(e: Exception, esRegistro: Boolean): Throwable {
        return when (e) {
            is FirebaseAuthUserCollisionException ->
                Exception(
                    "Ya existe una cuenta con este correo. Inicia sesión con el " +
                        "método que usaste al registrarte"
                )
            is FirebaseAuthWeakPasswordException ->
                Exception("La contraseña debe tener al menos 6 caracteres")
            is FirebaseAuthInvalidCredentialsException ->
                if (esRegistro) Exception("El correo no es válido")
                else Exception("Correo o contraseña incorrectos")
            is FirebaseAuthInvalidUserException ->
                Exception("Esta cuenta no existe o fue deshabilitada")
            is FirebaseAuthRecentLoginRequiredException ->
                Exception("Por seguridad, vuelve a iniciar sesión e intenta de nuevo")
            is FirebaseNetworkException ->
                Exception("Sin conexión. Verifica tu internet e intenta de nuevo")
            is FirebaseTooManyRequestsException ->
                Exception("Demasiados intentos. Espera un momento e intenta de nuevo")
            else -> when {
                e.message?.contains("network", ignoreCase = true) == true ->
                    Exception("Sin conexión. Verifica tu internet e intenta de nuevo")
                e.message?.contains("EMAIL_NOT_FOUND", ignoreCase = true) == true ->
                    Exception("Correo o contraseña incorrectos")
                else -> Exception("Error de autenticación. Intenta de nuevo")
            }
        }
    }

    companion object {
        const val COLECCION_USERS = "users"
    }
}
