package com.airgo.airgoApp.features.login.data.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.airgo.airgoApp.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

// Función para iniciar sesión con Google utilizando la API de Credenciales de Android
suspend fun signInWithGoogle(context: Context): GoogleIdTokenCredential? {
    val serverClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
    if (serverClientId.isBlank()) {
        Log.e("GoogleAuthSignIn", "GOOGLE_WEB_CLIENT_ID no configurado")
        return null
    }

    // Configura la solicitud para obtener el token de ID de Google
    val googleIdOption =
        GetGoogleIdOption
            .Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .build()

    // Crea la solicitud de credenciales
    val request =
        GetCredentialRequest
            .Builder()
            .addCredentialOption(googleIdOption)
            .build()

    // Intenta obtener la credencial de Google y convertirla en un token de ID
    return try {
        val credentialManager = CredentialManager.create(context)
        val result = credentialManager.getCredential(context, request)
        GoogleIdTokenCredential.createFrom(result.credential.data)
    } catch (e: GetCredentialException) {
        Log.e("GoogleAuthSignIn", "Error al obtener credencial de Google", e)
        null
    }
}
