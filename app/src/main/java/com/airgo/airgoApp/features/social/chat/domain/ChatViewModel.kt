package com.airgo.airgoApp.features.social.chat.domain

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airgo.airgoApp.BuildConfig
import com.airgo.airgoApp.R
import com.airgo.airgoApp.core.events.ChatEventBus
import com.airgo.airgoApp.core.network.RetrofitProvider
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.core.utils.UiText
import com.airgo.airgoApp.features.social.chat.data.Message
import com.airgo.airgoApp.features.social.chat.data.WebSocketMessage
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import retrofit2.HttpException
import java.io.IOException

private const val LOG_TAG = "ChatViewModel"
private const val WS_BASE_URL = BuildConfig.BACKEND_URL
private const val ERROR_CODE_NOT_FOUND = 404
private const val SOCKET_RESPONSE = 1000
private const val TYPE_MESSAGES_READ = "messages_read"

sealed class ChatUiState {
    data object Loading : ChatUiState()

    data object Empty : ChatUiState()

    data class Success(
        val messages: List<Message>,
    ) : ChatUiState()

    data class Error(
        val message: UiText.StringResource,
    ) : ChatUiState()
}

private const val DELAY_500MS = 500L
private const val ATTEMPTS_ZERO = 0
private const val ATTEMPTS_MAX = 10

class ChatViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    val uiState: StateFlow<ChatUiState> = _uiState

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private var webSocket: WebSocket? = null
    private var conversationId: String = ""
    private val gson = Gson()

    fun init(
        convId: String,
        otherUserId: Int,
    ) {
        viewModelScope.launch {
            // Esperar a que SessionManager.userId esté disponible si es un cold start
            var attempts = ATTEMPTS_ZERO
            while (SessionManager.userId == null && attempts < ATTEMPTS_MAX) {
                kotlinx.coroutines.delay(DELAY_500MS)
                attempts++
            }

            val currentUserId = SessionManager.userId
            // Si recibimos un ID simple (como "7"), lo convertimos al formato compuesto "X-Y"
            // que es lo que espera el endpoint de mensajes.
            val finalConvId =
                if (!convId.contains("-") && currentUserId != null) {
                    if (currentUserId < otherUserId) "$currentUserId-$otherUserId" else "$otherUserId-$currentUserId"
                } else {
                    convId
                }

            if (conversationId == finalConvId && _messages.value.isNotEmpty()) {
                return@launch
            }

            conversationId = finalConvId
            _messages.value = emptyList()
            loadMessages(finalConvId)
            connectWebSocket(otherUserId)

            viewModelScope.launch {
                ChatEventBus.messageReceived.collect { updatedChatId ->
                    if (updatedChatId == conversationId) {
                        loadMessages(conversationId)
                    }
                }
            }
        }
    }

    private fun loadMessages(convId: String) {
        viewModelScope.launch {
            _uiState.value = ChatUiState.Loading
            try {
                val msgs = RetrofitProvider.chatApi.getMessages(convId)
                _messages.value = msgs
                _uiState.value = if (msgs.isEmpty()) ChatUiState.Empty else ChatUiState.Success(msgs)
            } catch (e: HttpException) {
                if (e.code() == ERROR_CODE_NOT_FOUND) {
                    _messages.value = emptyList()
                    _uiState.value = ChatUiState.Empty
                } else {
                    _uiState.value = ChatUiState.Error(UiText.StringResource(R.string.error_server_generic, e.code()))
                }
            } catch (e: IOException) {
                Log.e(LOG_TAG, "❌ Error de red cargando mensajes (ID: $convId)", e)
                _uiState.value = ChatUiState.Error(UiText.StringResource(R.string.error_network_generic))
            }
        }
    }

    private fun connectWebSocket(otherUserId: Int) {
        val token = SessionManager.accessToken ?: return
        val wsUrl =
            WS_BASE_URL
                .replace("http://", "ws://")
                .replace("https://", "wss://")
                .trimEnd('/') + "/ws/chat/$otherUserId/?token=$token"

        val client = OkHttpClient()
        val request = Request.Builder().url(wsUrl).build()

        webSocket =
            client.newWebSocket(
                request,
                object : WebSocketListener() {
                    override fun onOpen(
                        webSocket: WebSocket,
                        response: Response,
                    ) {
                        _isConnected.value = true
                    }

                    override fun onMessage(
                        webSocket: WebSocket,
                        text: String,
                    ) {

                        try {
                            val json = gson.fromJson(text, JsonObject::class.java)
                            val type = json.get("type")?.asString

                            when (type) {
                                TYPE_MESSAGES_READ -> handleMessagesRead(json)
                                else -> handleChatMessage(text)
                            }
                        } catch (e: JsonSyntaxException) {
                            Log.e(LOG_TAG, "Invalid JSON format", e)
                        } catch (e: JsonParseException) {
                            Log.e(LOG_TAG, "JSON parsing error", e)
                        }
                    }

                    override fun onFailure(
                        webSocket: WebSocket,
                        t: Throwable,
                        response: Response?,
                    ) {
                        _isConnected.value = false
                        Log.e(LOG_TAG, "WebSocket failure: ${t.message}", t)
                    }

                    override fun onClosed(
                        webSocket: WebSocket,
                        code: Int,
                        reason: String,
                    ) {
                        _isConnected.value = false
                    }
                },
            )
    }

    private fun handleMessagesRead(json: JsonObject) {
        val readBy = json.get("read_by")?.asInt ?: return
        val currentUserId = SessionManager.userId ?: return

        if (readBy != currentUserId) {
            viewModelScope.launch {
                val updated =
                    _messages.value
                        .map { msg -> if (msg.emissor == currentUserId) msg.copy(llegit = true) else msg }
                        .toMutableList()
                _messages.value = updated.toList()
                _uiState.value = ChatUiState.Success(_messages.value)
            }
        }
    }

    private fun handleChatMessage(text: String) {
        val wsMsg = gson.fromJson(text, WebSocketMessage::class.java)
        if (wsMsg.contingut != null) {
            val newMsg =
                Message(
                    id = wsMsg.id ?: 0,
                    emissor = wsMsg.emissor ?: 0,
                    emissorUsername = wsMsg.emissorUsername ?: "",
                    emissorProfilePic = wsMsg.emissorProfilePic,
                    contingut = wsMsg.contingut,
                    enviatAt = wsMsg.enviatAt ?: "",
                    llegit = false,
                )
            viewModelScope.launch {
                if (_messages.value.none { it.id == newMsg.id }) {
                    _messages.value += newMsg
                    _uiState.value = ChatUiState.Success(_messages.value)
                    if (newMsg.emissor != SessionManager.userId && conversationId.isNotEmpty()) {
                        try {
                            RetrofitProvider.chatApi.markAsRead(conversationId)
                        } catch (e: HttpException) {
                            Log.e(LOG_TAG, "Error markAsRead HTTP: ${e.message()}", e)
                        } catch (e: IOException) {
                            Log.e(LOG_TAG, "Error markAsRead IO: ${e.message}", e)
                        }
                    }
                }
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val payload = gson.toJson(mapOf("missatge" to text))
        webSocket?.send(payload)
    }

    fun deleteMessage(messageId: Int) {
        viewModelScope.launch {
            try {
                RetrofitProvider.chatApi.deleteMessage(messageId)
                _messages.value = _messages.value.filter { it.id != messageId }
                if (_messages.value.isEmpty()) _uiState.value = ChatUiState.Empty
            } catch (e: HttpException) {
                _uiState.value = ChatUiState.Error(UiText.StringResource(R.string.error_server_generic, e.code()))
                Log.e(LOG_TAG, "HTTP error deleting message: ${e.code()}", e)
            } catch (e: IOException) {
                _uiState.value = ChatUiState.Error(UiText.StringResource(R.string.error_network_generic))
                Log.e(LOG_TAG, "Network error deleting message: ${e.message}", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        webSocket?.close(SOCKET_RESPONSE, "ViewModel cleared")
    }
}
