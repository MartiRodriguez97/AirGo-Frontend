package com.airgo.airgoApp.features.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

object FcmTokenManager {
    // Función suspendida para obtener el token
    suspend fun getToken(): String? =
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            token
        } catch (e: java.lang.Exception) {
            Log.e("AIRGO_FCM_DEBUG", "❌ ERROR FATAL OBTENIENDO TOKEN: ${e.message}", e)
            null
        }
}
