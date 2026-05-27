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
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.airgo.airgoApp.MainActivity
import com.airgo.airgoApp.R
import com.airgo.airgoApp.core.events.ChatEventBus
import com.airgo.airgoApp.core.events.FriendEventBus
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

class FirebaseMessagingService : FirebaseMessagingService() {
    companion object {
        const val CHAT_GROUP_KEY = "com.airgo.airgoApp.CHAT_GROUP"
        const val SUMMARY_ID = 1000
        private const val CHANNEL_ID = "chat_notifications"
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        sendTokenToBackend(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val type = remoteMessage.data["type"] ?: "chat"
        val emissorId = remoteMessage.data["emissor_id"]
        val receiverId = remoteMessage.data["receiver_id"]?.toIntOrNull()

        CoroutineScope(Dispatchers.IO).launch {
            ensureSessionActive()

            if (isNotificationForOtherUser(receiverId)) return@launch

            val finalChatId = resolveChatId(remoteMessage, emissorId)

            when (type) {
                "chat" -> {
                    handleChatNotification(remoteMessage, finalChatId)
                    ChatEventBus.emitMessage(finalChatId)
                }
                "friend_request" -> {
                    showFriendNotification(remoteMessage, isRequest = true)
                }
                "friend_accepted" -> {
                    FriendEventBus.emitFriendAccepted()
                    showFriendNotification(remoteMessage, isRequest = false)
                }
                else -> showSystemNotification(remoteMessage, finalChatId)
            }
        }
    }

    private suspend fun ensureSessionActive() {
        if (SessionManager.userId != null) return
        try {
            val repository = UserPreferencesRepository(applicationContext.dataStore)
            val prefs = repository.userPreferencesFlow.first()
            if (prefs.userId != null) {
                SessionManager.userId = prefs.userId
                SessionManager.accessToken = prefs.token
                SessionManager.profilePictureUrl = prefs.profilePictureUrl
            }
        } catch (e: java.lang.Exception) {
            Log.e("AIRGO_FCM", "Error restaurando session", e)
        }
    }

    private fun isNotificationForOtherUser(receiverId: Int?): Boolean {
        val currentUserId = SessionManager.userId
        val isForOther = receiverId != null && receiverId != currentUserId

        return isForOther
    }

    private fun resolveChatId(
        remoteMessage: RemoteMessage,
        emissorId: String?,
    ): String {
        val myId = SessionManager.userId
        return if (emissorId != null && myId != null) {
            val otherId = emissorId.toIntOrNull() ?: 0
            if (myId < otherId) "$myId-$otherId" else "$otherId-$myId"
        } else {
            remoteMessage.data["conversa_id"] ?: remoteMessage.data["chat_id"] ?: emissorId ?: "0"
        }
    }

    private fun handleChatNotification(
        remoteMessage: RemoteMessage,
        chatId: String,
    ) {
        val currentChat = SessionManager.currentChatId
        val notificationManager = getSystemService(NotificationManager::class.java)

        if (currentChat == chatId && isAppInForeground()) {
            notificationManager.cancel(chatId.hashCode())
            val hasMoreChats =
                notificationManager.activeNotifications.any {
                    it.id != chatId.hashCode() && it.id != SUMMARY_ID && it.notification.group == CHAT_GROUP_KEY
                }
            if (!hasMoreChats) notificationManager.cancel(SUMMARY_ID)
            return
        }

        showSystemNotification(remoteMessage, chatId)
    }

    private fun isAppInForeground(): Boolean =
        try {
            val processInfo = ActivityManager.RunningAppProcessInfo()
            ActivityManager.getMyMemoryState(processInfo)
            processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        } catch (ignored: java.lang.Exception) {
            Log.e("AIRGO_FCM", "Error verificando estado de la app", ignored)
            false
        }

    private fun showSystemNotification(
        remoteMessage: RemoteMessage,
        explicitChatId: String? = null,
    ) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        val chatId = explicitChatId ?: "0"

        createNotificationChannel(notificationManager)

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

        val circularBitmap = getBitmapFromUrl(profilePicUrl)?.let { getCircularBitmap(it) }

        val pendingIntent = createPendingIntent(chatId, emissorId, title, profilePicUrl)
        val (replyAction, readAction) = createActions(this, chatId, emissorId)

        val userPerson = createUserPerson(this)
        val senderPerson = createSenderPerson(title, circularBitmap)

        var messagingStyle = getOrInitMessagingStyle(this, chatId, userPerson)

        if (shouldResetStyle(this, messagingStyle)) {
            messagingStyle = NotificationCompat.MessagingStyle(userPerson)
        }

        messagingStyle.setConversationTitle(null)
        messagingStyle.isGroupConversation = false

        val messageTimestamp = if (remoteMessage.sentTime > 0) remoteMessage.sentTime else System.currentTimeMillis()

        if (body.isNotBlank() && !isDuplicateMessage(messagingStyle, body, messageTimestamp)) {
            messagingStyle.addMessage(body, messageTimestamp, senderPerson)
        }

        messagingStyle = trimMessagingStyleIfNeeded(messagingStyle, userPerson)

        val intentForShortcut =
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("CONV_ID", chatId)
                putExtra("OTHER_USER_ID", emissorId)
                putExtra("OTHER_USERNAME", title)
                putExtra("OTHER_PROFILE_PIC", profilePicUrl)
                putExtra("FROM_NOTIFICATION", true)
            }
        pushShortcut(chatId, title, circularBitmap, intentForShortcut)

        buildAndNotify(
            chatId,
            messagingStyle,
            circularBitmap,
            pendingIntent,
            replyAction,
            readAction,
            notificationManager,
        )

        showSummaryNotification(title, body, pendingIntent, notificationManager)
    }

    private fun showFriendNotification(
        remoteMessage: RemoteMessage,
        isRequest: Boolean,
    ) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel(notificationManager)

        val username =
            if (isRequest) {
                remoteMessage.data["solicitant_username"] ?: "Usuari"
            } else {
                remoteMessage.data["usuari_username"] ?: "Usuari"
            }

        val body =
            if (isRequest) {
                getString(R.string.friend_request)
            } else {
                getString(R.string.friend_accepted_body)
            }

        val profilePicUrl =
            if (isRequest) {
                remoteMessage.data["solicitant_profile_pic"]
            } else {
                remoteMessage.data["usuari_profile_pic"]
            }

        val circularBitmap = getBitmapFromUrl(profilePicUrl)?.let { getCircularBitmap(it) }

        val id =
            if (isRequest) {
                "friend_req_${remoteMessage.data["amistat_id"]}"
            } else {
                "friend_acc_${remoteMessage.data["usuari_id"]}"
            }

        val intent =
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("TARGET_SCREEN", "social")
                putExtra("FROM_NOTIFICATION", true)
            }
        val pendingIntent =
            PendingIntent.getActivity(
                this,
                id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val userPerson = createUserPerson(this)
        val senderPerson = createSenderPerson(username, circularBitmap)
        val style =
            NotificationCompat
                .MessagingStyle(userPerson)
                .addMessage(body, System.currentTimeMillis(), senderPerson)
                .setConversationTitle(null)
                .setGroupConversation(false)

        pushShortcut(id, username, circularBitmap, intent)

        val notificationBuilder =
            NotificationCompat
                .Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setStyle(style)
                .setShortcutId(id)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)

        if (isRequest) {
            val amistatId = remoteMessage.data["amistat_id"]?.toIntOrNull() ?: 0
            val actions = createFriendRequestActions(this, amistatId)
            actions.forEach { notificationBuilder.addAction(it) }
        }

        notificationManager.notify(id.hashCode(), notificationBuilder.build())
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
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
    }

    private fun createPendingIntent(
        chatId: String,
        emissorId: String,
        title: String?,
        profilePicUrl: String?,
    ): PendingIntent {
        val intent =
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("CONV_ID", chatId)
                putExtra("OTHER_USER_ID", emissorId)
                putExtra("OTHER_USERNAME", title)
                putExtra("OTHER_PROFILE_PIC", profilePicUrl)
                putExtra("FROM_NOTIFICATION", true)
            }
        return PendingIntent.getActivity(
            this,
            chatId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun pushShortcut(
        chatId: String,
        title: String,
        circularBitmap: Bitmap?,
        intent: Intent,
    ) {
        val senderPerson =
            androidx.core.app.Person
                .Builder()
                .setName(title)
                .setIcon(
                    circularBitmap?.let {
                        IconCompat.createWithBitmap(it)
                    },
                ).build()
        val shortcut =
            ShortcutInfoCompat
                .Builder(this, chatId)
                .setShortLabel(title)
                .setPerson(senderPerson)
                .setIntent(intent.apply { action = Intent.ACTION_VIEW })
                .setLongLived(true)
                .build()
        ShortcutManagerCompat.pushDynamicShortcut(this, shortcut)
    }

    private fun showSummaryNotification(
        title: String,
        body: String,
        pendingIntent: PendingIntent,
        notificationManager: NotificationManager,
    ) {
        val summaryNotification =
            NotificationCompat
                .Builder(this, CHANNEL_ID)
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
        val notificationBuilder =
            NotificationCompat
                .Builder(this, CHANNEL_ID)
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
                RetrofitProvider.authApi.updateFcmToken(UpdateFcmTokenRequest(token))
            } catch (e: java.lang.Exception) {
                Log.e("AIRGO_FCM", "Error de red enviando token", e)
            }
        }
    }
}
