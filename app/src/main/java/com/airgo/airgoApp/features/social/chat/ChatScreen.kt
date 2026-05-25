package com.airgo.airgoApp.features.social.chat

import android.app.NotificationManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.airgo.airgoApp.BuildConfig
import com.airgo.airgoApp.R
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.features.notifications.service.FirebaseMessagingService
import com.airgo.airgoApp.features.social.chat.data.Message
import com.airgo.airgoApp.features.social.chat.domain.ChatUiState
import com.airgo.airgoApp.features.social.chat.domain.ChatViewModel

private val OwnBubbleShape =
    androidx.compose.foundation.shape.RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = 18.dp,
        bottomEnd = 4.dp,
    )
private val OtherBubbleShape =
    androidx.compose.foundation.shape.RoundedCornerShape(
        topStart = 4.dp,
        topEnd = 18.dp,
        bottomStart = 18.dp,
        bottomEnd = 18.dp,
    )
private const val READ_TICK_COLOR = 0xFF4FC3F7

private val ReadTickColor = Color(READ_TICK_COLOR)
private const val AVATAR_INITIAL_PLACEHOLDER = "?"
private const val BUBBLE_MAX_WIDTH = 280

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun chatScreen(
    conversationId: String,
    otherUserId: Int,
    otherUsername: String,
    otherProfilePic: String? = null,
    onBack: () -> Unit,
    viewModel: ChatViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // Añadimos un log para ver qué nos llega
    LaunchedEffect(otherProfilePic) {
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(conversationId, lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    SessionManager.currentChatId = conversationId

                    // Limpiar notificaciones de este chat al entrar o volver
                    val notificationManager = context.getSystemService(
                        Context.NOTIFICATION_SERVICE)
                            as NotificationManager
                    notificationManager.cancel(conversationId.hashCode())

                    // Limpiar resumen si no quedan más chats activos
                    val activeNotifications = notificationManager.activeNotifications
                    val hasMoreChats =
                        activeNotifications.any {
                            it.id != FirebaseMessagingService.SUMMARY_ID &&
                                it.id != conversationId.hashCode() &&
                                it.notification.group == FirebaseMessagingService.CHAT_GROUP_KEY
                        }
                    if (!hasMoreChats) {
                        notificationManager.cancel(FirebaseMessagingService.SUMMARY_ID)
                    }
                } else if (event == Lifecycle.Event.ON_PAUSE) {
                    SessionManager.currentChatId = null
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            SessionManager.currentChatId = null
        }
    }

    LaunchedEffect(conversationId) {
        viewModel.init(conversationId, otherUserId)
    }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(0)
    }

    Scaffold(
        topBar = { chatTopBar(otherUsername = otherUsername, otherProfilePic = otherProfilePic, onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).imePadding()) {
            Box(modifier = Modifier.weight(1f)) {
                when (uiState) {
                    is ChatUiState.Loading ->
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    is ChatUiState.Empty -> chatEmptyState()
                    is ChatUiState.Error ->
                        Text(
                            text = (uiState as ChatUiState.Error).message.asString(),
                            modifier = Modifier.align(Alignment.Center).padding(16.dp),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    is ChatUiState.Success ->
                        chatMessageList(
                            messages = messages,
                            listState = listState,
                            onDelete = { viewModel.deleteMessage(it) },
                            otherProfilePic = otherProfilePic
                        )
                }
            }
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            chatInputBar(
                onSend = { text -> viewModel.sendMessage(text) },
                modifier = Modifier.navigationBarsPadding(),
            )
        }
    }
}

@Composable
private fun chatMessageList(
    messages: List<Message>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onDelete: (Int) -> Unit,
    otherProfilePic: String? = null,
) {
    var selectedMessageId by remember { mutableStateOf<Int?>(null) }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        reverseLayout = true,
    ) {
        items(messages.reversed(), key = { it.id }) { message ->
            messageBubble(
                message = message,
                isOwn = message.emissor == SessionManager.userId,
                isSelected = selectedMessageId == message.id,
                onSelect = { selectedMessageId = if (selectedMessageId == message.id) null else message.id },
                onDelete = {
                    onDelete(message.id)
                    selectedMessageId = null
                },
                otherProfilePic = otherProfilePic
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun chatTopBar(
    otherUsername: String,
    otherProfilePic: String? = null,
    onBack: () -> Unit,
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .then(
                                if (otherProfilePic != null) Modifier else Modifier.padding(8.dp),
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!otherProfilePic.isNullOrEmpty()) {
                        val fullUrl =
                            if (otherProfilePic.startsWith("http")) {
                                otherProfilePic
                            } else {
                                BuildConfig.BACKEND_URL.trimEnd('/') + "/" + otherProfilePic.trimStart('/')
                            }
                        AsyncImage(
                            model = fullUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = otherUsername,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
    )
}

@Composable
private fun chatEmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.begin_conversation),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun messageBubble(
    message: Message,
    isOwn: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    otherProfilePic: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (!isOwn) bubbleAvatar(message.emissorUsername, message.emissorProfilePic ?: otherProfilePic)
        Column(
            horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = BUBBLE_MAX_WIDTH.dp),
        ) {
            bubbleContent(message = message, isOwn = isOwn, onClick = { if (isOwn) onSelect() })
            if (isOwn) bubbleTicks(llegit = message.llegit)
            if (isOwn && isSelected) bubbleDeleteButton(onDelete = onDelete)
        }
        if (isOwn) Spacer(modifier = Modifier.width(6.dp))
    }
}

@Composable
private fun bubbleAvatar(
    username: String,
    profilePicUrl: String?,
) {
    Box(
        modifier =
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        val showImage = !profilePicUrl.isNullOrEmpty() && profilePicUrl != "null" && profilePicUrl != "undefined"
        if (showImage) {
            val fullUrl =
                if (profilePicUrl.startsWith("http")) {
                    profilePicUrl
                } else {
                    BuildConfig.BACKEND_URL.trimEnd('/') + "/" + profilePicUrl.trimStart('/')
                }
            AsyncImage(
                model = fullUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                text = username.firstOrNull()?.uppercaseChar()?.toString() ?: AVATAR_INITIAL_PLACEHOLDER,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
    Spacer(modifier = Modifier.width(6.dp))
}

@Composable
private fun bubbleContent(
    message: Message,
    isOwn: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = if (isOwn) OwnBubbleShape else OtherBubbleShape,
        color = if (isOwn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = if (isOwn) 0.dp else 1.dp,
        onClick = onClick,
    ) {
        Text(
            text = message.contingut,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 20.sp,
            color = if (isOwn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun bubbleTicks(llegit: Boolean) {
    Spacer(modifier = Modifier.height(2.dp))
    Icon(
        imageVector = if (llegit) Icons.Default.DoneAll else Icons.Default.Done,
        contentDescription = null,
        tint = if (llegit) ReadTickColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.size(14.dp),
    )
}

@Composable
private fun bubbleDeleteButton(onDelete: () -> Unit) {
    Spacer(modifier = Modifier.height(2.dp))
    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
        Icon(
            Icons.Default.Delete,
            contentDescription = stringResource(R.string.profile_delete_account_confirm),
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(16.dp),
        )
    }
}
