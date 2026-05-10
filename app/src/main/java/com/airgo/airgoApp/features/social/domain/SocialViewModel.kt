package com.airgo.airgoApp.features.social.domain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airgo.airgoApp.R
import com.airgo.airgoApp.core.network.RetrofitProvider
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.core.utils.UiText
import com.airgo.airgoApp.features.social.SocialRepository
import com.airgo.airgoApp.features.social.data.UserSearchResult
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
import kotlin.collections.filter

enum class SocialTab { FRIENDS, SEARCH, REQUESTS }

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
        val users: List<UserSearchResult>,
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

private const val SEARCH_DEBOUNCE_MS = 400L
private const val MIN_QUERY_LENGTH = 2

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

    // Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _searchUiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val searchUiState: StateFlow<SearchUiState> = _searchUiState

    private val _requestSentTo = MutableStateFlow<Set<Int>>(emptySet())
    val requestSentTo: StateFlow<Set<Int>> = _requestSentTo

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
        observeQueries()
    }

    fun onTabChange(tab: SocialTab) {
        _activeTab.value = tab
    }

    // Friends
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

    private fun observeQueries() {
        viewModelScope.launch {
            _friendsQuery
                .debounce(SEARCH_DEBOUNCE_MS)
                .distinctUntilChanged()
                .collect { query -> loadFriends(search = query.takeIf { it.isNotBlank() }) }
        }
        viewModelScope.launch {
            _searchQuery
                .debounce(SEARCH_DEBOUNCE_MS)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.length >= MIN_QUERY_LENGTH) {
                        performSearch(query)
                    } else if (query.isBlank()) {
                        _searchUiState.value = SearchUiState.Idle
                    }
                }
        }
    }

    fun onFriendsQueryChange(query: String) {
        _friendsQuery.value = query
    }

    fun clearFriendsQuery() {
        _friendsQuery.value = ""
    }

    // Search users
    private suspend fun performSearch(query: String) {
        _searchUiState.value = SearchUiState.Loading
        repository.searchUsers(query).fold(
            onSuccess = { users ->
                val currentUserId = SessionManager.userId
                val friendIds =
                    (_friendsUiState.value as? FriendsUiState.Success)
                        ?.friends
                        ?.map { it.amic.id }
                        ?.toSet()
                        ?: emptySet()

                val filtered =
                    users.filter { user ->
                        user.id != currentUserId && user.id !in friendIds
                    }

                _searchUiState.value =
                    if (filtered.isEmpty()) {
                        SearchUiState.Empty
                    } else {
                        SearchUiState.Success(filtered)
                    }
            },
            onFailure = { e ->
                _searchUiState.value = SearchUiState.Error(e.message)
            },
        )
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) _searchUiState.value = SearchUiState.Idle
    }

    fun clearSearchQuery() {
        _searchQuery.value = ""
        _searchUiState.value = SearchUiState.Idle
    }

    fun sendFriendRequest(userId: Int) {
        viewModelScope.launch {
            repository.sendFriendRequest(userId).fold(
                onSuccess = {
                    _requestSentTo.value += userId
                    _requestFeedback.emit(UiText.StringResource(R.string.request_sent))
                },
                onFailure = {
                    _requestFeedback.emit(UiText.StringResource(R.string.error_request))
                },
            )
        }
    }

    // Friend requests

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
                onFailure = { },
            )
        }
    }
}
