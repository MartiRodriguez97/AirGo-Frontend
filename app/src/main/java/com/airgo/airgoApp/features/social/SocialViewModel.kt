package com.airgo.airgoApp.features.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airgo.airgoApp.core.network.RetrofitProvider
import com.airgo.airgoApp.features.social.SocialRepository
import com.airgo.airgoApp.features.social.UserSearchResult
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

sealed class SearchUiState {
    object Idle : SearchUiState()

    object Loading : SearchUiState()

    data class Success(
        val users: List<UserSearchResult>,
    ) : SearchUiState()

    object Empty : SearchUiState()

    data class Error(
        val message: String,
    ) : SearchUiState()
}

private const val SEARCH_DEBOUNCE_MS = 400L
private const val MIN_QUERY_LENGTH = 2

@OptIn(FlowPreview::class)
class SocialViewModel : ViewModel() {
    private val repository = SocialRepository(RetrofitProvider.socialApi)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState

    init {
        viewModelScope.launch {
            _query
                .debounce(SEARCH_DEBOUNCE_MS)
                .distinctUntilChanged()
                .filter { it.length >= MIN_QUERY_LENGTH }
                .collect { query -> performSearch(query) }
        }
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        if (newQuery.isBlank()) {
            _uiState.value = SearchUiState.Idle
        }
    }

    fun clearQuery() {
        _query.value = ""
        _uiState.value = SearchUiState.Idle
    }

    private suspend fun performSearch(query: String) {
        _uiState.value = SearchUiState.Loading
        repository.searchUsers(query).fold(
            onSuccess = { users ->
                _uiState.value =
                    if (users.isEmpty()) {
                        SearchUiState.Empty
                    } else {
                        SearchUiState.Success(users)
                    }
            },
            onFailure = { e ->
                _uiState.value = SearchUiState.Error(e.message ?: "Error desconegut")
            },
        )
    }
}
