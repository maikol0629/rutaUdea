package com.udea.rutaudea.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.udea.rutaudea.domain.model.User
import kotlinx.coroutines.tasks.await

/**
 * Punto único de acceso a la autenticación (Firebase Auth, email/contraseña)
 * y al perfil del usuario (colección `users` de Firestore).
 */
class AuthRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    /** Usuario actualmente autenticado (desde el SDK de Firebase). */
    fun firebaseCurrentUser(): FirebaseUser? = auth.currentUser

    suspend fun isLoggedIn(): Boolean = auth.currentUser != null

    // ----- REGISTRO -----

    /**
     * Registra un usuario con email/contraseña y crea su documento
     * en la colección `users` con rol aspirante.
     */
    suspend fun register(nombre: String, correo: String, password: String): Result<User> = try {
        val result = auth.createUserWithEmailAndPassword(correo.trim(), password).await()
        val firebaseUser = result.user
            ?: return Result.failure(Exception("No se pudo crear la cuenta"))
        firebaseUser.updateProfile(
            com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(nombre.trim())
                .build()
        ).await()
        val user = User(
            uid = firebaseUser.uid,
            nombre = nombre.trim(),
            correo = correo.trim()
        )
        createUserDocument(user)
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(mapearError(e, esRegistro = true))
    }

    // ----- LOGIN -----

    /** Inicia sesión con email/contraseña y carga el perfil desde Firestore. */
    suspend fun login(correo: String, password: String): Result<User> = try {
        val result = auth.signInWithEmailAndPassword(correo.trim(), password).await()
        val firebaseUser = result.user
            ?: return Result.failure(Exception("No se pudo iniciar sesión"))
        Result.success(loadOrCreateProfile(firebaseUser))
    } catch (e: Exception) {
        Result.failure(mapearError(e, esRegistro = false))
    }

    /** Restaura la sesión persistida (si la hay) cargando el perfil. */
    suspend fun restoreSession(): User? {
        val firebaseUser = auth.currentUser ?: return null
        return try {
            loadOrCreateProfile(firebaseUser)
        } catch (e: Exception) {
            User(
                uid = firebaseUser.uid,
                nombre = firebaseUser.displayName ?: "",
                correo = firebaseUser.email ?: ""
            )
        }
    }

    fun logout() = auth.signOut()

    // ----- PERFIL (colección users) -----

    private suspend fun loadOrCreateProfile(firebaseUser: FirebaseUser): User {
        val doc = firestore.collection(COLECCION_USERS)
            .document(firebaseUser.uid)
            .get()
            .await()
        return if (doc.exists()) {
            User(
                uid = firebaseUser.uid,
                nombre = doc.getString(CAMPO_NOMBRE) ?: (firebaseUser.displayName ?: ""),
                correo = doc.getString(CAMPO_CORREO) ?: (firebaseUser.email ?: ""),
                rol = doc.getString(CAMPO_ROL) ?: User.ROL_ASPIRANTE,
                fechaCreacion = doc.getLong(CAMPO_FECHA_CREACION),
                estado = doc.getString(CAMPO_ESTADO) ?: "activo"
            )
        } else {
            val user = User(
                uid = firebaseUser.uid,
                nombre = firebaseUser.displayName ?: "",
                correo = firebaseUser.email ?: ""
            )
            createUserDocument(user)
            user
        }
    }

    private suspend fun createUserDocument(user: User) {
        firestore.collection(COLECCION_USERS)
            .document(user.uid)
            .set(
                mapOf(
                    CAMPO_NOMBRE to user.nombre,
                    CAMPO_CORREO to user.correo,
                    CAMPO_ROL to user.rol,
                    CAMPO_FECHA_CREACION to System.currentTimeMillis(),
                    CAMPO_ESTADO to user.estado
                )
            )
            .await()
    }

    // ----- ERRORES -----

    private fun mapearError(e: Exception, esRegistro: Boolean): Throwable {
        return when (e) {
            is FirebaseAuthUserCollisionException ->
                Exception("Este correo ya está registrado")
            is FirebaseAuthWeakPasswordException ->
                Exception("La contraseña debe tener al menos 6 caracteres")
            is FirebaseAuthInvalidCredentialsException ->
                if (esRegistro) Exception("El correo no es válido")
                else Exception("Correo o contraseña incorrectos")
            is FirebaseAuthInvalidUserException ->
                Exception("Esta cuenta no existe o fue deshabilitada")
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
        const val CAMPO_NOMBRE = "nombre"
        const val CAMPO_CORREO = "correo"
        const val CAMPO_ROL = "rol"
        const val CAMPO_FECHA_CREACION = "fechaCreacion"
        const val CAMPO_ESTADO = "estado"
    }
}
