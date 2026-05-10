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
import com.airgo.airgoApp.features.social.friends.data.FriendRequestResponse
import com.airgo.airgoApp.features.social.friends.data.FriendResponse
import com.airgo.airgoApp.features.social.friends.domain.FriendRequestsViewModel

@Composable
fun socialScreen(
    onFriendClick: (String) -> Unit = {},
    viewModel: SocialViewModel = viewModel(),
) {
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = activeTab.ordinal) {
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
        }

        when (activeTab) {
            SocialTab.FRIENDS ->
                friendsTab(
                    viewModel = viewModel,
                    onFriendClick = onFriendClick,
                )
            SocialTab.SEARCH -> searchTab(viewModel = viewModel)
            SocialTab.REQUESTS -> Unit
        }
    }
}

@Composable
private fun friendsTab(
    viewModel: SocialViewModel,
    requestsViewModel: FriendRequestsViewModel = viewModel(),
    onFriendClick: (String) -> Unit,
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
                    text =
                        if (query.isBlank()) {
                            stringResource(R.string.no_friends)
                        } else {
                            stringResource(R.string.no_friends_found)
                        },
                )
            is FriendsUiState.Error -> errorState(state.message)
            is FriendsUiState.Success ->
                friendsList(
                    friends = state.friends,
                    onFriendClick = onFriendClick,
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
    onFriendClick: (String) -> Unit,
    onDelete: (Int) -> Unit,
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
                onClick = { onFriendClick(friend.amic.id.toString()) },
                onDelete = { onDelete(friend.amic.id) },
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
    onClick: () -> Unit,
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
            // Solo la foto navega al perfil
            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable(onClick = onClick),
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
            // Botón eliminar siempre visible
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
                placeholder = stringResource(R.string.search_users),
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
            is SearchUiState.Empty -> emptyState(text = stringResource(R.string.no_user_found))
            is SearchUiState.Error -> errorState(state.message)
            is SearchUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding =
                        androidx.compose.foundation.layout
                            .PaddingValues(vertical = 12.dp),
                ) {
                    items(state.users, key = { it.id }) { user ->
                        userResultItem(
                            user = user,
                            requestSent = user.id in requestSentTo,
                            onSendRequest = { viewModel.sendFriendRequest(user.id) },
                        )
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
