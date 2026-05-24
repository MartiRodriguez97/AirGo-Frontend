package com.airgo.airgoApp.features.social.domain

import com.airgo.airgoApp.core.network.RetrofitProvider
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.features.social.SocialRepository
import com.airgo.airgoApp.features.social.forums.domain.DiscoverSearchResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

private const val SEARCH_DEBOUNCE_MS = 400L
private const val MIN_QUERY_LENGTH = 2

/**
 * Gestiona la cerca combinada d'usuaris i fòrums.
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

    private fun performSearch(query: String) {
        scope.launch {
            _searchUiState.value = SearchUiState.Loading
            val currentUserId = SessionManager.userId
            val friendIds = getFriendIds()

            val usersDeferred =
                async {
                    try {
                        repository.searchUsers(query).getOrNull() ?: emptyList()
                    } catch (e: IOException) {
                        android.util.Log.e("UserSearchHandler", "Network error searching users: ${e.message}", e)
                        emptyList()
                    } catch (e: HttpException) {
                        android.util.Log.e("UserSearchHandler", "HTTP error searching users: ${e.code()}", e)
                        emptyList()
                    }
                }
            val forumsDeferred =
                async {
                    try {
                        RetrofitProvider.forumApi.getForums(query)
                    } catch (e: IOException) {
                        android.util.Log.e("ForumSearchHandler", "Network error searching forums: ${e.message}", e)
                        emptyList()
                    } catch (e: HttpException) {
                        android.util.Log.e("ForumSearchHandler", "HTTP error searching forums: ${e.code()}", e)
                        emptyList()
                    }
                }

            val users = usersDeferred.await()
            val forums = forumsDeferred.await()

            val filteredUsers =
                users
                    .filter { user -> user.id != currentUserId && user.id !in friendIds }
                    .map { DiscoverSearchResult.UserResult(it) }

            val filteredForums = forums.map { DiscoverSearchResult.ForumResult(it) }

            val combined = filteredUsers + filteredForums
            _searchUiState.value =
                if (combined.isEmpty()) {
                    SearchUiState.Empty
                } else {
                    SearchUiState.Success(combined)
                }
        }
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
