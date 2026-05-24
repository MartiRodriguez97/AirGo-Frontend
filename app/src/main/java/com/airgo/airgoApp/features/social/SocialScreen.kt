package com.airgo.airgoApp.features.social

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.airgo.airgoApp.R
import com.airgo.airgoApp.features.social.components.emptyState
import com.airgo.airgoApp.features.social.components.errorState
import com.airgo.airgoApp.features.social.components.idleSearchState
import com.airgo.airgoApp.features.social.components.loadingState
import com.airgo.airgoApp.features.social.components.socialSearchBar
import com.airgo.airgoApp.features.social.components.userAvatarPlaceholder
import com.airgo.airgoApp.features.social.data.UserSearchResult
import com.airgo.airgoApp.features.social.domain.FriendsUiState
import com.airgo.airgoApp.features.social.domain.RequestsUiState
import com.airgo.airgoApp.features.social.domain.SearchUiState
import com.airgo.airgoApp.features.social.domain.SocialTab
import com.airgo.airgoApp.features.social.domain.SocialViewModel
import com.airgo.airgoApp.features.social.forums.components.createForumDialog
import com.airgo.airgoApp.features.social.forums.components.createForumFab
import com.airgo.airgoApp.features.social.forums.components.deleteForumDialog
import com.airgo.airgoApp.features.social.forums.components.forumCard
import com.airgo.airgoApp.features.social.forums.data.Forum
import com.airgo.airgoApp.features.social.forums.domain.DiscoverSearchResult
import com.airgo.airgoApp.features.social.forums.domain.ForumUiState
import com.airgo.airgoApp.features.social.forums.domain.ForumViewModel
import com.airgo.airgoApp.features.social.friends.data.FriendRequestResponse
import com.airgo.airgoApp.features.social.friends.data.FriendResponse
import com.airgo.airgoApp.features.social.friends.domain.FriendRequestsViewModel

private enum class ForumFilter { FAVORITS, MEUS }

@Composable
fun socialScreen(
    onFriendClick: (Int) -> Unit = {},
    onChatClick: (convId: String, otherUserId: Int, username: String) -> Unit = { _, _, _ -> },
    viewModel: SocialViewModel = viewModel(),
) {
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        val visibleTabs = listOf(SocialTab.FRIENDS, SocialTab.SEARCH, SocialTab.FORUMS)

        TabRow(selectedTabIndex = visibleTabs.indexOf(activeTab).coerceAtLeast(0)) {
            Tab(
                selected = activeTab == SocialTab.FRIENDS,
                onClick = { viewModel.onTabChange(SocialTab.FRIENDS) },
                text = { Text(stringResource(R.string.friends)) },
            )
            Tab(
                selected = activeTab == SocialTab.SEARCH,
                onClick = { viewModel.onTabChange(SocialTab.SEARCH) },
                text = { Text(stringResource(R.string.discover)) },
            )
            Tab(
                selected = activeTab == SocialTab.FORUMS,
                onClick = { viewModel.onTabChange(SocialTab.FORUMS) },
                text = { Text(stringResource(R.string.forums)) },
            )
        }

        when (activeTab) {
            SocialTab.FRIENDS ->
                friendsTab(
                    viewModel = viewModel,
                    onFriendClick = onFriendClick,
                    onChatClick = onChatClick,
                )
            SocialTab.SEARCH -> searchTab(viewModel = viewModel)
            SocialTab.REQUESTS -> Unit
            SocialTab.FORUMS -> forumsTab()
        }
    }
}

@Composable
private fun friendsTab(
    viewModel: SocialViewModel,
    requestsViewModel: FriendRequestsViewModel = viewModel(),
    onFriendClick: (Int) -> Unit,
    onChatClick: (convId: String, otherUserId: Int, username: String) -> Unit = { _, _, _ -> },
) {
    val query by viewModel.friendsQuery.collectAsStateWithLifecycle()
    val uiState by viewModel.friendsUiState.collectAsStateWithLifecycle()
    val showRequests by requestsViewModel.showRequests.collectAsStateWithLifecycle()
    val pendingCount by requestsViewModel.pendingCount.collectAsStateWithLifecycle()
    val requestsUiState by requestsViewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        friendsHeader(
            query = query,
            pendingCount = pendingCount,
            showRequests = showRequests,
            onQueryChange = viewModel::onFriendsQueryChange,
            onClearQuery = viewModel::clearFriendsQuery,
            onToggleRequests = { requestsViewModel.toggleRequests() },
        )
        AnimatedVisibility(
            visible = showRequests,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            requestsPanel(
                uiState = requestsUiState,
                onAccept = { id ->
                    requestsViewModel.respondRequest(id, accept = true) { viewModel.loadFriends() }
                },
                onReject = { id ->
                    requestsViewModel.respondRequest(id, accept = false) {}
                },
            )
        }
        HorizontalDivider()
        when (val state = uiState) {
            is FriendsUiState.Loading -> loadingState()
            is FriendsUiState.Empty ->
                emptyState(
                    "emptyUser",
                    text =
                        if (query.isBlank()) {
                            stringResource(R.string.no_friends)
                        } else {
                            stringResource(R.string.no_friends_found)
                        },
                )
            is FriendsUiState.Error -> errorState(state.message.asString())
            is FriendsUiState.Success ->
                friendsList(
                    friends = state.friends,
                    onFriendClick = onFriendClick,
                    onChatClick = { amicId ->
                        viewModel.getConversationId(amicId) { convId, otherUserId, username ->
                            onChatClick(convId, otherUserId, username)
                        }
                    },
                    onDelete = { amicId -> viewModel.deleteFriend(amicId) },
                )
        }
    }
}

@Composable
private fun friendsHeader(
    query: String,
    pendingCount: Int,
    showRequests: Boolean,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onToggleRequests: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        socialSearchBar(
            query = query,
            placeholder = stringResource(R.string.search_friend),
            onQueryChange = onQueryChange,
            onClear = onClearQuery,
            onSearch = { focusManager.clearFocus() },
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(8.dp))
        BadgedBox(
            badge = {
                if (pendingCount > 0) Badge { Text(pendingCount.toString()) }
            },
        ) {
            IconButton(onClick = onToggleRequests) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = stringResource(R.string.friend_requests),
                    tint =
                        if (showRequests) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }
        }
    }
}

@Composable
private fun friendsList(
    friends: List<FriendResponse>,
    onFriendClick: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onChatClick: (Int) -> Unit,
) {
    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding =
            androidx.compose.foundation.layout
                .PaddingValues(vertical = 12.dp),
    ) {
        items(friends, key = { it.id }) { friend ->
            friendItem(
                friend = friend,
                onFriendClick = { onFriendClick(friend.amic.id) },
                onDelete = { onDelete(friend.amic.id) },
                onChatClick = { onChatClick(friend.amic.id) },
            )
        }
    }
}

@Composable
private fun requestsPanel(
    uiState: RequestsUiState,
    onAccept: (Int) -> Unit,
    onReject: (Int) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = stringResource(R.string.pending_requests),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        when (val state = uiState) {
            is RequestsUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(modifier = Modifier.size(24.dp)) }
            }
            is RequestsUiState.Empty -> {
                Text(
                    text = stringResource(R.string.no_pending_requests),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            is RequestsUiState.Error -> {
                state.message?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            is RequestsUiState.Success -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.requests.forEach { request ->
                        friendRequestItem(
                            request = request,
                            onAccept = { onAccept(request.id) },
                            onReject = { onReject(request.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun friendRequestItem(
    request: FriendRequestResponse,
    onAccept: () -> Unit,
    onReject: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        userAvatarPlaceholder()
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = request.solicitantUsername,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = stringResource(R.string.friend_request),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(
                onClick = onAccept,
                modifier = Modifier.height(32.dp),
                contentPadding =
                    androidx.compose.foundation.layout
                        .PaddingValues(horizontal = 12.dp),
            ) {
                Text(stringResource(R.string.accept), style = MaterialTheme.typography.labelMedium)
            }
            OutlinedButton(
                onClick = onReject,
                modifier = Modifier.height(32.dp),
                contentPadding =
                    androidx.compose.foundation.layout
                        .PaddingValues(horizontal = 12.dp),
            ) {
                Text(stringResource(R.string.reject), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun friendItem(
    friend: FriendResponse,
    onFriendClick: (Int) -> Unit,
    onChatClick: (Int) -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_friend_title)) },
            text = {
                Text(stringResource(R.string.delete_friend_confirm, friend.amic.username))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                ) {
                    Text(
                        stringResource(R.string.profile_delete_account_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.profile_delete_account_cancel))
                }
            },
        )
    }

    Card(
        onClick = { onChatClick(friend.amic.id) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable(onClick = { onFriendClick(friend.amic.id) }),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = friend.amic.username,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.delete_friend_title),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun searchTab(viewModel: SocialViewModel) {
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val uiState by viewModel.searchUiState.collectAsStateWithLifecycle()
    val requestSentTo by viewModel.requestSentTo.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val forumViewModel: ForumViewModel = viewModel()
    val favoritForums by forumViewModel.favoritForums.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.requestFeedback.collect { uiText ->
            Toast.makeText(context, uiText.asString(context), Toast.LENGTH_SHORT).show()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            socialSearchBar(
                query = query,
                placeholder = stringResource(R.string.search_users_forums),
                onQueryChange = viewModel::onSearchQueryChange,
                onClear = viewModel::clearSearchQuery,
                onSearch = { focusManager.clearFocus() },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        HorizontalDivider()
        when (val state = uiState) {
            is SearchUiState.Idle -> idleSearchState()
            is SearchUiState.Loading -> loadingState()
            is SearchUiState.Empty -> emptyState("emptyUser", text = stringResource(R.string.no_user_found))
            is SearchUiState.Error -> errorState(state.message)
            is SearchUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding =
                        androidx.compose.foundation.layout
                            .PaddingValues(vertical = 12.dp),
                ) {
                    items(state.results, key = {
                        when (it) {
                            is DiscoverSearchResult.UserResult -> "user_${it.user.id}"
                            is DiscoverSearchResult.ForumResult -> "forum_${it.forum.id}"
                        }
                    }) { result ->
                        when (result) {
                            is DiscoverSearchResult.UserResult ->
                                userResultItem(
                                    user = result.user,
                                    requestSent = result.user.id in requestSentTo,
                                    onSendRequest = { viewModel.sendFriendRequest(result.user.id) },
                                )
                            is DiscoverSearchResult.ForumResult ->
                                forumCard(
                                    forum = result.forum,
                                    isFavorit = result.forum.id in favoritForums,
                                    isOwner = forumViewModel.isCreatedByCurrentUser(result.forum),
                                    onToggleFavorit = { forumViewModel.toggleFavorit(result.forum.id) },
                                    onDelete = { forumViewModel.deleteForum(result.forum.id) },
                                    onClick = {},
                                )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun userResultItem(
    user: UserSearchResult,
    requestSent: Boolean,
    onSendRequest: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (user.profilePic != null) {
                AsyncImage(
                    model = user.profilePic,
                    contentDescription = stringResource(R.string.photo) + " ${user.username}",
                    modifier = Modifier.size(44.dp).clip(CircleShape),
                )
            } else {
                userAvatarPlaceholder()
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = user.username,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            if (requestSent) {
                OutlinedButton(onClick = {}, enabled = false) {
                    Text(stringResource(R.string.sent), style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Button(onClick = onSendRequest) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.add))
                }
            }
        }
    }
}

@Composable
private fun forumsTab(forumViewModel: ForumViewModel = viewModel()) {
    val uiState by forumViewModel.forumsUiState.collectAsStateWithLifecycle()
    val favoritForums by forumViewModel.favoritForums.collectAsStateWithLifecycle()
    val searchQuery by forumViewModel.searchQuery.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var forumToDelete by remember { mutableStateOf<Forum?>(null) }

    if (showCreateDialog) {
        createForumDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { nom, descripcio ->
                forumViewModel.createForum(nom, descripcio)
                showCreateDialog = false
            },
        )
    }

    forumToDelete?.let { forum ->
        deleteForumDialog(
            forumNom = forum.nom ?: "",
            onDismiss = { forumToDelete = null },
            onConfirm = {
                forumViewModel.deleteForum(forum.id)
                forumToDelete = null
            },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                socialSearchBar(
                    query = searchQuery,
                    placeholder = stringResource(R.string.search_forum),
                    onQueryChange = forumViewModel::onSearchQueryChange,
                    onClear = forumViewModel::clearSearchQuery,
                    onSearch = { focusManager.clearFocus() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            HorizontalDivider()

            var activeFilter by remember { mutableStateOf(ForumFilter.FAVORITS) }
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = activeFilter == ForumFilter.FAVORITS,
                    onClick = { activeFilter = ForumFilter.FAVORITS },
                    label = { Text(stringResource(R.string.forums_filter_saved)) },
                    leadingIcon = {
                        Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                )
                FilterChip(
                    selected = activeFilter == ForumFilter.MEUS,
                    onClick = { activeFilter = ForumFilter.MEUS },
                    label = { Text(stringResource(R.string.forums_filter_mine)) },
                )
            }
            when (val state = uiState) {
                is ForumUiState.Loading -> loadingState()
                is ForumUiState.Empty ->
                    emptyState(
                        "emptyForum",
                        text = stringResource(R.string.no_favorite_created_forums_found),
                    )
                is ForumUiState.Error -> errorState(state.message)
                is ForumUiState.Success -> {
                    val filteredForums =
                        when (activeFilter) {
                            ForumFilter.FAVORITS ->
                                state.forums.filter {
                                    it.id in favoritForums
                                }
                            ForumFilter.MEUS ->
                                state.forums.filter {
                                    forumViewModel.isCreatedByCurrentUser(it)
                                }
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
                                onDelete = { forumToDelete = forum },
                                onClick = {},
                            )
                        }
                    }
                }
            }
        }

        createForumFab(
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            onClick = { showCreateDialog = true },
        )
    }
}
