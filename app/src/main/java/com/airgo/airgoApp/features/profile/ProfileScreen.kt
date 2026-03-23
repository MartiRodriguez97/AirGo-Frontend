package com.airgo.airgoApp.features.profile

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.features.profile.components.badgesAndActivitiesZone
import com.airgo.airgoApp.features.profile.components.deleteAccountDialog
import com.airgo.airgoApp.features.profile.components.deleteAccountSection
import com.airgo.airgoApp.features.profile.components.profileUserInfoCard
import com.airgo.airgoApp.features.profile.domain.ProfileUiState
import com.airgo.airgoApp.features.profile.domain.ProfileViewModel
import com.airgo.airgoApp.features.settings.settingsScreen

data class ProfileScreen(
    val displayName: String,
    val email: String?,
    val currentPictureUrl: String?,
    val initial: String,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun profileScreen(
    email: String?,
    pictureUrl: String?,
    viewModel: ProfileViewModel = viewModel(),
    onAccountDeleted: () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()
    var currentPictureUrl by remember { mutableStateOf(SessionManager.profilePictureUrl ?: pictureUrl) }
    val context = LocalContext.current
    val displayName =
        email
            ?.substringBefore('@')
            ?.replaceFirstChar { it.uppercase() } ?: "Usuario"
    val initial = displayName.firstOrNull()?.toString() ?: "U"

    val profileInfo =
        ProfileScreen(
            displayName = displayName,
            email = email,
            currentPictureUrl = currentPictureUrl,
            initial = initial,
        )

    val singlePhotoPickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
            onResult = { uri ->
                if (uri != null) {
                    viewModel.updateProfilePicture(context, uri.toString())
                }
            },
        )

    profileScreenLaunchedEffect(
        uiState = uiState,
        context = context,
        onAccountDeleted = onAccountDeleted,
        onResetUiState = { viewModel.resetUiState() },
        onPictureUpdateSuccess = { currentPictureUrl = SessionManager.profilePictureUrl },
    )

    Box(modifier = Modifier.fillMaxSize()) {
        profileScreenAnimatedContent(
            isSettingsVisible = showSettings,
            profileInfo = profileInfo,
            uiState = uiState,
            singlePhotoPickerLauncher = singlePhotoPickerLauncher,
            onDeleteClicked = { showDeleteDialog = true },
            onSettingsClick = { showSettings = true },
            onSettingsBack = { showSettings = false },
        )
    }

    profileDeleteDialog(
        showDialog = showDeleteDialog,
        onDismiss = { showDeleteDialog = false },
        onConfirm = {
            showDeleteDialog = false
            viewModel.deleteAccount()
        },
    )
}

@Composable
private fun profileScreenLaunchedEffect(
    uiState: ProfileUiState,
    context: android.content.Context,
    onAccountDeleted: () -> Unit,
    onResetUiState: () -> Unit,
    onPictureUpdateSuccess: () -> Unit,
) {
    LaunchedEffect(uiState) {
        when (uiState) {
            is ProfileUiState.DeleteSuccess -> onAccountDeleted()
            is ProfileUiState.PictureUpdateSuccess -> {
                onPictureUpdateSuccess()
                Toast.makeText(context, "Foto actualitzada correctament", Toast.LENGTH_SHORT).show()
                onResetUiState() // Resetear el estado tras mostrar el mensaje
            }
            is ProfileUiState.Error -> {
                Toast.makeText(context, uiState.message, Toast.LENGTH_LONG).show()
                onResetUiState()
            }
            else -> {}
        }
    }
}

@Composable
private fun profileScreenAnimatedContent(
    isSettingsVisible: Boolean,
    profileInfo: ProfileScreen,
    uiState: ProfileUiState,
    singlePhotoPickerLauncher: ActivityResultLauncher<PickVisualMediaRequest>,
    onDeleteClicked: () -> Unit,
    onSettingsClick: () -> Unit,
    onSettingsBack: () -> Unit,
) {
    AnimatedContent(
        targetState = isSettingsVisible,
        transitionSpec = {
            if (targetState) {
                (slideInHorizontally { fullWidth -> fullWidth } + fadeIn()).togetherWith(
                    slideOutHorizontally { fullWidth -> -fullWidth } + fadeOut(),
                )
            } else {
                (slideInHorizontally { fullWidth -> -fullWidth } + fadeIn()).togetherWith(
                    slideOutHorizontally { fullWidth -> fullWidth } + fadeOut(),
                )
            }
        },
        label = "profile-settings",
    ) { showSettings ->
        if (!showSettings) {
            showProfileInfo(
                profileInfo = profileInfo,
                uiState = uiState,
                singlePhotoPickerLauncher = singlePhotoPickerLauncher,
                onDeleteClicked = onDeleteClicked,
                onSettingsClick = onSettingsClick,
            )
        } else {
            settingsScreen(
                email = profileInfo.email,
                displayName = profileInfo.displayName,
                pictureUrl = profileInfo.currentPictureUrl,
                initial = profileInfo.initial,
                onBackClick = onSettingsBack,
            )
        }
    }
}

@Composable
private fun profileDeleteDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (showDialog) {
        deleteAccountDialog(
            onDismiss = onDismiss,
            onConfirm = onConfirm,
        )
    }
}

@Composable
fun showProfileInfo(
    profileInfo: ProfileScreen,
    uiState: ProfileUiState,
    singlePhotoPickerLauncher: ActivityResultLauncher<PickVisualMediaRequest>,
    onDeleteClicked: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        showProfileInfoContent(
            profileInfo = profileInfo,
            uiState = uiState,
            singlePhotoPickerLauncher = singlePhotoPickerLauncher,
            onDeleteClicked = onDeleteClicked,
        )

        showProfileInfoSettingsButton(onSettingsClick = onSettingsClick)
    }
}

@Composable
private fun showProfileInfoContent(
    profileInfo: ProfileScreen,
    uiState: ProfileUiState,
    singlePhotoPickerLauncher: ActivityResultLauncher<PickVisualMediaRequest>,
    onDeleteClicked: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        profileUserInfoCard(
            displayName = profileInfo.displayName,
            email = profileInfo.email,
            pictureUrl = profileInfo.currentPictureUrl,
            initial = profileInfo.initial,
            isUpdatingPicture = uiState is ProfileUiState.PictureUpdateLoading,
            onEditProfilePictureClick = {
                singlePhotoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
        )
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 16.dp),
            thickness = DividerDefaults.Thickness,
            color = DividerDefaults.color,
        )
        badgesAndActivitiesZone()
        Spacer(modifier = Modifier.weight(1f))

        deleteAccountSection(
            uiState = uiState,
            onDeleteClicked = onDeleteClicked,
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 16.dp),
            thickness = DividerDefaults.Thickness,
            color = DividerDefaults.color,
        )
    }
}

@Composable
private fun showProfileInfoSettingsButton(onSettingsClick: () -> Unit) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(top = 8.dp, end = 8.dp),
    ) {
        IconButton(
            onClick = onSettingsClick,
            modifier =
                Modifier
                    .align(Alignment.TopEnd),
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "Settings",
            )
        }
    }
}
