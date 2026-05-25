package com.airgo.airgoApp.features.profile.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.airgo.airgoApp.R
import com.airgo.airgoApp.core.utils.translation.translateBadgeDescription
import com.airgo.airgoApp.core.utils.translation.translateBadgeName
import com.airgo.airgoApp.features.profile.data.UserBadgeResponse

private const val BADGE_DIALOG_ICON_SIZE = 80
private const val DATE_LENGTH = 10

@Composable
fun badgesZone(badges: List<UserBadgeResponse>) {
    var selectedBadge by remember { mutableStateOf<UserBadgeResponse?>(null) }

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Text(
            text = stringResource(R.string.profile_badges_zone_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        if (badges.isEmpty()) {
            Text(
                text = stringResource(R.string.profile_badges_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            badgesList(badges) { selectedBadge = it }
        }
    }

    selectedBadge?.let { badge ->
        badgeDetailDialog(badge) { selectedBadge = null }
    }
}

@Composable
private fun badgesList(
    badges: List<UserBadgeResponse>,
    onBadgeClick: (UserBadgeResponse) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(badges) { userBadge ->
            Surface(
                onClick = { onBadgeClick(userBadge) },
                shape = CircleShape,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (userBadge.insignia.icona != null) {
                        AsyncImage(
                            model = userBadge.insignia.icona,
                            contentDescription = userBadge.insignia.nom,
                            modifier = Modifier.size(64.dp).clip(CircleShape),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = userBadge.insignia.nom,
                            modifier = Modifier.size(64.dp).clip(CircleShape),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun badgeDetailDialog(
    badge: UserBadgeResponse,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = translateBadgeName(badge.insignia.nom),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(16.dp))
                badgeIcon(badge)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = translateBadgeDescription(badge.insignia.nom),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.profile_badges_earned_on, badge.dataGuanyada.take(DATE_LENGTH)),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onDismiss) {
                    Text(stringResource(R.string.profile_badges_dialog_close))
                }
            }
        }
    }
}

@Composable
private fun badgeIcon(badge: UserBadgeResponse) {
    if (badge.insignia.icona != null) {
        AsyncImage(
            model = badge.insignia.icona,
            contentDescription = badge.insignia.nom,
            modifier = Modifier.size(BADGE_DIALOG_ICON_SIZE.dp),
        )
    } else {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = badge.insignia.nom,
            modifier = Modifier.size(BADGE_DIALOG_ICON_SIZE.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}
