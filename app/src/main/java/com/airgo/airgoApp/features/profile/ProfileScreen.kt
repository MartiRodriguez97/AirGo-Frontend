package com.airgo.airgoApp.features.profile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airgo.airgoApp.features.profile.domain.ProfileUiState
import com.airgo.airgoApp.features.profile.domain.ProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

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
                    viewModel.updateProfilePicture(uri.toString())
                }
            },
        )
    LaunchedEffect(uiState) {
        when (uiState) {
            is ProfileUiState.DeleteSuccess -> onAccountDeleted()
            is ProfileUiState.PictureUpdateSuccess -> {
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
            pictureUrl = pictureUrl,
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

@Composable
fun profileUserInfoCard(
    displayName: String,
    email: String?,
    pictureUrl: String?,
    initial: String,
    isUpdatingPicture: Boolean,
    onEditProfilePictureClick: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 28.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        profileAvatar(
            pictureUrl = pictureUrl,
            initial = initial,
            isLoading = isUpdatingPicture,
            onEditClick = onEditProfilePictureClick,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = displayName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = email ?: "Sense correu disponible",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (!pictureUrl.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Foto vinculada correctamente",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
fun badgesAndActivitiesZone() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    RoundedCornerShape(16.dp),
                ).padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Zona per insignies i activitats",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun deleteAccountSection(
    uiState: ProfileUiState,
    onDeleteClicked: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (uiState is ProfileUiState.Error) {
            Text(
                text = uiState.message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        if (uiState is ProfileUiState.Loading) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        } else {
            deleteAccountButton(onDeleteClicked = onDeleteClicked)
        }
    }
}

@Composable
fun deleteAccountButton(onDeleteClicked: () -> Unit) {
    OutlinedButton(
        onClick = onDeleteClicked,
        colors =
            ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
        border =
            ButtonDefaults.outlinedButtonBorder.copy(
                brush =
                    androidx.compose.ui.graphics
                        .SolidColor(MaterialTheme.colorScheme.error),
            ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Eliminar compte")
    }
}

@Composable
fun deleteAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Eliminar compte?")
        },
        text = {
            Text(text = "Segur que vols eliminar el compte? Aquesta acció no es pot desfer.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
            ) {
                Text("Eliminar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    )
}

@Composable
fun profileAvatar(
    pictureUrl: String?,
    initial: String,
    isLoading: Boolean,
    onEditClick: () -> Unit,
) {
    val avatarBitmap by produceState<Bitmap?>(
        initialValue = null,
        key1 = pictureUrl,
    ) {
        if (pictureUrl.isNullOrBlank()) {
            value = null
            return@produceState
        }

        value =
            withContext(Dispatchers.IO) {
                runCatching {
                    URL(pictureUrl).openStream().use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }.getOrNull()
            }
    }
    Box(
        modifier = Modifier.padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (avatarBitmap != null) {
            avatarImage(bitmap = avatarBitmap!!)
        } else {
            avatarFallback(initial = initial)
        }
        if (isLoading) {
            avatarLoadingOverlay()
        }
        if (!isLoading) {
            avatarEditButton(onEditClick = onEditClick)
        }
    }
}

@Composable
private fun avatarImage(bitmap: Bitmap) {
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "Foto de perfil",
        contentScale = ContentScale.Crop,
        modifier =
            Modifier
                .size(84.dp)
                .clip(CircleShape)
                .testTag("profile_avatar_image"),
    )
}

@Composable
private fun avatarFallback(initial: String) {
    Box(
        modifier =
            Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .testTag("profile_avatar_fallback"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun avatarLoadingOverlay() {
    Box(
        modifier =
            Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            color = Color.White,
            strokeWidth = 3.dp,
            modifier = Modifier.size(32.dp),
        )
    }
}

@Composable
private fun BoxScope.avatarEditButton(onEditClick: () -> Unit) {
    IconButton(
        onClick = onEditClick,
        modifier =
            Modifier
                .align(Alignment.BottomEnd)
                .size(32.dp)
                .offset(x = 8.dp, y = 8.dp),
        colors =
            IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
    ) {
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "Canviar foto de perfil",
            modifier = Modifier.size(18.dp),
        )
    }
}
