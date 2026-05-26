package com.airgo.airgoApp.features.calendar.details

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.airgo.airgoApp.R
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.features.home.data.Exercise
import com.airgo.airgoApp.features.home.helpers.TemplateMapper
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

private const val DATE_SUBSTRING_END = 10

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun calendarDetailsScreen(
    date: LocalDate,
    exercicisList: List<Exercise>,
    onBackClick: () -> Unit,
    onMoveActivity: (Int, String) -> Unit,
) {
    val padding16 = 16.dp
    var exerciseToMove by remember { mutableStateOf<Exercise?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.calendar_details_for, date.toString())) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.calendar_back_desc),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(padding16),
        ) {
            val exercisesForToday =
                exercicisList.filter { ex ->
                    if (ex.dataInici != null && ex.dataInici.length >= DATE_SUBSTRING_END) {
                        try {
                            val dateStr = ex.dataInici.substring(0, DATE_SUBSTRING_END)
                            LocalDate.parse(dateStr) == date
                        } catch (_: DateTimeParseException) {
                            false
                        }
                    } else {
                        false
                    }
                }

            if (exercisesForToday.isEmpty()) {
                Text(text = stringResource(R.string.calendar_no_exercises))
            } else {
                exercisesForToday.forEach { ex ->
                    calendarExerciseCard(
                        ex,
                        onMoveClick = {
                            // Al hacer clic, guardamos el ejercicio y abrimos el diálogo
                            exerciseToMove = ex
                            showDatePicker = true
                        },
                    )
                }
            }
        }
        if (showDatePicker && exerciseToMove != null) {
            val plan = SessionManager.trainingPlans?.find { it.id == exerciseToMove?.pla }
            val dataFiMillis =
                remember(plan?.dataFi) {
                    plan?.dataFi?.take(DATE_SUBSTRING_END)?.let { dateStr ->
                        try {
                            LocalDate
                                .parse(dateStr)
                                .atStartOfDay(ZoneOffset.UTC)
                                .toInstant()
                                .toEpochMilli()
                        } catch (e: DateTimeParseException) {
                            null
                        }
                    }
                }

            val datePickerState =
                rememberDatePickerState(
                    selectableDates =
                        object : SelectableDates {
                            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                                val limitPasado = System.currentTimeMillis() - 86400000L
                                val isPastBlocked = utcTimeMillis >= limitPasado
                                val isBeforeDataFi = if (dataFiMillis != null) utcTimeMillis <= dataFiMillis else true
                                return isPastBlocked && isBeforeDataFi
                            }
                        },
                )

            DatePickerDialog(
                onDismissRequest = {
                    showDatePicker = false
                    exerciseToMove = null
                },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val instant = Instant.ofEpochMilli(millis)
                            val newDateIso = instant.toString()

                            // Enviamos la petición al ViewModel
                            onMoveActivity(exerciseToMove!!.id, newDateIso)

                            showDatePicker = false
                            exerciseToMove = null
                        }
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDatePicker = false
                        exerciseToMove = null
                    }) {
                        Text(stringResource(R.string.acc_close))
                    }
                },
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}

@Composable
fun calendarExerciseCard(
    ex: Exercise,
    onMoveClick: () -> Unit,
) {
    val padding16 = 16.dp
    val padding8 = 8.dp

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = padding8),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(padding16)) {
                val planName = ex.nomPla?.nom
                if (!planName.isNullOrBlank()) {
                    Text(
                        text = planName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
                val template = ex.template

                if (template != null) {
                    val titleResId = TemplateMapper.getTitleResId(template.nom)
                    val descResId = TemplateMapper.getDescResId(template.nom)

                    Text(
                        text = stringResource(id = titleResId),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )

                    Text(
                        text = stringResource(id = descResId),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )

                    val sportString =
                        when (template.tipusExercici) {
                            "CAM" -> stringResource(R.string.sport_run)
                            "BIC" -> stringResource(R.string.sport_bike)
                            else -> template.tipusExercici
                        }

                    Text(
                        text = stringResource(R.string.calendar_type, sportString),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.template_default_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onMoveClick,
            modifier = Modifier.align(Alignment.End),
        ) {
            Text(text = stringResource(R.string.activity_btn_move))
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
