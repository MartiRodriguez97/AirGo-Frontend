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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SignalCellularAlt1Bar
import androidx.compose.material.icons.filled.SignalCellularAlt2Bar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airgo.airgoApp.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val HALF_WIDTH = 0.5f

// Level IDs
private const val LEVEL_EASY_ID = 1
private const val LEVEL_MEDIUM_ID = 2
private const val LEVEL_ADVANCED_ID = 3

// Day IDs
private const val DAY_MONDAY = 0
private const val DAY_TUESDAY = 1
private const val DAY_WEDNESDAY = 2
private const val DAY_THURSDAY = 3
private const val DAY_FRIDAY = 4
private const val DAY_SATURDAY = 5
private const val DAY_SUNDAY = 6

@Composable
internal fun stepSportSelection(
    selectedSport: String?,
    onSportSelected: (String) -> Unit,
) {
    Text(stringResource(R.string.quest_title_sport), style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(24.dp))

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val sportRun = stringResource(R.string.sport_run)
        selectionButton(
            text = sportRun,
            icon = Icons.Default.DirectionsRun,
            selected = selectedSport == "CAM",
            onClick = { onSportSelected("CAM") },
        )

        val sportBike = stringResource(R.string.sport_bike)
        selectionButton(
            text = sportBike,
            icon = Icons.Default.DirectionsBike,
            selected = selectedSport == "BIC",
            onClick = { onSportSelected("BIC") },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun stepDateSelection(datePickerState: DatePickerState) {
    var showDialog by remember { mutableStateOf(false) }

    Text(stringResource(R.string.quest_title_date), style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(24.dp))

    val selectedDateText =
        datePickerState.selectedDateMillis?.let { millis ->
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(millis))
        } ?: stringResource(R.string.select_date_button)

    selectionButton(
        text = selectedDateText,
        icon = Icons.Default.DateRange,
        selected = datePickerState.selectedDateMillis != null,
        onClick = { showDialog = true },
    )

    if (showDialog) {
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.acc_close))
                }
            },
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false,
            )
        }
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

@Composable
internal fun stepNameSelection(
    name: String,
    onNameChange: (String) -> Unit,
) {
    Text(
        text = stringResource(R.string.question_plan_name),
        style = MaterialTheme.typography.titleMedium,
    )
    Spacer(modifier = Modifier.height(24.dp))

    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text(stringResource(R.string.default_training_plan_name)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
}
