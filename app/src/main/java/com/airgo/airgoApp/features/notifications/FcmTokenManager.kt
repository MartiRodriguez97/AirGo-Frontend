package com.airgo.airgoApp.features.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

object FcmTokenManager {
    // Función suspendida para obtener el token
    suspend fun getToken(): String? =
        try {
            Log.e("AIRGO_FCM_DEBUG", "--- SOLICITANDO TOKEN A FIREBASE ---")
            val token = FirebaseMessaging.getInstance().token.await()
            Log.e("AIRGO_FCM_DEBUG", "✅ TOKEN OBTENIDO: $token")
            token
        } catch (e: Exception) {
            Log.e("AIRGO_FCM_DEBUG", "❌ ERROR FATAL OBTENIENDO TOKEN: ${e.message}", e)
            null
        }
}
