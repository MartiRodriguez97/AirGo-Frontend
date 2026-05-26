package com.airgo.airgoApp.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airgo.airgoApp.R
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.features.home.domain.HomeViewModel
import com.airgo.airgoApp.features.home.helpers.TemplateMapper
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

private const val SENCS_IN_A_DAY = 86400000L

private const val MAX_DATE_LENGTH = 10

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun activityDetailScreen(
    onBackClick: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val exercici = SessionManager.exerciciActiu
    var showDatePicker by remember { mutableStateOf(false) }
    val plan = SessionManager.trainingPlans?.find { it.id == exercici?.pla }

    val datePickerState = rememberActivityDateState(plan?.dataFi)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.activity_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.acc_back_button),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            if (exercici != null) {
                activityDetailContent(
                    templateCode = exercici.template?.nom ?: "",
                    planName = exercici.nomPla?.nom ?: plan?.nom,
                )

                Spacer(modifier = Modifier.height(40.dp))
                Button(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(R.string.activity_btn_move))
                }

                if (showDatePicker) {
                    moveActivityDialog(
                        datePickerState = datePickerState,
                        onDismiss = { showDatePicker = false },
                        onConfirm = { millis ->
                            val instant = Instant.ofEpochMilli(millis)
                            val newDateIso = instant.toString()
                            viewModel.updateExerciseDate(exercici.id, newDateIso) {
                                showDatePicker = false
                                onBackClick()
                            }
                        },
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(32.dp))
                Text(stringResource(R.string.no_activity_data))
            }
        }
    }
}

@Composable
private fun activityDetailContent(
    templateCode: String,
    planName: String?,
) {
    val titleResId =
        if (templateCode.isNotEmpty()) {
            TemplateMapper.getTitleResId(templateCode)
        } else {
            R.string.home_pending_activity_card
        }

    val descResId =
        if (templateCode.isNotEmpty()) {
            TemplateMapper.getDescResId(templateCode)
        } else {
            R.string.template_default_desc
        }

    Spacer(modifier = Modifier.height(16.dp))

    if (!planName.isNullOrBlank()) {
        Text(
            text = planName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
    }

    Text(
        text = stringResource(id = titleResId),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = stringResource(id = descResId),
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun rememberActivityDateState(planDataFi: String?): DatePickerState {
    val dataFiMillis =
        remember(planDataFi) {
            planDataFi?.take(MAX_DATE_LENGTH)?.let { dateStr ->
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

    return rememberDatePickerState(
        selectableDates =
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val limitPasado = System.currentTimeMillis() - SENCS_IN_A_DAY
                    val isPastBlocked = utcTimeMillis >= limitPasado
                    val isBeforeDataFi = if (dataFiMillis != null) utcTimeMillis <= dataFiMillis else true
                    return isPastBlocked && isBeforeDataFi
                }
            },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun moveActivityDialog(
    datePickerState: DatePickerState,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { onConfirm(it) }
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.acc_close))
            }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}
