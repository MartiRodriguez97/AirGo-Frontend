package com.airgo.airgoApp.features.settings

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

@Composable
fun settingsScreen(
    email: String?,
    displayName: String,
    pictureUrl: String?,
    initial: String,
    onBackClick: () -> Unit,
    onAccountSettingsClick: () -> Unit,
) {
    BackHandler {
        onBackClick()
    }

    settingsMainContent(
        email = email,
        displayName = displayName,
        pictureUrl = pictureUrl,
        initial = initial,
        onBackClick = onBackClick,
        onAccountSettingsClick = onAccountSettingsClick,
    )
}

@Composable
private fun settingsMainContent(
    email: String?,
    displayName: String,
    pictureUrl: String?,
    initial: String,
    onBackClick: () -> Unit,
    onAccountSettingsClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        settingsHeader(onBackClick = onBackClick)

        settingsUserProfile(
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

        settingsOptions(onAccountSettingsClick = onAccountSettingsClick)
    }
}

@Composable
private fun settingsHeader(onBackClick: () -> Unit) {
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
    }
}

@Composable
private fun settingsUserProfile(
    displayName: String,
    email: String?,
    pictureUrl: String?,
    initial: String,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        settingsProfilePicture(pictureUrl = pictureUrl, initial = initial)

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = displayName,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = email ?: "Sense correu",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun settingsProfilePicture(
    pictureUrl: String?,
    initial: String,
) {
    if (!pictureUrl.isNullOrBlank()) {
        val avatarBitmap by produceState<Bitmap?>(
            initialValue = null,
            key1 = pictureUrl,
        ) {
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
                contentDescription = "Profile Picture",
                modifier =
                    Modifier
                        .size(80.dp)
                        .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else {
            settingsProfileInitialAvatar(initial = initial)
        }
    } else {
        settingsProfileInitialAvatar(initial = initial)
    }
}

@Composable
private fun settingsProfileInitialAvatar(initial: String) {
    Box(
        modifier =
            Modifier
                .size(80.dp)
                .clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun settingsOptions(onAccountSettingsClick: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
    ) {
        settingsAccountOption(onAccountSettingsClick = onAccountSettingsClick)
    }
}

@Composable
private fun settingsAccountOption(onAccountSettingsClick: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onAccountSettingsClick() }
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "El teu compte",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = "Go to account settings",
            modifier = Modifier.size(24.dp),
        )
    }
}
