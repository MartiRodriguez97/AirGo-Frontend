package com.airgo.airgoApp.core.events

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object FriendEventBus {
    private val _friendAccepted = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val friendAccepted = _friendAccepted.asSharedFlow()

    fun emitFriendAccepted() {
        _friendAccepted.tryEmit(Unit)
    }
}
