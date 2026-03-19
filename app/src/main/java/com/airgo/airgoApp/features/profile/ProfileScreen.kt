package com.airgo.airgoApp.features.profile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
    val displayName =
        email
            ?.substringBefore('@')
            ?.replaceFirstChar { it.uppercase() } ?: "Usuario"
    val initial = displayName.firstOrNull()?.toString() ?: "U"

    if (uiState is ProfileUiState.DeleteSuccess) {
        LaunchedEffect(Unit) {
            onAccountDeleted()
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
) {
    Column(
        modifier = Modifier.padding(horizontal = 28.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        profileAvatar(
            pictureUrl = pictureUrl,
            initial = initial,
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
        Text("Eliminar cuenta")
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

    if (avatarBitmap != null) {
        Image(
            bitmap = avatarBitmap!!.asImageBitmap(),
            contentDescription = "Foto de perfil",
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .testTag("profile_avatar_image"),
        )
    } else {
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
}
