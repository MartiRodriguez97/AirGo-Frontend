package com.airgo.airgoApp.features.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airgo.airgoApp.R
import com.airgo.airgoApp.features.social.components.emptyState
import com.airgo.airgoApp.features.social.components.errorState
import com.airgo.airgoApp.features.social.components.loadingState
import com.airgo.airgoApp.features.social.components.socialSearchBar
import com.airgo.airgoApp.features.social.forums.components.createForumDialog
import com.airgo.airgoApp.features.social.forums.components.createForumFab
import com.airgo.airgoApp.features.social.forums.components.deleteForumDialog
import com.airgo.airgoApp.features.social.forums.components.forumCard
import com.airgo.airgoApp.features.social.forums.data.Forum
import com.airgo.airgoApp.features.social.forums.domain.ForumUiState
import com.airgo.airgoApp.features.social.forums.domain.ForumViewModel

private enum class ForumFilter { FAVORITS, MEUS }

@Composable
internal fun forumsTab(
    onForumClick: (Int, String) -> Unit = { _, _ -> },
    forumViewModel: ForumViewModel = viewModel(),
) {
    val uiState by forumViewModel.forumsUiState.collectAsStateWithLifecycle()
    val favoritForums by forumViewModel.favoritForums.collectAsStateWithLifecycle()
    val searchQuery by forumViewModel.searchQuery.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var forumToDelete by remember { mutableStateOf<Forum?>(null) }
    var activeFilter by remember { mutableStateOf(ForumFilter.FAVORITS) }

    forumDialogs(
        showCreateDialog = showCreateDialog,
        forumToDelete = forumToDelete,
        onDismissCreate = { showCreateDialog = false },
        onCreate = { nom, descripcio ->
            forumViewModel.createForum(nom, descripcio)
            showCreateDialog = false
        },
        onDismissDelete = { forumToDelete = null },
        onConfirmDelete = {
            forumViewModel.deleteForum(it)
            forumToDelete = null
        },
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            forumsSearchBar(query = searchQuery, forumViewModel = forumViewModel)
            HorizontalDivider()
            forumsFilterRow(activeFilter = activeFilter, onFilterChange = { activeFilter = it })
            forumsContent(
                uiState = uiState,
                favoritForums = favoritForums,
                activeFilter = activeFilter,
                forumViewModel = forumViewModel,
                onDeleteRequest = { forumToDelete = it },
                onForumClick = { forumId, forumName -> onForumClick(forumId, forumName) },
            )
        }
        createForumFab(
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            onClick = { showCreateDialog = true },
        )
    }
}

@Composable
private fun forumDialogs(
    showCreateDialog: Boolean,
    forumToDelete: Forum?,
    onDismissCreate: () -> Unit,
    onCreate: (String, String) -> Unit,
    onDismissDelete: () -> Unit,
    onConfirmDelete: (Int) -> Unit,
) {
    if (showCreateDialog) {
        createForumDialog(onDismiss = onDismissCreate, onCreate = onCreate)
    }
    forumToDelete?.let { forum ->
        deleteForumDialog(
            forumNom = forum.nom ?: "",
            onDismiss = onDismissDelete,
            onConfirm = { onConfirmDelete(forum.id) },
        )
    }
}

@Composable
private fun forumsSearchBar(
    query: String,
    forumViewModel: ForumViewModel,
) {
    val focusManager = LocalFocusManager.current
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        socialSearchBar(
            query = query,
            placeholder = stringResource(R.string.search_forum),
            onQueryChange = forumViewModel::onSearchQueryChange,
            onClear = forumViewModel::clearSearchQuery,
            onSearch = { focusManager.clearFocus() },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun forumsFilterRow(
    activeFilter: ForumFilter,
    onFilterChange: (ForumFilter) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = activeFilter == ForumFilter.FAVORITS,
            onClick = { onFilterChange(ForumFilter.FAVORITS) },
            label = { Text(stringResource(R.string.forums_filter_saved)) },
            leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp)) },
        )
        FilterChip(
            selected = activeFilter == ForumFilter.MEUS,
            onClick = { onFilterChange(ForumFilter.MEUS) },
            label = { Text(stringResource(R.string.forums_filter_mine)) },
        )
    }
}

@Composable
private fun forumsContent(
    uiState: ForumUiState,
    favoritForums: Set<Int>,
    activeFilter: ForumFilter,
    forumViewModel: ForumViewModel,
    onDeleteRequest: (Forum) -> Unit,
    onForumClick: (Int, String) -> Unit,
) {
    when (val state = uiState) {
        is ForumUiState.Loading -> loadingState()
        is ForumUiState.Empty ->
            emptyState("emptyForum", text = stringResource(R.string.no_favorite_created_forums_found))
        is ForumUiState.Error -> errorState(state.message)
        is ForumUiState.Success -> {
            val filteredForums =
                when (activeFilter) {
                    ForumFilter.FAVORITS -> state.forums.filter { it.id in favoritForums }
                    ForumFilter.MEUS -> state.forums.filter { forumViewModel.isCreatedByCurrentUser(it) }
                }
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp, start = 0.dp, end = 0.dp),
            ) {
                items(filteredForums, key = { it.id }) { forum ->
                    forumCard(
                        forum = forum,
                        isFavorit = forum.id in favoritForums,
                        isOwner = forumViewModel.isCreatedByCurrentUser(forum),
                        onToggleFavorit = { forumViewModel.toggleFavorit(forum.id) },
                        onDelete = { onDeleteRequest(forum) },
                        onClick = { onForumClick(forum.id, forum.nom ?: "") },
                    )
                }
            }
        }
    }
}
