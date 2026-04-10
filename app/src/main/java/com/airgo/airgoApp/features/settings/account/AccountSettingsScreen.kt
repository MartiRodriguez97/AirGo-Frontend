package com.airgo.airgoApp.features.settings.account

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.features.settings.account.username.editUsernameScreen

@Composable
fun accountSettingsScreen(
    displayName: String,
    onBackClick: () -> Unit,
) {
    var showEditUsername by remember { mutableStateOf(false) }
    // Update local state, initialize with parameters or SessionManager
    var currentDisplayName by remember { mutableStateOf(SessionManager.displayName ?: displayName) }
    var isEnabled by remember { mutableStateOf(false) }

    BackHandler {
        onBackClick()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = showEditUsername,
            transitionSpec = {
                if (showEditUsername) {
                    // Profile -> Settings
                    (slideInHorizontally { fullWidth -> fullWidth } + fadeIn()).togetherWith(
                        slideOutHorizontally { fullWidth -> -fullWidth } + fadeOut(),
                    )
                } else {
                    // Settings -> Profile
                    (slideInHorizontally { fullWidth -> -fullWidth } + fadeIn()).togetherWith(
                        slideOutHorizontally { fullWidth -> fullWidth } + fadeOut(),
                    )
                }
            },
            label = "edit-username-transition",
        ) { isEditUsernameVisible ->
            if (!isEditUsernameVisible) {
                accountSettingsContent(
                    displayName = currentDisplayName,
                    isEnabled = isEnabled,
                    onBicycleToggle = { isEnabled = it },
                    onEditUsername = { showEditUsername = true },
                    onBackClick = onBackClick,
                )
            } else {
                editUsernameScreen(
                    displayName = currentDisplayName,
                    onBackClick = { showEditUsername = false },
                    onSave = { newName ->
                        currentDisplayName = newName
                    },
                )
            }
        }
    }
}

@Composable
private fun accountSettingsContent(
    displayName: String,
    isEnabled: Boolean,
    onBicycleToggle: (Boolean) -> Unit,
    onEditUsername: () -> Unit,
    onBackClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        accountSettingsHeader(onBackClick = onBackClick)
        HorizontalDivider(
            thickness = DividerDefaults.Thickness,
            color = DividerDefaults.color,
        )
        usernameRow(
            displayName = displayName,
            onEditUsername = onEditUsername,
        )
        HorizontalDivider(
            thickness = DividerDefaults.Thickness,
            color = DividerDefaults.color,
        )
        bicycleAvailableRow(
            isEnabled = isEnabled,
            onBicycleToggle = onBicycleToggle,
        )
        HorizontalDivider(
            thickness = DividerDefaults.Thickness,
            color = DividerDefaults.color,
        )
    }
}

@Composable
private fun accountSettingsHeader(onBackClick: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "Tu cuenta",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun usernameRow(
    displayName: String,
    onEditUsername: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onEditUsername() }
                .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Nombre de usuario",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = displayName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
            contentDescription = "Editar",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun bicycleAvailableRow(
    isEnabled: Boolean,
    onBicycleToggle: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Bicicleta disponible",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = isEnabled,
            onCheckedChange = onBicycleToggle,
        )
    }
}
