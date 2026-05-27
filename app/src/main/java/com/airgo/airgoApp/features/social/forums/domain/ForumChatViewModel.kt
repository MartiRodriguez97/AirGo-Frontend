package com.airgo.airgoApp.features.social.forums.domain

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airgo.airgoApp.BuildConfig
import com.airgo.airgoApp.core.network.RetrofitProvider
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.features.social.forums.data.ForumMessage
import com.airgo.airgoApp.features.social.forums.data.ForumWebSocketMessage
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

private const val LOG_TAG = "ForumChatViewModel"
private const val WS_BASE_URL = BuildConfig.BACKEND_URL
private const val SOCKET_CLOSE_NORMAL = 1000

sealed class ForumChatUiState {
    data object Loading : ForumChatUiState()

    data object Empty : ForumChatUiState()

    data class Success(
        val messages: List<ForumMessage>,
    ) : ForumChatUiState()

    data class Error(
        val message: String?,
    ) : ForumChatUiState()
}

class ForumChatViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<ForumChatUiState>(ForumChatUiState.Loading)
    val uiState: StateFlow<ForumChatUiState> = _uiState

    private val _messages = MutableStateFlow<List<ForumMessage>>(emptyList())
    val messages: StateFlow<List<ForumMessage>> = _messages

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private var webSocket: WebSocket? = null
    private val gson = Gson()

    fun init(forumId: Int) {
        loadMessages(forumId)
        connectWebSocket(forumId)
    }

    private fun loadMessages(forumId: Int) {
        viewModelScope.launch {
            _uiState.value = ForumChatUiState.Loading
            try {
                val msgs = RetrofitProvider.forumChatApi.getMessages(forumId)
                _messages.value = msgs
                _uiState.value =
                    if (msgs.isEmpty()) {
                        ForumChatUiState.Empty
                    } else {
                        ForumChatUiState.Success(msgs)
                    }
            } catch (e: HttpException) {
                _uiState.value = ForumChatUiState.Error("Error del servidor: ${e.code()}")
                Log.e(LOG_TAG, "HTTP failure: ${e.message()}", e)
            } catch (e: IOException) {
                _uiState.value = ForumChatUiState.Error("Error de connexió")
                Log.e(LOG_TAG, "Network failure: ${e.message}", e)
            }
        }
    }

    private fun connectWebSocket(forumId: Int) {
        val token = SessionManager.accessToken ?: return
        val wsUrl =
            WS_BASE_URL
                .replace("http://", "ws://")
                .replace("https://", "wss://")
                .trimEnd('/') + "/ws/forum/$forumId/?token=$token"

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
                            val wsMsg = gson.fromJson(json, ForumWebSocketMessage::class.java)
                            if (wsMsg.contingut != null) {
                                val newMsg =
                                    ForumMessage(
                                        id = wsMsg.id ?: 0,
                                        emissor = wsMsg.emissor ?: 0,
                                        emissorUsername = wsMsg.emissorUsername ?: "",
                                        contingut = wsMsg.contingut,
                                        enviatAt = wsMsg.enviatAt ?: "",
                                    )
                                viewModelScope.launch {
                                    if (_messages.value.none { it.id == newMsg.id }) {
                                        _messages.value = _messages.value + newMsg
                                        _uiState.value = ForumChatUiState.Success(_messages.value)
                                    }
                                }
                            }
                        } catch (e: JsonSyntaxException) {
                            Log.e(LOG_TAG, "Invalid JSON: ${e.message}", e)
                        } catch (e: JsonParseException) {
                            Log.e(LOG_TAG, "JSON parse error: ${e.message}", e)
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

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val payload = gson.toJson(mapOf("missatge" to text))
        webSocket?.send(payload)
    }

    override fun onCleared() {
        super.onCleared()
        webSocket?.close(SOCKET_CLOSE_NORMAL, "ViewModel cleared")
    }
}
