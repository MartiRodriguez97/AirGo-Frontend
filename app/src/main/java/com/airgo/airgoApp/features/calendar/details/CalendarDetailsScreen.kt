package com.airgo.airgoApp.features.calendar.details

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.airgo.airgoApp.R
import com.airgo.airgoApp.features.home.data.Exercise
import com.airgo.airgoApp.features.home.helpers.TemplateMapper
import java.time.LocalDate
import java.time.format.DateTimeParseException

private const val DATE_SUBSTRING_END = 10

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun calendarDetailsScreen(
    date: LocalDate,
    exercicisList: List<Exercise>,
    onBackClick: () -> Unit,
) {
    val padding16 = 16.dp

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
                    calendarExerciseCard(ex)
                }
            }
        }
    }
}

@Composable
fun calendarExerciseCard(ex: Exercise) {
    val padding16 = 16.dp
    val padding8 = 8.dp

    Card(
        modifier =
            Modifier
                .padding(vertical = padding8)
                .fillMaxWidth(),
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
                // agafem els noms dels titols a partir del template
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
}
