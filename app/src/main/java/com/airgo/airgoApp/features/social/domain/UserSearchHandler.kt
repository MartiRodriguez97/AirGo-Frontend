package com.airgo.airgoApp.features.social.domain

import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.features.social.SocialRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private const val SEARCH_DEBOUNCE_MS = 400L
private const val MIN_QUERY_LENGTH = 2

/**
 * Gestiona la lògica de cerca d'usuaris.
 * S'usa des del SocialViewModel per separar responsabilitats.
 */
@OptIn(FlowPreview::class)
class UserSearchHandler(
    private val repository: SocialRepository,
    private val scope: CoroutineScope,
    private val getFriendIds: () -> Set<Int>,
) {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _searchUiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val searchUiState: StateFlow<SearchUiState> = _searchUiState

    private val _requestSentTo = MutableStateFlow<Set<Int>>(emptySet())
    val requestSentTo: StateFlow<Set<Int>> = _requestSentTo

    init {
        observeQuery()
    }

    private fun observeQuery() {
        scope.launch {
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

    private suspend fun performSearch(query: String) {
        _searchUiState.value = SearchUiState.Loading
        repository.searchUsers(query).fold(
            onSuccess = { users ->
                val currentUserId = SessionManager.userId
                val friendIds = getFriendIds()
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

    fun onQueryChange(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) _searchUiState.value = SearchUiState.Idle
    }

    fun clearQuery() {
        _searchQuery.value = ""
        _searchUiState.value = SearchUiState.Idle
    }

    fun markRequestSent(userId: Int) {
        _requestSentTo.value += userId
    }
}
