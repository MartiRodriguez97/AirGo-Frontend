package com.airgo.airgoApp.features.social.friends.domain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airgo.airgoApp.core.network.RetrofitProvider
import com.airgo.airgoApp.features.social.SocialRepository
import com.airgo.airgoApp.features.social.domain.RequestsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.collections.filter

class FriendRequestsViewModel : ViewModel() {
    private val repository = SocialRepository(RetrofitProvider.socialApi)

    private val _uiState = MutableStateFlow<RequestsUiState>(RequestsUiState.Loading)
    val uiState: StateFlow<RequestsUiState> = _uiState

    private val _showRequests = MutableStateFlow(false)
    val showRequests: StateFlow<Boolean> = _showRequests

    val pendingCount: StateFlow<Int> =
        _uiState
            .map { if (it is RequestsUiState.Success) it.requests.size else 0 }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)

    private  val pollInterval = 30_000L

    init {
        loadRequests()
        startPolling()
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(pollInterval)
                loadRequests()
            }
        }
    }

    fun loadRequests() {
        viewModelScope.launch {
            _uiState.value = RequestsUiState.Loading
            repository.getFriendRequests().fold(
                onSuccess = { requests ->
                    _uiState.value =
                        if (requests.isEmpty()) {
                            RequestsUiState.Empty
                        } else {
                            RequestsUiState.Success(requests)
                        }
                },
                onFailure = { e -> _uiState.value = RequestsUiState.Error(e.message) },
            )
        }
    }

    fun toggleRequests() {
        _showRequests.value = !_showRequests.value
    }

    fun respondRequest(
        amistatId: Int,
        accept: Boolean,
        onAccepted: () -> Unit,
    ) {
        viewModelScope.launch {
            repository.respondFriendRequest(amistatId, if (accept) "accept" else "reject").fold(
                onSuccess = {
                    val current = _uiState.value
                    if (current is RequestsUiState.Success) {
                        val updated = current.requests.filter { it.id != amistatId }
                        _uiState.value = if (updated.isEmpty()) RequestsUiState.Empty
                        else RequestsUiState.Success(updated)
                    }
                    if (accept) onAccepted()
                    loadRequests()
                },
                onFailure = { },
            )
        }
    }
}
