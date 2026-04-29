package com.airgo.airgoApp.features.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airgo.airgoApp.R
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.features.home.domain.HomeViewModel

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

@Composable
fun planDetailsContent(
    plan: com.airgo.airgoApp.features.home.data.TrainingPlanResponse,
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
            text = stringResource(R.string.home_plan_summary_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = stringResource(R.string.home_plan_duration, plan.diesDurada))
        Text(text = stringResource(R.string.home_plan_workouts_per_week, plan.numEntrenamentsSetmanals))

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.home_plan_workouts_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )

        plan.templates.forEach { template ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = template.nom,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = template.descripcio,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(32.dp))

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
