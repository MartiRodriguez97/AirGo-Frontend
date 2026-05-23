package com.airgo.airgoApp.features.notifications.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import com.airgo.airgoApp.MainActivity
import com.airgo.airgoApp.R
import com.airgo.airgoApp.core.network.RetrofitProvider
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.features.login.data.auth.UpdateFcmTokenRequest
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AirgoFirebaseService : FirebaseMessagingService() {

    override fun onCreate() {
        super.onCreate()
        Log.e("AIRGO_FCM", "### SERVICIO INICIALIZADO POR EL SISTEMA ###")
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.e("AIRGO_FCM", "NUEVO TOKEN GENERADO: $token")
        sendTokenToBackend(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.e("AIRGO_FCM", "====================================================")
        Log.e("AIRGO_FCM", "📩 ¡MENSAJE RECIBIDO DESDE FIREBASE!")
        Log.e("AIRGO_FCM", "Data (Payload): ${remoteMessage.data}")
        if (remoteMessage.notification != null) {
            Log.e("AIRGO_FCM", "⚠️ AVISO: Se ha recibido un objeto 'notification'. Esto hará que Android gestione la notificación automáticamente en segundo plano e ignore el agrupamiento.")
        }
        Log.e("AIRGO_FCM", "====================================================")

        // Intentamos detectar el tipo y el ID del chat/emisor usando lo visto en el WebSocket
        val type = remoteMessage.data["type"] ?: "chat"
        
        // El backend en WS usa 'emissor_id', intentamos sacarlo de ahí si 'chat_id' no viene
        val chatIdFromData = remoteMessage.data["conversa_id"] 
            ?: remoteMessage.data["id_conversa"] 
            ?: remoteMessage.data["chat_id"]
        
        val emissorId = remoteMessage.data["emissor_id"]
        var myId = SessionManager.userId
        
        // Si la app está cerrada, SessionManager.userId es null. 
        // Intentamos recuperarlo de las preferencias de forma rápida.
        if (myId == null) {
            val sharedPrefs = getSharedPreferences("user_settings", android.content.Context.MODE_PRIVATE)
            // Nota: DataStore es preferible, pero para un servicio en background 
            // que necesita el ID YA, consultamos el nombre del archivo de SharedPreferences
            // que genera DataStore (o si usas SharedPreferences directamente).
            // Si usas puramente DataStore, intentaremos sacarlo del payload si el backend lo incluyera.
            Log.e("AIRGO_FCM", "SessionManager.userId es NULL (app cerrada)")
        }

        // Construimos el ID compuesto X-Y que espera el API
        val finalChatId = if (emissorId != null && myId != null) {
            val otherId = emissorId.toIntOrNull() ?: 0
            if (myId < otherId) "$myId-$otherId" else "$otherId-$myId"
        } else {
            // Si no podemos calcular el compuesto, usamos lo que venga, 
            // pero esto es lo que está dando el 404.
            remoteMessage.data["conversa_id"] ?: remoteMessage.data["chat_id"] ?: emissorId ?: "0"
        }

        Log.e("AIRGO_FCM", "ID calculado para API: $finalChatId (Emisor: $emissorId, MiID: $myId)")

        when (type) {
            "chat" -> handleChatNotification(remoteMessage, finalChatId)
            else -> {
                Log.e("AIRGO_FCM", "Tipo desconocido ($type), mostrando por defecto")
                showSystemNotification(remoteMessage, finalChatId)
            }
        }
    }

    private fun handleChatNotification(remoteMessage: RemoteMessage, chatId: String) {
        val currentChat = SessionManager.currentChatId
        Log.e("AIRGO_FCM", "Validando chat: recibido=$chatId, actual_en_pantalla=$currentChat")

        // Si el usuario está en la pantalla de ese chat concreto, no mostramos notificación
        if (currentChat == chatId) {
            Log.e("AIRGO_FCM", "❌ Notificación OMITIDA: El usuario ya está viendo el chat $chatId")
            return
        }
        
        Log.e("AIRGO_FCM", "✅ Mostrando notificación de chat...")
        showSystemNotification(remoteMessage, chatId)
    }

    private fun showSystemNotification(remoteMessage: RemoteMessage, explicitChatId: String? = null) {
        val channelId = "chat_notifications"
        val notificationManager = getSystemService(NotificationManager::class.java)

        // Unificar ID para asegurar que el grupo sea consistente
        val chatId = explicitChatId ?: "0"
        val groupKey = "com.airgo.airgoApp.CHAT_$chatId"
        
        Log.e("AIRGO_FCM", "Mostrando notificación: ChatId=$chatId, GroupKey=$groupKey")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Missatges de AirGo",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificacions dels chats"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val title = remoteMessage.data["emissor_username"] ?: remoteMessage.notification?.title ?: "AirGo"
        val body = remoteMessage.data["contingut"] ?: remoteMessage.data["body"] ?: remoteMessage.notification?.body ?: ""

        // Intent para abrir el chat
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("CONV_ID", chatId)
            putExtra("OTHER_USER_ID", remoteMessage.data["emissor_id"])
            putExtra("OTHER_USERNAME", title)
            putExtra("FROM_NOTIFICATION", true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, chatId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 1. Notificación individual (el hijo)
        val person = Person.Builder().setName(title).build()
        val messagingStyle = NotificationCompat.MessagingStyle(person)
            .addMessage(body, System.currentTimeMillis(), person)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setStyle(messagingStyle)
            .setGroup(groupKey) // <--- CLAVE DE GRUPO
            .setSortKey(System.currentTimeMillis().toString()) // Para ordenar correctamente
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .build()

        // Notificación individual con ID único basado en el tiempo para que se acumulen
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)

        // 2. Notificación de RESUMEN (el padre)
        // Es vital que el Summary tenga el MISMO groupKey y setGroupSummary(true)
        val summaryNotification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText("Tens missatges nous")
            .setGroup(groupKey) // <--- MISMA CLAVE
            .setGroupSummary(true) // <--- INDICA QUE ES EL PADRE
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true) // No vibrar por cada mensaje del mismo grupo
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // El ID del resumen debe ser FIJO por chat (usamos el hash del groupKey)
        notificationManager.notify(groupKey.hashCode(), summaryNotification)
    }

    private fun sendTokenToBackend(token: String) {
        if (SessionManager.accessToken == null) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitProvider.authApi.updateFcmToken(UpdateFcmTokenRequest(token))
                if (response.isSuccessful) {
                    Log.e("AIRGO_FCM", "Token actualizado en servidor con éxito")
                } else {
                    Log.e("AIRGO_FCM", "Error al actualizar token: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("AIRGO_FCM", "Error de red enviando token", e)
            }
        }
    }
}
