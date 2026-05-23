package com.airgo.airgoApp.features.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SignalCellularAlt1Bar
import androidx.compose.material.icons.filled.SignalCellularAlt2Bar
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airgo.airgoApp.R

private const val HALF_WIDTH = 0.5f

@Composable
internal fun stepSportSelection(
    selectedSport: Int?,
    onSportSelected: (Int) -> Unit,
) {
    Text(stringResource(R.string.quest_title_sport), style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(24.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        // correr opcio 1
        val sportRun = stringResource(R.string.sport_run)
        selectionButton(
            text = sportRun,
            icon = Icons.Default.DirectionsRun,
            selected = selectedSport == 1,
            onClick = { onSportSelected(1) },
        )
        // bici opcio 2
        val sportBike = stringResource(R.string.sport_bike)
        selectionButton(
            text = sportBike,
            icon = Icons.Default.DirectionsBike,
            selected = selectedSport == 2,
            onClick = { onSportSelected(2) },
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
            selected = selectedLevel == 1,
            onClick = { onLevelSelected(1) },
        )
        val medium = stringResource(R.string.level_medium)
        selectionButton(
            text = medium,
            icon = Icons.Default.SignalCellularAlt2Bar,
            selected = selectedLevel == 2,
            onClick = { onLevelSelected(2) },
        )
        val advanced = stringResource(R.string.level_advanced)
        selectionButton(
            text = advanced,
            icon = Icons.Default.SignalCellularAlt,
            selected = selectedLevel == 3,
            onClick = { onLevelSelected(3) },
        )
    }
}

@Composable
internal fun stepDaysSelection(selectedDays: SnapshotStateList<Int>) {
    val daysOfWeek = listOf(
        1 to (R.string.day_monday),
        2 to (R.string.day_tuesday),
        3 to (R.string.day_wednesday),
        4 to (R.string.day_thursday),
        5 to (R.string.day_friday),
        6 to (R.string.day_saturday),
        7 to (R.string.day_sunday),
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
                modifier = Modifier.fillMaxWidth(0.5f),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun stepDateSelection(datePickerState: DatePickerState) {
    Text(stringResource(R.string.quest_title_date), style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(16.dp))
    DatePicker(
        state = datePickerState,
        modifier = Modifier.fillMaxWidth(),
        showModeToggle = false,
    )
}

@Composable
internal fun selectionButton(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val containerCol =
        if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        }

    val contentCol =
        if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        }

    ElevatedButton(
        onClick = onClick,
        colors =
            ButtonDefaults.elevatedButtonColors(
                containerColor = containerCol,
                contentColor = contentCol,
            ),
        elevation =
            ButtonDefaults.elevatedButtonElevation(
                defaultElevation = if (selected) 8.dp else 2.dp,
            ),
        modifier = Modifier.height(56.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text)
    }
}
