package com.airgo.airgoApp.core.events

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object ChatEventBus {
    private val _messageReceived = MutableSharedFlow<String>(replay = 0)
    val messageReceived = _messageReceived.asSharedFlow()

    suspend fun emitMessage(chatId: String) {
        _messageReceived.emit(chatId)
    }
}
