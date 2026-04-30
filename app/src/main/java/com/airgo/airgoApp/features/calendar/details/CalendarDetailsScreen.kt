package com.airgo.airgoApp.features.calendar.details

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.airgo.airgoApp.R
import com.airgo.airgoApp.features.home.data.TrainingPlanResponse
import java.time.LocalDate

private const val DATE_SUBSTRING_END = 10

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun calendarDetailsScreen(
    date: LocalDate,
    plaList: List<TrainingPlanResponse>,
    onBackClick: () -> Unit,
) {
    val padding16 = 16.dp
    val padding8 = 8.dp

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
                    .padding(padding16),
        ) {
            var hasExercises = false

            plaList.forEach { pla ->
                pla.templates.forEach { template ->
                    template.exercicis.forEach { ex ->
                        val exDate =
                            if (ex.dataInici != null && ex.dataInici.length >= DATE_SUBSTRING_END) {
                                try {
                                    LocalDate.parse(ex.dataInici.substring(0, DATE_SUBSTRING_END))
                                } catch (_: java.time.format.DateTimeParseException) {
                                    null
                                }
                            } else {
                                null
                            }

                        if (exDate == date) {
                            hasExercises = true
                            Card(
                                modifier =
                                    Modifier
                                        .padding(vertical = padding8)
                                        .fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(padding16)) {
                                    Text(
                                        text =
                                            stringResource(
                                                R.string.calendar_plan,
                                                pla.diesDurada,
                                                pla.numEntrenamentsSetmanals,
                                            ),
                                    )
                                    Text(text = stringResource(R.string.calendar_template, template.nom))
                                    Text(text = template.descripcio)
                                    Text(text = stringResource(R.string.calendar_type, template.tipusExercici))
                                    Text(text = stringResource(R.string.calendar_exercise_date, ex.dataInici ?: ""))
                                    Text(text = stringResource(R.string.calendar_completed, ex.completat))

                                    ex.detallsEspecifics?.let { details ->
                                        Text(text = stringResource(R.string.calendar_details, details.toString()))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!hasExercises) {
                Text(text = stringResource(R.string.calendar_no_exercises))
            }
        }
    }
}
