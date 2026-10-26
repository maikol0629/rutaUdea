package com.udea.rutaudea.ui.screen.perfil

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/** Resultado de intentar obtener el ID token de Google. */
sealed interface ResultadoGoogle {
    /** Se obtuvo un ID token listo para enviar a Firebase Auth. */
    data class Exito(val idToken: String) : ResultadoGoogle

    /** El usuario cerró el selector de cuentas: no es un error. */
    data object Cancelado : ResultadoGoogle

    /** Falló el flujo; [mensaje] es apto para mostrar al usuario. */
    data class Error(val mensaje: String) : ResultadoGoogle
}

/**
 * Abre el selector de cuentas de Google con Credential Manager y devuelve
 * el ID token. Requiere el Web Client ID (`serverClientId`) del proyecto
 * de Firebase y un dispositivo/emulador con Google Play Services.
 */
suspend fun obtenerIdTokenGoogle(
    context: Context,
    serverClientId: String
): ResultadoGoogle = try {
    val credentialManager = CredentialManager.create(context)
    val googleIdOption = GetGoogleIdOption.Builder()
        // false para permitir elegir cualquier cuenta de Google del
        // dispositivo, no solo las ya autorizadas para la app.
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(serverClientId)
        .setAutoSelectEnabled(false)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    val response = credentialManager.getCredential(context, request)
    val credential = response.credential

    if (credential is CustomCredential &&
        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    ) {
        val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
        ResultadoGoogle.Exito(googleCredential.idToken)
    } else {
        ResultadoGoogle.Error("No se pudo obtener la cuenta de Google")
    }
} catch (e: GetCredentialCancellationException) {
    ResultadoGoogle.Cancelado
} catch (e: NoCredentialException) {
    ResultadoGoogle.Error(
        "No hay cuentas de Google disponibles. Agrega una cuenta en el " +
            "dispositivo e intenta de nuevo"
    )
} catch (e: GetCredentialException) {
    ResultadoGoogle.Error("No se pudo iniciar sesión con Google. Intenta de nuevo")
} catch (e: Exception) {
    ResultadoGoogle.Error("No se pudo iniciar sesión con Google. Intenta de nuevo")
}
