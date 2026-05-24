package com.airgo.airgoApp.features.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SignalCellularAlt1Bar
import androidx.compose.material.icons.filled.SignalCellularAlt2Bar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.airgo.airgoApp.R

private const val HALF_WIDTH = 0.5f

// Level IDs
private const val LEVEL_EASY_ID = 1
private const val LEVEL_MEDIUM_ID = 2
private const val LEVEL_ADVANCED_ID = 3

// Day IDs
private const val DAY_MONDAY = 1
private const val DAY_TUESDAY = 2
private const val DAY_WEDNESDAY = 3
private const val DAY_THURSDAY = 4
private const val DAY_FRIDAY = 5
private const val DAY_SATURDAY = 6
private const val DAY_SUNDAY = 7

@Composable
internal fun stepSportSelection(
    selectedSport: String?,
    onSportSelected: (String) -> Unit,
) {
    Text(stringResource(R.string.quest_title_sport), style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(24.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val sportRun = stringResource(R.string.sport_run)
        selectionButton(
            text = sportRun,
            icon = Icons.Default.DirectionsRun,
            selected = selectedSport == "CAM",
            onClick = { onSportSelected("CAM") },
            modifier = Modifier.weight(1f),
        )
        val sportBike = stringResource(R.string.sport_bike)
        selectionButton(
            text = sportBike,
            icon = Icons.Default.DirectionsBike,
            selected = selectedSport == "BIC",
            onClick = { onSportSelected("BIC") },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
internal fun stepLevelSelection(
    selectedLevel: Int?,
    onLevelSelected: (Int) -> Unit,
) {
    Text(stringResource(R.string.quest_title_level), style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(24.dp))
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val easy = stringResource(R.string.level_easy)
        selectionButton(
            text = easy,
            icon = Icons.Default.SignalCellularAlt1Bar,
            selected = selectedLevel == LEVEL_EASY_ID,
            onClick = { onLevelSelected(LEVEL_EASY_ID) },
        )
        val medium = stringResource(R.string.level_medium)
        selectionButton(
            text = medium,
            icon = Icons.Default.SignalCellularAlt2Bar,
            selected = selectedLevel == LEVEL_MEDIUM_ID,
            onClick = { onLevelSelected(LEVEL_MEDIUM_ID) },
        )
        val advanced = stringResource(R.string.level_advanced)
        selectionButton(
            text = advanced,
            icon = Icons.Default.SignalCellularAlt,
            selected = selectedLevel == LEVEL_ADVANCED_ID,
            onClick = { onLevelSelected(LEVEL_ADVANCED_ID) },
        )
    }
}

@Composable
internal fun stepDaysSelection(selectedDays: SnapshotStateList<Int>) {
    val daysOfWeek =
        listOf(
            DAY_MONDAY to (R.string.day_monday),
            DAY_TUESDAY to (R.string.day_tuesday),
            DAY_WEDNESDAY to (R.string.day_wednesday),
            DAY_THURSDAY to (R.string.day_thursday),
            DAY_FRIDAY to (R.string.day_friday),
            DAY_SATURDAY to (R.string.day_saturday),
            DAY_SUNDAY to (R.string.day_sunday),
        )
    Text(stringResource(R.string.quest_title_days), style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(24.dp))
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        daysOfWeek.forEach { (dayId, dayRes) ->
            val dayName = stringResource(dayRes)
            val isSelected = selectedDays.contains(dayId)

            FilterChip(
                selected = isSelected,
                onClick = {
                    if (isSelected) {
                        selectedDays.remove(dayId)
                    } else {
                        selectedDays.add(dayId)
                    }
                },
                label = {
                    Text(
                        text = dayName,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                },
                modifier = Modifier.fillMaxWidth(HALF_WIDTH),
            )
        }
    }
}

private const val MAX_MIDA_CALENDARI = 1.2f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun stepDateSelection(datePickerState: DatePickerState) {
    Text(stringResource(R.string.quest_title_date), style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(16.dp))

    val currentDensity = LocalDensity.current
    val customDensity =
        Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale.coerceAtMost(MAX_MIDA_CALENDARI),
        )

    CompositionLocalProvider(LocalDensity provides customDensity) {
        DatePicker(
            state = datePickerState,
            modifier = Modifier.fillMaxWidth(),
            showModeToggle = false,
        )
    }
}

@Composable
internal fun selectionButton(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier =
            modifier
                .fillMaxWidth()
                .height(60.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor =
                    if (selected) {
                        MaterialTheme
                            .colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                contentColor =
                    if (selected) {
                        MaterialTheme
                            .colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            ),
        shape = RoundedCornerShape(12.dp),
        elevation =
            ButtonDefaults.buttonElevation(
                defaultElevation = 2.dp,
                pressedElevation = 8.dp,
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
