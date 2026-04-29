package com.airgo.airgoApp.features.home

import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.airgo.airgoApp.R

@Composable
fun homeScreen(onNavigateToQuestionnaire: () -> Unit = {}) {
    val plans = TrainingPlanStore.plans

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToQuestionnaire) {
                Icon(Icons.Default.Add, contentDescription = "Crear nuevo plan")
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

            if (plans.isNotEmpty()) {
                items(plans) { plan ->
                    activePlanSection(plan)
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
fun activePlanSection(plan: TrainingPlan) {
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
                text = "Tu Plan de Entrenamiento",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Deporte: ${plan.sport}")
            Text("Nivel: ${plan.level}")
            Text("Días: ${plan.days.joinToString(", ")}")
            Text("Finaliza: ${plan.endDate}")
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
