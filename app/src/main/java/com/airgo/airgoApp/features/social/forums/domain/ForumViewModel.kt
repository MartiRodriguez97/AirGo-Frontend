package com.airgo.airgoApp.features.social.forums.domain

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airgo.airgoApp.core.network.RetrofitProvider
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.features.social.forums.data.AddFavoritForumRequest
import com.airgo.airgoApp.features.social.forums.data.CreateForumRequest
import com.airgo.airgoApp.features.social.forums.data.Forum
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

private const val LOG_TAG = "ForumViewModel"
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

    // Llista de tots els fòrums carregats (favorits + creats per l'usuari)
    private var allMyForums: List<Forum> = emptyList()

    init {
        loadMyForums()
        observeSearchQuery()
    }

    /**
     * Carrega els fòrums rellevants per a l'usuari:
     * favorits + creats per ell.
     */
    fun loadMyForums() {
        viewModelScope.launch {
            _forumsUiState.value = ForumUiState.Loading
            try {
                val currentUserId = SessionManager.userId
                val favoritResponses = RetrofitProvider.forumApi.getFavoritForums()
                val favorits = favoritResponses.map { it.forum }
                android.util.Log.d("ForumViewModel", "Favorits raw: $favorits")
                favorits.forEach {
                    android.util.Log.d("ForumViewModel", "Favorit: id=${it.id}, nom=${it.nom}, creatPer=${it.creatPer}")
                }
                val allForums = RetrofitProvider.forumApi.getForums()
                val createdByUser = allForums.filter { it.creatPer == currentUserId }

                // Combinar sense duplicats
                val combined = (favorits + createdByUser).distinctBy { it.id }
                allMyForums = combined
                _favoritForums.value = favorits.map { it.id }.toSet()

                _forumsUiState.value =
                    if (combined.isEmpty()) {
                        ForumUiState.Empty
                    } else {
                        ForumUiState.Success(combined)
                    }
            } catch (e: IOException) {
                _forumsUiState.value = ForumUiState.Error("Error de connexió")
                Log.e(LOG_TAG, "Network failure: ${e.message}", e)
            } catch (e: HttpException) {
                _forumsUiState.value = ForumUiState.Error("Error del servidor: ${e.code()}")
                Log.e(LOG_TAG, "HTTP failure: ${e.message()}", e)
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
                        filterMyForums(query)
                    } else if (query.isBlank()) {
                        _forumsUiState.value =
                            if (allMyForums.isEmpty()) {
                                ForumUiState.Empty
                            } else {
                                ForumUiState.Success(allMyForums)
                            }
                    }
                }
        }
    }

    private fun filterMyForums(query: String) {
        val filtered =
            allMyForums.filter {
                it.nom?.contains(query, ignoreCase = true) == true ||
                    it.descripcio?.contains(query, ignoreCase = true) == true
            }
        _forumsUiState.value =
            if (filtered.isEmpty()) {
                ForumUiState.Empty
            } else {
                ForumUiState.Success(filtered)
            }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun clearSearchQuery() {
        _searchQuery.value = ""
        _forumsUiState.value =
            if (allMyForums.isEmpty()) {
                ForumUiState.Empty
            } else {
                ForumUiState.Success(allMyForums)
            }
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
            try {
                RetrofitProvider.forumApi.createForum(CreateForumRequest(nom, descripcio))
                _actionState.value = ForumActionState.Success
                loadMyForums()
            } catch (e: IOException) {
                _actionState.value = ForumActionState.Error("Error de connexió")
                Log.e(LOG_TAG, "Network failure: ${e.message}", e)
            } catch (e: HttpException) {
                _actionState.value = ForumActionState.Error("Error del servidor: ${e.code()}")
                Log.e(LOG_TAG, "HTTP failure: ${e.message()}", e)
            }
        }
    }

    fun deleteForum(forumId: Int) {
        viewModelScope.launch {
            try {
                RetrofitProvider.forumApi.deleteForum(forumId)
                _favoritForums.value -= forumId
                loadMyForums()
            } catch (e: IOException) {
                Log.e(LOG_TAG, "Network failure: ${e.message}", e)
            } catch (e: HttpException) {
                Log.e(LOG_TAG, "HTTP failure: ${e.message()}", e)
            }
        }
    }

    fun toggleFavorit(forumId: Int) {
        viewModelScope.launch {
            try {
                if (forumId in _favoritForums.value) {
                    RetrofitProvider.forumApi.removeFavoritForum(forumId)
                    _favoritForums.value -= forumId
                } else {
                    RetrofitProvider.forumApi.addFavoritForum(AddFavoritForumRequest(forumId))
                    _favoritForums.value += forumId
                }
                loadMyForums()
            } catch (e: IOException) {
                Log.e(LOG_TAG, "Network failure toggle favorit: ${e.message}", e)
            } catch (e: HttpException) {
                Log.e(LOG_TAG, "HTTP failure toggle favorit: ${e.message()}", e)
            }
        }
    }

    fun clearActionState() {
        _actionState.value = ForumActionState.Idle
    }

    fun isCreatedByCurrentUser(forum: Forum): Boolean = forum.creatPer == SessionManager.userId
}
