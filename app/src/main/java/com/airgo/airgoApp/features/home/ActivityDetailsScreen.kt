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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.airgo.airgoApp.R
import com.airgo.airgoApp.core.network.SessionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun activityDetailScreen(
    onBackClick: () -> Unit,
    onStartActivityClick: () -> Unit,
) {
    val exercici = SessionManager.exerciciActiu
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
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.activity_today_header),
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (exercici != null) {
                Text(
                    text = "Objetivo de hoy: ${exercici.distanciaObjectiu} km",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Progreso actual: ${exercici.distanciaFeta} km",
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                Text("No hay datos de la actividad.")
            }
            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onStartActivityClick,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp),
            ) {
                Text(stringResource(R.string.btn_start))
            }
        }
    }
}
