package com.airgo.airgoApp.features.home

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airgo.airgoApp.R
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.features.home.data.Exercise
import com.airgo.airgoApp.features.home.data.TrainingPlanResponse
import com.airgo.airgoApp.features.home.domain.HomeViewModel
import com.airgo.airgoApp.features.tracking.components.titleSelectionDialog

@Composable
fun pendingTitlesCard(
    count: Int,
    onClick: () -> Unit,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 16.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Títols Pendents!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (count == 1) "Tens 1 títol per triar" else "Tens $count títols per triar",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            TextButton(onClick = onClick) {
                Text("TRIAR")
            }
        }
    }
}

@Composable
fun homeScreen(
    onNavigateToQuestionnaire: () -> Unit = {},
    onNavigateToDetails: (Int) -> Unit = {},
    onNavigateToPendingActivity: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(),
) {
    val plans by viewModel.plans.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val titolsPendents by viewModel.titolsPendents.collectAsState()
    val availableTitles by viewModel.availableTitles.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadPlans()
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { resId ->
            Toast.makeText(context, resId, Toast.LENGTH_SHORT).show()
            viewModel.clearErrorMessage()
        }
    }

    if (viewModel.showTitleSelectionDialog) {
        titleSelectionDialog(
            options = availableTitles,
            onSelect = { title, setAsMain ->
                viewModel.unlockTitle(title.id, setAsMain)
            },
            onDismiss = {
                viewModel.showTitleSelectionDialog = false
            },
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToQuestionnaire) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.plan_create_title),
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (titolsPendents > 0) {
                item {
                    pendingTitlesCard(
                        count = titolsPendents,
                        onClick = { viewModel.fetchAvailableTitles() },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            item {
                pendingActivitySection(
                    viewModel = viewModel,
                    exercises = exercises,
                    onNavigateToPendingActivity = onNavigateToPendingActivity,
                )
            }

            if (plans.isNotEmpty()) {
                items(plans) { plan ->
                    activePlanSection(plan, onClick = { onNavigateToDetails(plan.id) })
                    Spacer(modifier = Modifier.height(16.dp))
                }
            } else {
                item {
                    // secció de entrament
                    trainingPlanSection(onCreatePlanClick = onNavigateToQuestionnaire)
                }
            }
        }
    }
}

@Composable
fun activePlanSection(
    plan: TrainingPlanResponse,
    onClick: () -> Unit,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.DirectionsRun,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(
                text = stringResource(R.string.home_active_plan_label),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = stringResource(R.string.home_plan_duration, plan.diesDurada))
            Text(text = stringResource(R.string.home_plan_workouts_per_week, plan.numEntrenamentsSetmanals))

            val nextExercise = plan.templates.flatMap { it.exercicis.orEmpty() }.firstOrNull { !it.completat }
            val nextTitle =
                plan.templates.firstOrNull { template -> template.exercicis.orEmpty().any { it == nextExercise } }?.nom
                    ?: stringResource(R.string.home_plan_no_next_workout)

            Text(text = stringResource(R.string.home_plan_next_workout, nextTitle))
        }
    }
}

@Composable
fun trainingPlanSection(onCreatePlanClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.DirectionsRun,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Text(
                text = stringResource(R.string.home_training_plan_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.home_training_plan_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onCreatePlanClick,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(text = stringResource(R.string.home_btn_create_plan))
            }
        }
    }
}

@Composable
fun pendingActivitySection(
    viewModel: HomeViewModel,
    exercises: List<Exercise>,
    onNavigateToPendingActivity: () -> Unit,
) {
    if (exercises.isNotEmpty()) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                text = stringResource(R.string.home_today_task_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            exercises.forEach { exercise ->
                val template =
                    SessionManager.trainingPlans
                        ?.flatMap { it.templates }
                        ?.find { it.exercicis.orEmpty().any { ex -> ex.id == exercise.id } }
                val templateName = template?.nom ?: stringResource(R.string.home_pending_activity_card)

                ElevatedCard(
                    onClick = {
                        viewModel.startExercise(exercise)
                        onNavigateToPendingActivity()
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                ) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.DirectionsRun,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        Text(
                            text = templateName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.home_pending_activity_objective, exercise.distanciaObjectiu),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (exercise.distanciaFeta > 0) {
                            Text(
                                text = stringResource(R.string.home_pending_activity_progress, exercise.distanciaFeta),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                }
            }
        }
    }
}
