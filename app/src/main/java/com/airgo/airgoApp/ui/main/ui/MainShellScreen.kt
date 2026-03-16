package com.airgo.airgoApp.ui.main.ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airgo.airgoApp.ui.theme.airGoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

enum class MainShellScreen(
    val label: String,
    val icon: ImageVector,
) {
    HOME(label = "Home", icon = Icons.Outlined.Home),
    FORUM(label = "Forum", icon = Icons.Outlined.People),
    ACTIVITY(label = "Actividad", icon = Icons.AutoMirrored.Outlined.DirectionsRun),
    MESSAGES(label = "Mensajes", icon = Icons.Outlined.MailOutline),
    PROFILE(label = "Perfil", icon = Icons.Outlined.Person),
}

@Composable
fun mainShellScreen(
    email: String?,
    pictureUrl: String?,
) {
    var selectedTab by rememberSaveable { mutableStateOf(MainShellScreen.HOME) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                MainShellScreen.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                            )
                        },
                        label = { Text(text = tab.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        menu(
            selectedTab = selectedTab,
            email = email,
            pictureUrl = pictureUrl,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
        )
    }
}

@Composable
private fun menu(
    selectedTab: MainShellScreen,
    email: String?,
    pictureUrl: String?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        when (selectedTab) {
            MainShellScreen.HOME ->
                mainSection(
                    title = "Home",
                    subtitle = "Tu punto de partida en AirGo",
                    icon = MainShellScreen.HOME.icon,
                )
            MainShellScreen.FORUM ->
                mainSection(
                    title = "Forum",
                    subtitle = "Espacio para conectar con otros usuarios",
                    icon = MainShellScreen.FORUM.icon,
                )
            MainShellScreen.ACTIVITY ->
                mainSection(
                    title = "Actividad",
                    subtitle = "Sigue tu movimiento y tus proximas rutas",
                    icon = MainShellScreen.ACTIVITY.icon,
                )
            MainShellScreen.MESSAGES ->
                mainSection(
                    title = "Mensajes",
                    subtitle = "Consulta tus conversaciones y avisos",
                    icon = MainShellScreen.MESSAGES.icon,
                )
            MainShellScreen.PROFILE ->
                profileSection(
                    email = email,
                    pictureUrl = pictureUrl,
                )
        }
    }
}

@Composable
private fun mainSection(
    title: String,
    subtitle: String,
    icon: ImageVector,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun profileSection(
    email: String?,
    pictureUrl: String?,
) {
    val displayName = email?.substringBefore('@')?.replaceFirstChar { it.uppercase() } ?: "Usuario"
    val initial = displayName.firstOrNull()?.toString() ?: "U"

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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
                    text = email ?: "Sin correo disponible",
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

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Aquí podrás mostrar tus datos, ajustes y foto de perfil.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun profileAvatar(
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun mainShellScreenPreview() {
    airGoTheme {
        mainShellScreen(
            email = "airgo@example.com",
            pictureUrl = "https://example.com/avatar.png",
        )
    }
}
