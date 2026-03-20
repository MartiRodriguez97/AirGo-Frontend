package com.airgo.airgoApp.features.profile

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun profileScreen(
    email: String?,
    pictureUrl: String?,
    viewModel: ProfileViewModel = viewModel(),
    onAccountDeleted: () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
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

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
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
                // Implementar funcionalitat de canvi de foto de perfil
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
