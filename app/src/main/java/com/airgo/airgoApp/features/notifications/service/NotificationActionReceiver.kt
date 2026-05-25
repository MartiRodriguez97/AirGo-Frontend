package com.airgo.airgoApp.features.notifications.service

import android.app.NotificationManager
import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import com.airgo.airgoApp.BuildConfig
import com.airgo.airgoApp.R
import com.airgo.airgoApp.core.network.RetrofitProvider
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.core.network.UserPreferencesRepository
import com.airgo.airgoApp.core.network.dataStore
import com.airgo.airgoApp.features.notifications.NotificationUtils.getBitmapFromUrl
import com.airgo.airgoApp.features.notifications.NotificationUtils.getCircularBitmap
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

private const val CODE_1000 = 1000

private const val MAX_TIME_MS = 10000L
private const val MAX_TIMEOUT = 10L

class NotificationActionReceiver : BroadcastReceiver() {
    private val gson = Gson()
    private val client =
        OkHttpClient
            .Builder()
            .connectTimeout(MAX_TIMEOUT, TimeUnit.SECONDS)
            .build()

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val pendingResult = goAsync()
        val action = intent.action
        val chatId = intent.getStringExtra("CHAT_ID") ?: ""
        val otherUserId = intent.getIntExtra("OTHER_USER_ID", 0)
        val notificationId = intent.getIntExtra("NOTIFICATION_ID", 0)

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val token = ensureToken(context)

                if (token != null && chatId.isNotEmpty()) {
                    when (action) {
                        "com.airgo.airgoApp.ACTION_MARK_AS_READ" -> {
                            markAsRead(chatId)
                        }
                        "com.airgo.airgoApp.ACTION_REPLY" -> {
                            val replyText =
                                RemoteInput
                                    .getResultsFromIntent(intent)
                                    ?.getCharSequence("KEY_TEXT_REPLY")
                                    ?.toString()

                            if (!replyText.isNullOrBlank()) {
                                updateNotificationWithReply(context, notificationId, replyText)
                                withTimeoutOrNull(MAX_TIME_MS) {
                                    sendReply(otherUserId, chatId, replyText, token)
                                }
                            }
                        }
                    }
                }
            } catch (e: java.lang.Exception) {
                Log.e("AirGo_Receiver", "Error processant acció: ${e.message}", e)
            } finally {
                if (action == "com.airgo.airgoApp.ACTION_MARK_AS_READ") {
                    val notificationManager =
                        context.getSystemService(
                            Context.NOTIFICATION_SERVICE,
                        ) as NotificationManager
                    notificationManager.cancel(notificationId)

                    // Si ya no quedan notificaciones de chat, cancelamos también el resumen
                    val activeNotifications = notificationManager.activeNotifications
                    val hasMoreChats =
                        activeNotifications.any {
                            it.id != FirebaseMessagingService.SUMMARY_ID &&
                                it.id != notificationId &&
                                it.notification.group == FirebaseMessagingService.CHAT_GROUP_KEY
                        }

                    if (!hasMoreChats) {
                        notificationManager.cancel(FirebaseMessagingService.SUMMARY_ID)
                    }
                }

                pendingResult.finish()
            }
        }
    }

    private fun updateNotificationWithReply(
        context: Context,
        notificationId: Int,
        replyText: String,
    ) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val activeNotif = notificationManager.activeNotifications.find { it.id == notificationId } ?: return
            val oldNotification = activeNotif.notification

            val style =
                NotificationCompat.MessagingStyle
                    .extractMessagingStyleFromNotification(oldNotification) ?: return

            // Cargamos nuestra propia foto para que la burbuja de respuesta tenga nuestro avatar
            val myProfilePicUrl = SessionManager.profilePictureUrl
            val myBitmap = getBitmapFromUrl(myProfilePicUrl)?.let { getCircularBitmap(it) }
            val myIcon = myBitmap?.let { IconCompat.createWithBitmap(it) }

            val userPerson =
                Person
                    .Builder()
                    .setName(context.getString(R.string.chat_user_me))
                    .apply { if (myIcon != null) setIcon(myIcon) }
                    .build()

            style.addMessage(replyText, System.currentTimeMillis(), userPerson)

            val updatedNotification =
                NotificationCompat
                    .Builder(context, oldNotification)
                    .setStyle(style)
                    .setOnlyAlertOnce(true)
                    .build()

            notificationManager.notify(notificationId, updatedNotification)
        } catch (e: java.lang.Exception) {
            Log.e("AirGo_Receiver", "Error actualitzant notificació", e)
        }
    }

    private suspend fun markAsRead(chatId: String) {
        try {
            RetrofitProvider.chatApi.markAsRead(chatId)
        } catch (e: java.lang.Exception) {
            Log.e("AirGo_Receiver", "Error markAsRead", e)
        }
    }

    private suspend fun ensureToken(context: Context): String? {
        if (SessionManager.accessToken != null && SessionManager.profilePictureUrl != null) {
            return SessionManager.accessToken
        }
        return try {
            val repository = UserPreferencesRepository(context.dataStore)
            val prefs = repository.userPreferencesFlow.first()
            SessionManager.accessToken = prefs.token
            SessionManager.userId = prefs.userId
            SessionManager.profilePictureUrl = prefs.profilePictureUrl
            prefs.token
        } catch (e: java.lang.Exception) {
            Log.e("AirGo_Receiver", "Error restaurando session", e)
            null
        }
    }

    private suspend fun sendReply(
        otherUserId: Int,
        chatId: String,
        text: String,
        token: String,
    ) {
        val baseUrl = BuildConfig.BACKEND_URL
        val wsUrl =
            baseUrl
                .replace("http://", "ws://")
                .replace("https://", "wss://")
                .trimEnd('/') + "/ws/chat/$otherUserId/?token=$token"

        val request = Request.Builder().url(wsUrl).build()

        kotlinx.coroutines.suspendCancellableCoroutine<Unit> { continuation ->
            var isFinished = false
            val webSocket =
                client.newWebSocket(
                    request,
                    object : WebSocketListener() {
                        override fun onOpen(
                            webSocket: WebSocket,
                            response: Response,
                        ) {
                            val payload = gson.toJson(mapOf("missatge" to text))
                            webSocket.send(payload)
                            webSocket.close(CODE_1000, "Reply sent")

                            CoroutineScope(Dispatchers.IO).launch {
                                markAsRead(chatId)
                                if (!isFinished) {
                                    isFinished = true
                                    continuation.resume(Unit)
                                }
                            }
                        }

                        override fun onFailure(
                            webSocket: WebSocket,
                            t: Throwable,
                            response: Response?,
                        ) {
                            Log.e("AirGo_Receiver", "WS Error enviant resposta: ${t.message}", t)
                            if (!isFinished) {
                                isFinished = true
                                continuation.resume(Unit)
                            }
                        }

                        override fun onClosed(
                            webSocket: WebSocket,
                            code: Int,
                            reason: String,
                        ) {
                            if (!isFinished) {
                                isFinished = true
                                continuation.resume(Unit)
                            }
                        }
                    },
                )
            continuation.invokeOnCancellation { webSocket.cancel() }
        }
    }
}
