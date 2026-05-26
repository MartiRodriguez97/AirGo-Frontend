package com.airgo.airgoApp.features.home

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDefaults.dateFormatter
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airgo.airgoApp.R
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.features.home.data.Exercise
import com.airgo.airgoApp.features.home.data.TrainingPlanResponse
import com.airgo.airgoApp.features.home.domain.HomeViewModel
import com.airgo.airgoApp.features.home.helpers.TemplateMapper
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun planDetailsScreen(
    planId: Int,
    onBack: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val plans by viewModel.plans.collectAsState()
    val plan = plans.find { it.id == planId } ?: SessionManager.trainingPlans?.find { it.id == planId }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_plan_details_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.acc_back_button))
                    }
                },
            )
        },
    ) { padding ->
        if (plan != null) {
            planDetailsContent(
                plan = plan,
                modifier = Modifier.padding(padding).padding(16.dp),
                onFinishClick = { showDeleteDialog = true },
            )
        } else {
            // Handle plan not found
            Text(
                text = stringResource(R.string.home_plan_not_found),
                modifier = Modifier.padding(padding).padding(16.dp),
            )
        }
    }

    if (showDeleteDialog && plan != null) {
        finishPlanDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                viewModel.deletePlan(plan.id) {
                    onBack()
                }
            },
        )
    }
}

private const val MAX_LENGHT_DATE = 10

@Composable
fun planDetailsContent(
    plan: TrainingPlanResponse,
    modifier: Modifier = Modifier,
    onFinishClick: () -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = plan.nom.takeIf { !it.isNullOrBlank() } ?: stringResource(R.string.home_plan_summary_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        planSummaryCard(plan = plan)
        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.home_plan_workouts_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (plan.exercicis.isNotEmpty()) {
            val sortedExercises = plan.exercicis.sortedBy { it.dataInici ?: "" }
            sortedExercises.forEach { exercise ->
                planExerciseCard(exercise = exercise)
            }
        } else {
            Text(
                text = stringResource(R.string.no_planned_exercises),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Button(
            onClick = onFinishClick,
            modifier = Modifier.fillMaxWidth(),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                ),
        ) {
            Text(text = stringResource(R.string.home_plan_finish_btn))
        }
    }
}

@Composable
fun finishPlanDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.home_plan_delete_dialog_title))
        },
        text = {
            Text(text = stringResource(R.string.home_plan_delete_dialog_text))
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
            ) {
                Text(stringResource(R.string.home_plan_delete_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.home_plan_delete_cancel))
            }
        },
    )
}

@Composable
fun planSummaryCard(plan: TrainingPlanResponse) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            if (!plan.nom.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.home_plan_summary_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.home_plan_duration, plan.diesDurada),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DirectionsRun,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.home_plan_workouts_per_week, plan.numEntrenamentsSetmanals),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))

                val formattedEndDate =
                    plan.dataFi?.take(MAX_LENGHT_DATE)?.let { dateString ->
                        try {
                            LocalDate.parse(dateString).format(DateTimeFormatter.ofPattern("dd/MM"))
                        } catch (e: DateTimeParseException) {
                            Log.e("PlanDetailsScreen", "Error parseando la fecha final: $dateString", e)
                            dateString
                        }
                    } ?: "--/--"

                Text(
                    text = stringResource(R.string.ends_at, formattedEndDate),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
fun planExerciseCard(exercise: Exercise) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM")
    val templateCode = exercise.template?.nom ?: ""

    val titleResId =
        if (templateCode.isNotEmpty()) {
            TemplateMapper.getTitleResId(templateCode)
        } else {
            R.string.template_default_title
        }

    val descResId =
        if (templateCode.isNotEmpty()) {
            TemplateMapper.getDescResId(templateCode)
        } else {
            R.string.template_default_desc
        }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val formattedDate =
                exercise.dataInici?.take(MAX_LENGHT_DATE)?.let { dateString ->
                    try {
                        val parsedDate = LocalDate.parse(dateString)
                        parsedDate.format(dateFormatter)
                    } catch (e: DateTimeParseException) {
                        Log.e("PlanDetailsScreen", "Error parseando la fecha del ejercicio: $dateString", e)
                        dateString
                    }
                } ?: "--/--"

            Text(
                text = formattedDate,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(id = titleResId),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(id = descResId),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
