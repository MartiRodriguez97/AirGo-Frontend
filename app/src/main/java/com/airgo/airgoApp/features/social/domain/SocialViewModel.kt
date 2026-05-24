package com.airgo.airgoApp.features.social.domain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airgo.airgoApp.R
import com.airgo.airgoApp.core.network.RetrofitProvider
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.core.utils.UiText
import com.airgo.airgoApp.features.social.SocialRepository
import com.airgo.airgoApp.features.social.forums.domain.DiscoverSearchResult
import com.airgo.airgoApp.features.social.friends.data.FriendRequestResponse
import com.airgo.airgoApp.features.social.friends.data.FriendResponse
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SocialTab { FRIENDS, SEARCH, REQUESTS, FORUMS }

sealed class FriendsUiState {
    object Loading : FriendsUiState()

    object Empty : FriendsUiState()

    data class Success(
        val friends: List<FriendResponse>,
    ) : FriendsUiState()

    data class Error(
        val message: String?,
    ) : FriendsUiState()
}

sealed class SearchUiState {
    object Idle : SearchUiState()

    object Loading : SearchUiState()

    object Empty : SearchUiState()

    data class Success(
        val results: List<DiscoverSearchResult>,
    ) : SearchUiState()

    data class Error(
        val message: String?,
    ) : SearchUiState()
}

sealed class RequestsUiState {
    object Loading : RequestsUiState()

    object Empty : RequestsUiState()

    data class Success(
        val requests: List<FriendRequestResponse>,
    ) : RequestsUiState()

    data class Error(
        val message: String?,
    ) : RequestsUiState()
}

private const val FRIENDS_DEBOUNCE_MS = 400L

@OptIn(FlowPreview::class)
class SocialViewModel : ViewModel() {
    private val repository = SocialRepository(RetrofitProvider.socialApi)

    private val _activeTab = MutableStateFlow(SocialTab.FRIENDS)
    val activeTab: StateFlow<SocialTab> = _activeTab

    // Friends
    private val _friendsQuery = MutableStateFlow("")
    val friendsQuery: StateFlow<String> = _friendsQuery

    private val _friendsUiState = MutableStateFlow<FriendsUiState>(FriendsUiState.Loading)
    val friendsUiState: StateFlow<FriendsUiState> = _friendsUiState

    // Search — delegat a UserSearchHandler
    private val searchHandler =
        UserSearchHandler(
            repository = repository,
            scope = viewModelScope,
            getFriendIds = {
                (_friendsUiState.value as? FriendsUiState.Success)
                    ?.friends
                    ?.map { it.amic.id }
                    ?.toSet() ?: emptySet()
            },
        )
    val searchQuery: StateFlow<String> = searchHandler.searchQuery
    val searchUiState: StateFlow<SearchUiState> = searchHandler.searchUiState
    val requestSentTo: StateFlow<Set<Int>> = searchHandler.requestSentTo

    // Requests
    private val _requestsUiState = MutableStateFlow<RequestsUiState>(RequestsUiState.Loading)
    val requestsUiState: StateFlow<RequestsUiState> = _requestsUiState

    private val _respondedTo = MutableStateFlow<Set<Int>>(emptySet())
    val respondedTo: StateFlow<Set<Int>> = _respondedTo

    private val _requestFeedback = MutableSharedFlow<UiText>(extraBufferCapacity = 1)
    val requestFeedback = _requestFeedback.asSharedFlow()

    private val _showRequests = MutableStateFlow(false)
    val showRequests: StateFlow<Boolean> = _showRequests

    val pendingRequestsCount: StateFlow<Int> =
        _requestsUiState
            .map { state -> if (state is RequestsUiState.Success) state.requests.size else 0 }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)

    init {
        loadFriends()
        observeFriendsQuery()
    }

    fun onTabChange(tab: SocialTab) {
        _activeTab.value = tab
    }

    internal fun loadFriends(search: String? = null) {
        viewModelScope.launch {
            _friendsUiState.value = FriendsUiState.Loading
            repository.getFriends(search).fold(
                onSuccess = { friends ->
                    _friendsUiState.value =
                        if (friends.isEmpty()) {
                            FriendsUiState.Empty
                        } else {
                            FriendsUiState.Success(friends)
                        }
                },
                onFailure = { e -> _friendsUiState.value = FriendsUiState.Error(e.message) },
            )
        }
    }

    private fun observeFriendsQuery() {
        viewModelScope.launch {
            _friendsQuery
                .debounce(FRIENDS_DEBOUNCE_MS)
                .distinctUntilChanged()
                .collect { query -> loadFriends(search = query.takeIf { it.isNotBlank() }) }
        }
    }

    fun onFriendsQueryChange(query: String) {
        _friendsQuery.value = query
    }

    fun clearFriendsQuery() {
        _friendsQuery.value = ""
    }

    fun onSearchQueryChange(query: String) {
        searchHandler.onQueryChange(query)
    }

    fun clearSearchQuery() {
        searchHandler.clearQuery()
    }

    fun sendFriendRequest(userId: Int) {
        viewModelScope.launch {
            repository.sendFriendRequest(userId).fold(
                onSuccess = {
                    searchHandler.markRequestSent(userId)
                    _requestFeedback.emit(UiText.StringResource(R.string.request_sent))
                },
                onFailure = {
                    _requestFeedback.emit(UiText.StringResource(R.string.error_request))
                },
            )
        }
    }

    fun deleteFriend(amicId: Int) {
        viewModelScope.launch {
            repository.deleteFriend(amicId).fold(
                onSuccess = {
                    val current = _friendsUiState.value
                    if (current is FriendsUiState.Success) {
                        val updated = current.friends.filter { it.amic.id != amicId }
                        _friendsUiState.value =
                            if (updated.isEmpty()) {
                                FriendsUiState.Empty
                            } else {
                                FriendsUiState.Success(updated)
                            }
                    }
                },
                onFailure = {},
            )
        }
    }

    fun getConversationId(
        amicId: Int,
        onResult: (convId: String, otherUserId: Int, username: String) -> Unit,
    ) {
        val friend =
            (_friendsUiState.value as? FriendsUiState.Success)
                ?.friends
                ?.find { it.amic.id == amicId } ?: return
        val currentUserId = SessionManager.userId ?: return
        val convId = if (currentUserId < amicId) "$currentUserId-$amicId" else "$amicId-$currentUserId"
        onResult(convId, amicId, friend.amic.username)
    }
}
