package com.airgo.airgoApp.core

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object AppEvents {
    private val _activitySaved = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val activitySaved = _activitySaved.asSharedFlow()

    fun notifyActivitySaved() {
        _activitySaved.tryEmit(Unit)
    }
}
