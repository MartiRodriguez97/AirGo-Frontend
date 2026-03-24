package com.airgo.airgoApp.features.profile.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

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
