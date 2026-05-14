package com.airgo.airgoApp.features.social.forums.domain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.features.social.forums.data.Forum
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private const val SEARCH_DEBOUNCE_MS = 400L
private const val MIN_QUERY_LENGTH = 2

sealed class ForumUiState {
    data object Loading : ForumUiState()

    data object Empty : ForumUiState()

    data class Success(
        val forums: List<Forum>,
    ) : ForumUiState()

    data class Error(
        val message: String?,
    ) : ForumUiState()
}

sealed class ForumActionState {
    data object Idle : ForumActionState()

    data object Loading : ForumActionState()

    data object Success : ForumActionState()

    data class Error(
        val message: String?,
    ) : ForumActionState()
}

@OptIn(FlowPreview::class)
class ForumViewModel : ViewModel() {
    private val _forumsUiState = MutableStateFlow<ForumUiState>(ForumUiState.Loading)
    val forumsUiState: StateFlow<ForumUiState> = _forumsUiState

    private val _favoritForums = MutableStateFlow<Set<Int>>(emptySet())
    val favoritForums: StateFlow<Set<Int>> = _favoritForums

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _actionState = MutableStateFlow<ForumActionState>(ForumActionState.Idle)
    val actionState: StateFlow<ForumActionState> = _actionState

    // Llista local de fòrums mentre no hi ha backend
    private val mockForums = mutableListOf<Forum>()
    private var nextId = 1

    init {
        loadForums()
        observeSearchQuery()
    }

    fun loadForums(search: String? = null) {
        viewModelScope.launch {
            _forumsUiState.value = ForumUiState.Loading
            val filtered =
                if (search.isNullOrBlank()) {
                    mockForums.toList()
                } else {
                    mockForums.filter {
                        it.nom.contains(search, ignoreCase = true) ||
                            it.descripcio.contains(search, ignoreCase = true)
                    }
                }
            _forumsUiState.value =
                if (filtered.isEmpty()) {
                    ForumUiState.Empty
                } else {
                    ForumUiState.Success(filtered)
                }
        }
    }

    private fun observeSearchQuery() {
        viewModelScope.launch {
            _searchQuery
                .debounce(SEARCH_DEBOUNCE_MS)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.length >= MIN_QUERY_LENGTH) {
                        loadForums(query)
                    } else if (query.isBlank()) {
                        loadForums()
                    }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun clearSearchQuery() {
        _searchQuery.value = ""
        loadForums()
    }

    fun createForum(
        nom: String,
        descripcio: String,
    ) {
        if (nom.isBlank()) {
            _actionState.value = ForumActionState.Error("El nom no pot estar buit")
            return
        }
        viewModelScope.launch {
            _actionState.value = ForumActionState.Loading
            val newForum =
                Forum(
                    id = nextId++,
                    nom = nom,
                    descripcio = descripcio,
                    creatPer = SessionManager.userId ?: -1,
                )
            mockForums.add(newForum)
            _actionState.value = ForumActionState.Success
            loadForums()
        }
    }

    fun deleteForum(forumId: Int) {
        viewModelScope.launch {
            mockForums.removeAll { it.id == forumId }
            _favoritForums.value -= forumId
            loadForums()
        }
    }

    fun toggleFavorit(forumId: Int) {
        _favoritForums.value =
            if (forumId in _favoritForums.value) {
                _favoritForums.value - forumId
            } else {
                _favoritForums.value + forumId
            }
    }

    fun clearActionState() {
        _actionState.value = ForumActionState.Idle
    }

    fun isCreatedByCurrentUser(forum: Forum): Boolean = forum.creatPer == SessionManager.userId
}
