package com.airgo.airgoApp.features.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SignalCellularAlt1Bar
import androidx.compose.material.icons.filled.SignalCellularAlt2Bar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TOTAL_STEPS = 4
private const val MILLIS_IN_DAY = 86400000L
private const val HALF_WIDTH = 0.5f

private const val STEP_SPORT = 0
private const val STEP_LEVEL = 1
private const val STEP_DAYS = 2
private const val STEP_DATE = 3
private const val TARGET_MAX_STEP = 3

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun questionnaireScreen(
    onPlanCreated: () -> Unit,
    onClose: () -> Unit,
) {
    var selectedSport by remember { mutableStateOf<String?>(null) }
    var selectedLevel by remember { mutableStateOf<String?>(null) }
    val selectedDays = remember { mutableStateListOf<String>() }
    var endDate by remember { mutableStateOf("") }
    var currentStep by remember { mutableStateOf(0) }

    val datePickerState = rememberDateState()

    val isNextEnabled = checkNextEnabled(
        currentStep,
        selectedSport,
        selectedLevel,
        selectedDays,
        datePickerState
    )

    BackHandler(enabled = true) {
        if (currentStep > 0) currentStep-- else onClose()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear Plan") },
                navigationIcon = {
                    if (currentStep > 0) {
                        IconButton(onClick = { currentStep-- }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                        }
                    } else {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val progress = (currentStep + 1) / TOTAL_STEPS.toFloat()
            val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
            )
            Spacer(modifier = Modifier.height(24.dp))

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { it } + fadeIn()).togetherWith(
                            slideOutHorizontally { -it } + fadeOut(),
                        )
                    } else {
                        (slideInHorizontally { -it } + fadeIn()).togetherWith(
                            slideOutHorizontally { it } + fadeOut(),
                        )
                    }
                },
                label = "anim",
                modifier = Modifier.weight(1f),
            ) { step ->
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    when (step) {
                        STEP_SPORT -> stepSportSelection(selectedSport) { selectedSport = it }
                        STEP_LEVEL -> stepLevelSelection(selectedLevel) { selectedLevel = it }
                        STEP_DAYS -> stepDaysSelection(selectedDays)
                        STEP_DATE -> stepDateSelection(datePickerState)
                    }
                }
            }

            Button(
                onClick = {
                    if (currentStep < TARGET_MAX_STEP) {
                        currentStep++
                    } else {
                        val millis = datePickerState.selectedDateMillis
                        val formattedDate = if (millis == null) "" else {
                            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            formatter.format(Date(millis))
                        }
                        savePlan(selectedSport, selectedLevel, selectedDays, formattedDate)
                        onPlanCreated()
                    }
                },
                enabled = isNextEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (currentStep == TARGET_MAX_STEP) "Finalizar" else "Continuar")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun rememberDateState(): DatePickerState {
    return rememberDatePickerState(
        selectableDates =
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val limit = System.currentTimeMillis() - MILLIS_IN_DAY
                    return utcTimeMillis >= limit
                }

                override fun isSelectableYear(year: Int): Boolean {
                    val currentYear =
                        java.util.Calendar
                            .getInstance()
                            .get(java.util.Calendar.YEAR)
                    return year >= currentYear
                }
            },
    )
}

private fun savePlan(
    sport: String?,
    level: String?,
    days: List<String>,
    endDate: String,
) {
    TrainingPlanStore.plans.add(
        TrainingPlan(
            sport = sport ?: "",
            level = level ?: "",
            days = days,
            endDate = if (endDate.isBlank()) "Sin especificar" else endDate,
        ),
    )
}

@Composable
private fun stepSportSelection(
    selectedSport: String?,
    onSportSelected: (String) -> Unit,
) {
    Text("¿Cuál es tu modalidad preferida?", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(24.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        selectionButton(
            text = "Correr",
            icon = Icons.Default.DirectionsRun,
            selected = selectedSport == "Correr",
            onClick = { onSportSelected("Correr") },
        )
        selectionButton(
            text = "Bici",
            icon = Icons.Default.DirectionsBike,
            selected = selectedSport == "Bici",
            onClick = { onSportSelected("Bici") },
        )
    }
}

@Composable
private fun stepLevelSelection(
    selectedLevel: String?,
    onLevelSelected: (String) -> Unit,
) {
    Text("¿Cuál es tu nivel actual?", style = MaterialTheme.typography.titleMedium)
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
private fun stepDaysSelection(selectedDays: SnapshotStateList<String>) {
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
private fun stepDateSelection(datePickerState: DatePickerState) {
    Text("Selecciona la fecha de finalización", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(16.dp))
    DatePicker(
        state = datePickerState,
        modifier = Modifier.fillMaxWidth(),
        showModeToggle = false,
    )
}

@Composable
fun selectionButton(
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

private fun checkNextEnabled(
    currentStep: Int,
    selectedSport: String?,
    selectedLevel: String?,
    selectedDays: SnapshotStateList<String>,
    datePickerState: DatePickerState
): Boolean {
    return when (currentStep) {
        STEP_SPORT -> selectedSport != null
        STEP_LEVEL -> selectedLevel != null
        STEP_DAYS -> selectedDays.isNotEmpty()
        STEP_DATE -> datePickerState.selectedDateMillis != null
        else -> false
    }
}
