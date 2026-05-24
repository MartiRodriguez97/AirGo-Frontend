package com.airgo.airgoApp.features.tracking.components

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.airgo.airgoApp.core.utils.translation.translateTitolDescription
import com.airgo.airgoApp.core.utils.translation.translateTitolName
import com.airgo.airgoApp.features.profile.data.BadgeDefinition
import com.airgo.airgoApp.features.tracking.data.TitleOption
import com.airgo.airgoApp.features.tracking.domain.TrackingViewModel
import com.airgo.airgoApp.features.tracking.domain.TrackingViewModel.SaveState
import com.airgo.airgoApp.features.tracking.domain.fetchAvailableTitles
import com.airgo.airgoApp.features.tracking.domain.unlockTitle

private const val PADDING_SMALL = 4
private const val PADDING_MEDIUM = 8
private const val PADDING_LARGE = 16
private const val PADDING_EXTRA_LARGE = 24
private const val ICON_SIZE_BADGE = 80
private const val TEXT_SIZE_TITLE = 20
private const val TEXT_SIZE_SUBTITLE = 18
private const val TEXT_SIZE_BODY = 14

@Composable
internal fun trackingSummaryBadgeHandler(
    saveState: SaveState,
    activitySavedText: String,
    viewModel: TrackingViewModel,
    onFinish: () -> Unit,
) {
    val context = LocalContext.current
    var showBadgeDialog by remember { mutableStateOf<BadgeDefinition?>(null) }

    LaunchedEffect(saveState) {
        if (saveState is SaveState.Success) {
            val badges = saveState.response.newBadges
            if (badges.isNotEmpty()) {
                val b = badges.first()
                showBadgeDialog = BadgeDefinition(b.id, b.nom, b.descripcio, b.icona, "", 0)
            } else if (viewModel.titolsPendents > 0) {
                viewModel.fetchAvailableTitles()
            } else {
                finishTrackingSummary(context, activitySavedText, viewModel, onFinish)
            }
        }
    }

    showBadgeDialog?.let { badge ->
        newBadgeDialog(badge) {
            showBadgeDialog = null
            if (viewModel.titolsPendents > 0) {
                viewModel.fetchAvailableTitles()
            } else {
                finishTrackingSummary(context, activitySavedText, viewModel, onFinish)
            }
        }
    }

    if (viewModel.showTitleSelectionDialog) {
        titleSelectionDialog(
            options = viewModel.availableTitles,
            onSelect = { title, setAsMain ->
                viewModel.unlockTitle(title.id, setAsMain)
                finishTrackingSummary(context, activitySavedText, viewModel, onFinish)
            },
            onDismiss = {
                viewModel.showTitleSelectionDialog = false
                finishTrackingSummary(context, activitySavedText, viewModel, onFinish)
            },
        )
    }
}

private fun finishTrackingSummary(
    context: Context,
    message: String,
    viewModel: TrackingViewModel,
    onFinish: () -> Unit,
) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    viewModel.clearSummary()
    viewModel.clearSaveState()
    onFinish()
}

@Composable
fun titleSelectionDialog(
    options: List<TitleOption>,
    onSelect: (TitleOption, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var setAsMain by remember { mutableStateOf(true) }

    BackHandler(onBack = onDismiss)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.padding(PADDING_LARGE.dp).fillMaxWidth(),
            shape = RoundedCornerShape(PADDING_LARGE.dp),
        ) {
            Column(
                modifier = Modifier.padding(PADDING_EXTRA_LARGE.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text =
                        stringResource(
                            R.string.tracking_new_title_title,
                        ),
                    fontWeight = FontWeight.Bold,
                    fontSize = TEXT_SIZE_TITLE.sp,
                )
                Spacer(modifier = Modifier.height(PADDING_MEDIUM.dp))
                Text(
                    text = stringResource(R.string.tracking_new_title_desc),
                    fontSize = TEXT_SIZE_BODY.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(PADDING_LARGE.dp))

                options.forEach { option ->
                    OutlinedButton(
                        onClick = { onSelect(option, setAsMain) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = PADDING_SMALL.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = translateTitolName(option.nom), fontWeight = FontWeight.Bold)
                            Text(
                                text = translateTitolDescription(option.nom),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 14.sp,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(PADDING_LARGE.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(PADDING_SMALL.dp),
                ) {
                    androidx.compose.material3.Checkbox(
                        checked = setAsMain,
                        onCheckedChange = { setAsMain = it },
                    )
                    Text(
                        text = stringResource(R.string.tracking_set_main_title),
                        fontSize = TEXT_SIZE_BODY.sp,
                    )
                }
                Spacer(modifier = Modifier.height(PADDING_LARGE.dp))
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.tracking_later)) }
            }
        }
    }
}

@Composable
private fun newBadgeDialog(
    badge: BadgeDefinition,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.padding(PADDING_LARGE.dp).fillMaxWidth(),
            shape = RoundedCornerShape(PADDING_LARGE.dp),
        ) {
            Column(
                modifier = Modifier.padding(PADDING_EXTRA_LARGE.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.tracking_new_badge_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = TEXT_SIZE_TITLE.sp,
                )
                Spacer(modifier = Modifier.height(PADDING_LARGE.dp))
                if (badge.icona != null) {
                    AsyncImage(
                        model = badge.icona,
                        contentDescription = stringResource(R.string.tracking_badge_icon_desc),
                        modifier = Modifier.size(ICON_SIZE_BADGE.dp),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = stringResource(R.string.tracking_badge_icon_desc),
                        modifier = Modifier.size(ICON_SIZE_BADGE.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(modifier = Modifier.height(PADDING_LARGE.dp))
                Text(
                    text = translateBadgeName(badge.nom),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = TEXT_SIZE_SUBTITLE.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(PADDING_MEDIUM.dp))
                Text(
                    text = translateBadgeDescription(badge.nom),
                    fontSize = TEXT_SIZE_BODY.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(PADDING_EXTRA_LARGE.dp))
                Button(onClick = onDismiss) { Text(stringResource(R.string.tracking_accept)) }
            }
        }
    }
}
