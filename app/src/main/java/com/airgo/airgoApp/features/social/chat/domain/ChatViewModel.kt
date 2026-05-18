package com.airgo.airgoApp.features.social.chat.domain

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airgo.airgoApp.BuildConfig
import com.airgo.airgoApp.core.network.RetrofitProvider
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.features.social.chat.data.Message
import com.airgo.airgoApp.features.social.chat.data.WebSocketMessage
import com.google.gson.Gson
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

sealed class ChatUiState {
    data object Loading : ChatUiState()

    data object Empty : ChatUiState()

    data class Success(
        val messages: List<Message>,
    ) : ChatUiState()

    data class Error(
        val message: String?,
    ) : ChatUiState()
}

private const val ERROR_CODE_NOT_FOUND = 404

private const val SOCKET_RESPONSE = 1000

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
        conversationId = convId
        loadMessages(convId)
        connectWebSocket(otherUserId)
    }

    private fun loadMessages(convId: String) {
        viewModelScope.launch {
            _uiState.value = ChatUiState.Loading
            try {
                val msgs = RetrofitProvider.chatApi.getMessages(convId)
                _messages.value = msgs
                _uiState.value = if (msgs.isEmpty()) ChatUiState.Empty else ChatUiState.Success(msgs)
                RetrofitProvider.chatApi.markAsRead(convId)
            } catch (e: HttpException) {
                if (e.code() == ERROR_CODE_NOT_FOUND) {
                    _messages.value = emptyList()
                    _uiState.value = ChatUiState.Empty
                } else {
                    _uiState.value = ChatUiState.Error("Error del servidor: ${e.code()}")
                    Log.e(LOG_TAG, "HTTP failure: ${e.message()}", e)
                }
            } catch (e: IOException) {
                _uiState.value = ChatUiState.Error("Error de connexió")
                Log.e(LOG_TAG, "Network failure: ${e.message}", e)
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

        Log.d(LOG_TAG, "Connectant WebSocket a: $wsUrl")

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
                        Log.d(LOG_TAG, "WebSocket connected")
                    }

                    override fun onMessage(
                        webSocket: WebSocket,
                        text: String,
                    ) {
                        Log.d(LOG_TAG, "Rebut raw: $text")
                        try {
                            val wsMsg = gson.fromJson(text, WebSocketMessage::class.java)
                            Log.d(
                                LOG_TAG,
                                "Parsed: id=${wsMsg.id}, " +
                                    "emissor=${wsMsg.emissor}, contingut=${wsMsg.contingut}",
                            )
                            if (wsMsg.contingut != null) {
                                val newMsg =
                                    Message(
                                        id = wsMsg.id ?: 0,
                                        emissor = wsMsg.emissor ?: 0,
                                        emissorUsername = wsMsg.emissorUsername ?: "",
                                        contingut = wsMsg.contingut,
                                        enviatAt = wsMsg.enviatAt ?: "",
                                        llegit = false,
                                    )
                                viewModelScope.launch {
                                    _messages.value = _messages.value + newMsg
                                    _uiState.value = ChatUiState.Success(_messages.value)
                                }
                            }
                        } catch (e: JsonSyntaxException) {
                            Log.e(LOG_TAG, "Invalid JSON format", e)
                        } catch (e: JsonParseException) {
                            Log.e(LOG_TAG, "JSON parsing error", e)
                        }
                    }
                },
            )
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val payload = gson.toJson(mapOf("missatge" to text))
        Log.d(LOG_TAG, "Enviant: $payload")
        webSocket?.send(payload)
    }

    fun deleteMessage(messageId: Int) {
        viewModelScope.launch {
            try {
                RetrofitProvider.chatApi.deleteMessage(messageId)
                _messages.value = _messages.value.filter { it.id != messageId }
                if (_messages.value.isEmpty()) _uiState.value = ChatUiState.Empty
            } catch (e: HttpException) {
                Log.e(LOG_TAG, "HTTP error deleting message: ${e.code()}", e)
            } catch (e: IOException) {
                Log.e(LOG_TAG, "Network error deleting message: ${e.message}", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        webSocket?.close(SOCKET_RESPONSE, "ViewModel cleared")
    }
}
