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
    selectedSport: String?,
    onSportSelected: (String) -> Unit,
) {
    Text(stringResource(R.string.quest_title_sport), style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(24.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        selectionButton(
            text = stringResource(R.string.sport_run),
            icon = Icons.Default.DirectionsRun,
            selected = selectedSport == stringResource(R.string.sport_run),
            onClick = { onSportSelected("Correr") },
        )
        selectionButton(
            text = stringResource(R.string.sport_bike),
            icon = Icons.Default.DirectionsBike,
            selected = selectedSport ==  stringResource(R.string.sport_bike),
            onClick = { onSportSelected("Bici") },
        )
    }
}

@Composable
internal fun stepLevelSelection(
    selectedLevel: String?,
    onLevelSelected: (String) -> Unit,
) {
    Text(stringResource(R.string.quest_title_level), style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(24.dp))
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        selectionButton(
            text = "Fácil",
            icon = Icons.Default.SignalCellularAlt1Bar,
            selected = selectedLevel == "Fácil",
            onClick = { onLevelSelected("Fácil") },
        )
        selectionButton(
            text = "Medio",
            icon = Icons.Default.SignalCellularAlt2Bar,
            selected = selectedLevel == "Medio",
            onClick = { onLevelSelected("Medio") },
        )
        selectionButton(
            text = "Avanzado",
            icon = Icons.Default.SignalCellularAlt,
            selected = selectedLevel == "Avanzado",
            onClick = { onLevelSelected("Avanzado") },
        )
    }
}

@Composable
internal fun stepDaysSelection(selectedDays: SnapshotStateList<String>) {
    val daysOfWeek =
        listOf(
            "Lunes",
            "Martes",
            "Miércoles",
            "Jueves",
            "Viernes",
            "Sábado",
            "Domingo",
        )
    Text("¿Qué días puedes entrenar?", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(24.dp))
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        daysOfWeek.forEach { day ->
            val isSelected = selectedDays.contains(day)
            FilterChip(
                selected = isSelected,
                onClick = {
                    if (isSelected) {
                        selectedDays.remove(day)
                    } else {
                        selectedDays.add(day)
                    }
                },
                label = {
                    Text(
                        text = day,
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
    Text("Selecciona la fecha de finalización", style = MaterialTheme.typography.titleMedium)
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
