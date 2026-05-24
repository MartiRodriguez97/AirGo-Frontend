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
import com.airgo.airgoApp.core.network.RetrofitProvider
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.core.network.UserPreferencesRepository
import com.airgo.airgoApp.core.network.dataStore
import com.google.gson.Gson
import com.airgo.airgoApp.features.notifications.NotificationUtils.getBitmapFromUrl
import com.airgo.airgoApp.features.notifications.NotificationUtils.getCircularBitmap
import com.airgo.airgoApp.features.notifications.service.AirgoFirebaseService
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

class NotificationActionReceiver : BroadcastReceiver() {
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val action = intent.action
        val chatId = intent.getStringExtra("CHAT_ID") ?: ""
        val otherUserId = intent.getIntExtra("OTHER_USER_ID", 0)
        val notificationId = intent.getIntExtra("NOTIFICATION_ID", 0)

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                Log.d("AirGo_Receiver", "Acció rebuda: $action per a chatId: $chatId")
                val token = ensureToken(context)
                
                if (token != null && chatId.isNotEmpty()) {
                    when (action) {
                        "com.airgo.airgoApp.ACTION_MARK_AS_READ" -> {
                            markAsRead(chatId)
                        }
                        "com.airgo.airgoApp.ACTION_REPLY" -> {
                            val replyText = RemoteInput.getResultsFromIntent(intent)
                                ?.getCharSequence("KEY_TEXT_REPLY")?.toString()
                            
                            Log.d("AirGo_Receiver", "Text de resposta: $replyText")
                            if (!replyText.isNullOrBlank()) {
                                updateNotificationWithReply(context, chatId, notificationId, replyText)
                                withTimeoutOrNull(10000) {
                                    sendReply(otherUserId, chatId, replyText, token)
                                }
                            }
                        }
                    }
                } else {
                    Log.e("AirGo_Receiver", "Token o ChatId absents. Token: ${token != null}, ChatId: $chatId")
                }
            } catch (e: Exception) {
                Log.e("AirGo_Receiver", "Error processant acció: ${e.message}", e)
            } finally {
                if (action == "com.airgo.airgoApp.ACTION_MARK_AS_READ") {
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(notificationId)
                    
                    // Si ya no quedan notificaciones de chat, cancelamos también el resumen
                    val activeNotifications = notificationManager.activeNotifications
                    val hasMoreChats = activeNotifications.any { 
                        it.id != AirgoFirebaseService.SUMMARY_ID && 
                        it.id != notificationId &&
                        it.notification.group == AirgoFirebaseService.CHAT_GROUP_KEY 
                    }
                    
                    if (!hasMoreChats) {
                        notificationManager.cancel(AirgoFirebaseService.SUMMARY_ID)
                    }
                }

                pendingResult.finish()
            }
        }
    }

    private fun updateNotificationWithReply(context: Context, chatId: String, notificationId: Int, replyText: String) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val activeNotif = notificationManager.activeNotifications.find { it.id == notificationId } ?: return
            val oldNotification = activeNotif.notification
            
            val style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(oldNotification) ?: return
            
            // Cargamos nuestra propia foto para que la burbuja de respuesta tenga nuestro avatar
            val myProfilePicUrl = SessionManager.profilePictureUrl
            val myBitmap = getBitmapFromUrl(myProfilePicUrl)?.let { getCircularBitmap(it) }
            val myIcon = myBitmap?.let { IconCompat.createWithBitmap(it) }

            val userPerson = Person.Builder()
                .setName("Jo")
                .apply { if (myIcon != null) setIcon(myIcon) }
                .build()
            
            style.addMessage(replyText, System.currentTimeMillis(), userPerson)
            
            val updatedNotification = NotificationCompat.Builder(context, oldNotification)
                .setStyle(style)
                .setOnlyAlertOnce(true)
                .build()
                
            notificationManager.notify(notificationId, updatedNotification)
            Log.d("AirGo_Receiver", "Notificació actualitzada amb la resposta")
        } catch (e: Exception) {
            Log.e("AirGo_Receiver", "Error actualitzant notificació", e)
        }
    }

    private suspend fun markAsRead(chatId: String) {
        try {
            val response = RetrofitProvider.chatApi.markAsRead(chatId)
            Log.d("AirGo_Receiver", "Marcar com llegit ($chatId): ${response.isSuccessful}")
        } catch (e: Exception) {
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
            Log.d("AirGo_Receiver", "Sessió restaurada: ID=${prefs.userId}, Pic=${prefs.profilePictureUrl != null}")
            prefs.token
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun sendReply(otherUserId: Int, chatId: String, text: String, token: String) {
        val baseUrl = BuildConfig.BACKEND_URL
        val wsUrl = baseUrl
            .replace("http://", "ws://")
            .replace("https://", "wss://")
            .trimEnd('/') + "/ws/chat/$otherUserId/?token=$token"

        Log.d("AirGo_Receiver", "Connectant a WS per respondre: $wsUrl")
        val request = Request.Builder().url(wsUrl).build()

        kotlinx.coroutines.suspendCancellableCoroutine<Unit> { continuation ->
            var isFinished = false
            val webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d("AirGo_Receiver", "WS Obert, enviant missatge...")
                    val payload = gson.toJson(mapOf("missatge" to text))
                    webSocket.send(payload)
                    webSocket.close(1000, "Reply sent")
                    
                    CoroutineScope(Dispatchers.IO).launch {
                        markAsRead(chatId)
                        if (!isFinished) {
                            isFinished = true
                            continuation.resume(Unit)
                        }
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e("AirGo_Receiver", "WS Error enviant resposta: ${t.message}", t)
                    if (!isFinished) {
                        isFinished = true
                        continuation.resume(Unit)
                    }
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d("AirGo_Receiver", "WS Tancat: $reason")
                    if (!isFinished) {
                        isFinished = true
                        continuation.resume(Unit)
                    }
                }
            })
            continuation.invokeOnCancellation { webSocket.cancel() }
        }
    }
}
