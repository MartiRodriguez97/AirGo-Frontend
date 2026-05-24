package com.airgo.airgoApp.features.notifications.service

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.airgo.airgoApp.MainActivity
import com.airgo.airgoApp.R
import com.airgo.airgoApp.core.network.RetrofitProvider
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.core.network.UserPreferencesRepository
import com.airgo.airgoApp.core.network.dataStore
import com.airgo.airgoApp.features.login.data.auth.UpdateFcmTokenRequest
import com.airgo.airgoApp.features.notifications.NotificationUtils.getBitmapFromUrl
import com.airgo.airgoApp.features.notifications.NotificationUtils.getCircularBitmap
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

class FirebaseMessagingService : FirebaseMessagingService() {
    companion object {
        const val CHAT_GROUP_KEY = "com.airgo.airgoApp.CHAT_GROUP"
        const val SUMMARY_ID = 1000
    }

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
        val type = remoteMessage.data["type"] ?: "chat"
        val emissorId = remoteMessage.data["emissor_id"]
        val receiverId = remoteMessage.data["receiver_id"]?.toIntOrNull()

        CoroutineScope(Dispatchers.IO).launch {
            if (SessionManager.userId == null) {
                try {
                    val repository = UserPreferencesRepository(applicationContext.dataStore)
                    val prefs = repository.userPreferencesFlow.first()
                    if (prefs.userId != null) {
                        SessionManager.userId = prefs.userId
                        SessionManager.accessToken = prefs.token
                        SessionManager.profilePictureUrl = prefs.profilePictureUrl
                        Log.e("AIRGO_FCM", "Session restaurada desde DataStore: ${prefs.userId}")
                    }
                } catch (e: Exception) {
                    Log.e("AIRGO_FCM", "Error restaurando session", e)
                }
            }

            // Filtrado estricto por usuario
            val currentUserId = SessionManager.userId
            if (receiverId != null && receiverId != currentUserId) {
                Log.e(
                    "AIRGO_FCM",
                    "Notificación bloqueada: Destinatario ($receiverId) " +
                        "no coincide con usuario activo ($currentUserId)",
                )
                return@launch
            }

            val myId = currentUserId
            val finalChatId =
                if (emissorId != null && myId != null) {
                    val otherId = emissorId.toIntOrNull() ?: 0
                    if (myId < otherId) "$myId-$otherId" else "$otherId-$myId"
                } else {
                    remoteMessage.data["conversa_id"] ?: remoteMessage.data["chat_id"] ?: emissorId ?: "0"
                }

            when (type) {
                "chat" -> handleChatNotification(remoteMessage, finalChatId)
                else -> showSystemNotification(remoteMessage, finalChatId)
            }
        }
    }

    private fun handleChatNotification(
        remoteMessage: RemoteMessage,
        chatId: String,
    ) {
        val currentChat = SessionManager.currentChatId
        val notificationManager = getSystemService(NotificationManager::class.java)

        // Solo silenciamos si el chat está abierto Y la app está en primer plano
        if (currentChat == chatId && isAppInForeground()) {
            notificationManager.cancel(chatId.hashCode())
            val activeNotifications = notificationManager.activeNotifications
            val hasMoreChats =
                activeNotifications.any {
                    it.id != chatId.hashCode() &&
                        it.id != SUMMARY_ID &&
                        it.notification.group == CHAT_GROUP_KEY
                }
            if (!hasMoreChats) {
                notificationManager.cancel(SUMMARY_ID)
            }
            return
        }

        showSystemNotification(remoteMessage, chatId)
    }

    private fun isAppInForeground(): Boolean =
        try {
            val processInfo = ActivityManager.RunningAppProcessInfo()
            ActivityManager.getMyMemoryState(processInfo)
            processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        } catch (e: Exception) {
            false
        }

    private fun showSystemNotification(
        remoteMessage: RemoteMessage,
        explicitChatId: String? = null,
    ) {
        val channelId = "chat_notifications"
        val notificationManager = getSystemService(NotificationManager::class.java)
        val chatId = explicitChatId ?: "0"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    channelId,
                    "Missatges de AirGo",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Notificacions dels chats"
                    enableLights(true)
                    enableVibration(true)
                    setShowBadge(true)
                }
            notificationManager.createNotificationChannel(channel)
        }

        val title = remoteMessage.data["emissor_username"] ?: remoteMessage.notification?.title ?: "AirGo"
        val body =
            remoteMessage.data["contingut"]
                ?: remoteMessage.data["body"]
                ?: remoteMessage.notification?.body ?: ""
        val emissorId = remoteMessage.data["emissor_id"] ?: "0"

        val profilePicUrl =
            remoteMessage.data["emissor_profile_pic"]
                ?: remoteMessage.data["profile_pic"]
                ?: remoteMessage.data["emissor_photo"]

        val rawBitmap = getBitmapFromUrl(profilePicUrl)
        val circularBitmap = rawBitmap?.let { getCircularBitmap(it) }

        val intent =
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("CONV_ID", chatId)
                putExtra("OTHER_USER_ID", emissorId)
                putExtra("OTHER_USERNAME", title)
                putExtra("OTHER_PROFILE_PIC", profilePicUrl) // Añadimos esto
                putExtra("FROM_NOTIFICATION", true)
            }

        val pendingIntent =
            PendingIntent.getActivity(
                this,
                chatId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val readIntent =
            Intent(this, NotificationActionReceiver::class.java).apply {
                action = "com.airgo.airgoApp.ACTION_MARK_AS_READ"
                putExtra("CHAT_ID", chatId)
                putExtra("NOTIFICATION_ID", chatId.hashCode())
            }
        val readPendingIntent =
            PendingIntent.getBroadcast(
                this,
                chatId.hashCode() + 1,
                readIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val remoteInput =
            RemoteInput
                .Builder("KEY_TEXT_REPLY")
                .setLabel("Escriu una resposta...")
                .build()

        val replyIntent =
            Intent(this, NotificationActionReceiver::class.java).apply {
                action = "com.airgo.airgoApp.ACTION_REPLY"
                putExtra("CHAT_ID", chatId)
                putExtra("OTHER_USER_ID", emissorId.toIntOrNull() ?: 0)
                putExtra("NOTIFICATION_ID", chatId.hashCode())
            }
        val replyPendingIntent =
            PendingIntent.getBroadcast(
                this,
                chatId.hashCode() + 2,
                replyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )

        val replyAction =
            NotificationCompat.Action
                .Builder(
                    R.drawable.ic_launcher_foreground,
                    "Respondre",
                    replyPendingIntent,
                ).addRemoteInput(remoteInput)
                .build()

        val readAction =
            NotificationCompat.Action
                .Builder(
                    R.drawable.ic_launcher_foreground, // Using an icon instead of 0
                    "Marcar com a vist",
                    readPendingIntent,
                ).build()

        // --- CONFIGURACIÓN DE MESSAGINGSTYLE (Estilo WhatsApp) ---
        // Cargamos nuestra propia foto para las respuestas
        val myProfilePicUrl = SessionManager.profilePictureUrl
        val myBitmap = getBitmapFromUrl(myProfilePicUrl)?.let { getCircularBitmap(it) }
        val myIcon = myBitmap?.let { IconCompat.createWithBitmap(it) }

        val userPerson =
            Person
                .Builder()
                .setName("Jo")
                .apply { if (myIcon != null) setIcon(myIcon) }
                .build()

        val avatarIcon = circularBitmap?.let { IconCompat.createWithBitmap(it) }
        val senderPerson =
            Person
                .Builder()
                .setName(title)
                .apply { if (avatarIcon != null) setIcon(avatarIcon) }
                .build()

        // Publicar Shortcut para habilitar funciones de "Conversación" (Avatar con overlay) en Android 11+
        val shortcut =
            ShortcutInfoCompat
                .Builder(this, chatId)
                .setShortLabel(title)
                .setPerson(senderPerson)
                .setIntent(intent.setAction(Intent.ACTION_VIEW))
                .setLongLived(true)
                .build()
        ShortcutManagerCompat.pushDynamicShortcut(this, shortcut)

        // Buscamos si ya existe una notificación para este chat
        val activeNotif = notificationManager.activeNotifications.find { it.id == chatId.hashCode() }
        val existingStyle =
            activeNotif?.let {
                NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(it.notification)
            }

        var messagingStyle = existingStyle ?: NotificationCompat.MessagingStyle(userPerson)

        // Si el último mensaje en el historial es nuestro ("Jo"), empezamos un nuevo estilo
        // para que solo se vea lo nuevo del otro usuario tras haber respondido,
        // cumpliendo el comportamiento tipo WhatsApp tras una respuesta.
        if (messagingStyle.messages.isNotEmpty() &&
            messagingStyle.messages
                .last()
                .person
                ?.name == "Jo"
        ) {
            messagingStyle = NotificationCompat.MessagingStyle(userPerson)
        }

        messagingStyle.setConversationTitle(null)
        messagingStyle.isGroupConversation = false

        // Añadimos el mensaje con timestamp real de FCM si está disponible
        val messageTimestamp = if (remoteMessage.sentTime > 0) remoteMessage.sentTime else System.currentTimeMillis()

        // Evitar duplicados exactos (mismo texto y timestamp cercano)
        val isDuplicate =
            messagingStyle.messages.any {
                it.text == body && abs(it.timestamp - messageTimestamp) < 1000
            }

        if (body.isNotBlank() && !isDuplicate) {
            messagingStyle.addMessage(body, messageTimestamp, senderPerson)
        }

        // Limitamos historial a los últimos 10 mensajes
        var finalStyle = messagingStyle
        if (messagingStyle.messages.size > 10) {
            val recentMessages = messagingStyle.messages.takeLast(10)
            val newStyle = NotificationCompat.MessagingStyle(userPerson)
            newStyle.setConversationTitle(null)
            newStyle.isGroupConversation = false
            for (msg in recentMessages) {
                newStyle.addMessage(msg)
            }
            finalStyle = newStyle
        }

        buildAndNotify(chatId, finalStyle, circularBitmap, pendingIntent, replyAction, readAction, notificationManager)

        // Resumen global
        val summaryNotification =
            NotificationCompat
                .Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("AirGo")
                .setContentText(if (title != "AirGo" && title.isNotEmpty()) "$title: $body" else body)
                .setGroup(CHAT_GROUP_KEY)
                .setGroupSummary(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent)
                .build()

        notificationManager.notify(SUMMARY_ID, summaryNotification)
    }

    private fun buildAndNotify(
        chatId: String,
        style: NotificationCompat.MessagingStyle,
        circularBitmap: Bitmap?,
        pendingIntent: PendingIntent,
        replyAction: NotificationCompat.Action,
        readAction: NotificationCompat.Action,
        notificationManager: NotificationManager,
    ) {
        val channelId = "chat_notifications"
        val notificationBuilder =
            NotificationCompat
                .Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setGroup(CHAT_GROUP_KEY)
                .setShortcutId(chatId)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .addAction(replyAction)
                .addAction(readAction)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setOnlyAlertOnce(false)
                .setStyle(style)

        if (circularBitmap != null) {
            notificationBuilder.setLargeIcon(circularBitmap)
        }

        notificationManager.notify(chatId.hashCode(), notificationBuilder.build())
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
