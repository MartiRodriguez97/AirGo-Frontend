package com.airgo.airgoApp.features.profile

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.with
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
import com.airgo.airgoApp.features.settings.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ProfileScreen(
    email: String?,
    pictureUrl: String?,
    viewModel: ProfileViewModel = viewModel(),
    onAccountDeleted: () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()
    var currentPictureUrl by remember { mutableStateOf(pictureUrl ?: SessionManager.profilePictureUrl) }
    val context = LocalContext.current
    val displayName =
        email
            ?.substringBefore('@')
            ?.replaceFirstChar { it.uppercase() } ?: "Usuario"
    val initial = displayName.firstOrNull()?.toString() ?: "U"

    val singlePhotoPickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
            onResult = { uri ->
                if (uri != null) {
                    viewModel.updateProfilePicture(context, uri.toString())
                }
            },
        )
    LaunchedEffect(uiState) {
        when (uiState) {
            is ProfileUiState.DeleteSuccess -> onAccountDeleted()
            is ProfileUiState.PictureUpdateSuccess -> {
                currentPictureUrl = SessionManager.profilePictureUrl
                Toast.makeText(context, "Foto actualitzada correctament", Toast.LENGTH_SHORT).show()
            }
            is ProfileUiState.Error -> {
                Toast.makeText(context, (uiState as ProfileUiState.Error).message, Toast.LENGTH_LONG).show()
                viewModel.resetUiState()
            }
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        AnimatedContent(
            targetState = showSettings,
            transitionSpec = {
                if (targetState) {
                    // Profile -> Settings
                    (slideInHorizontally { fullWidth -> fullWidth } + fadeIn()).togetherWith(
                        slideOutHorizontally { fullWidth -> -fullWidth } + fadeOut())
                } else {
                    // Settings -> Profile
                    (slideInHorizontally { fullWidth -> -fullWidth } + fadeIn()).togetherWith(
                        slideOutHorizontally { fullWidth -> fullWidth } + fadeOut())
                }
            },
            label = "profile-settings"
        ) {
            isSettingsVisible ->
            if (!isSettingsVisible) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // ProfileScreen
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top,
                    ) {
                        // user card info
                        profileUserInfoCard(
                            displayName = displayName,
                            email = email,
                            pictureUrl = currentPictureUrl,
                            initial = initial,
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
                            onDeleteClicked = { showDeleteDialog = true },
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            thickness = DividerDefaults.Thickness,
                            color = DividerDefaults.color,
                        )
                    }

                    // Settings button overlay (top-right corner)
                    IconButton(
                        onClick = { showSettings = true },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 8.dp, end = 8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                        )
                    }
                }
            } else {
                // SettingsScreen
                SettingsScreen(
                    email = email,
                    displayName = displayName,
                    pictureUrl = currentPictureUrl,
                    initial = initial,
                    onBackClick = { showSettings = false },
                )
            }
        }
    }

    if (showDeleteDialog) {
        deleteAccountDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteAccount()
            },
        )
    }
}
