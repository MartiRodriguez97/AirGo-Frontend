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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.airgo.airgoApp.R
import com.airgo.airgoApp.features.home.data.Exercise
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
                    .verticalScroll(rememberScrollState())
                    .padding(padding16),
        ) {
            Text(
                text = "DEBUG: He rebut ${exercicisList.size} exercicis en total.",
                color = androidx.compose.ui.graphics.Color.Red,
            )
            val exercisesForToday =
                exercicisList.filter { ex ->
                    if (ex.dataInici != null && ex.dataInici.length >= DATE_SUBSTRING_END) {
                        try {
                            val dateStr = ex.dataInici.substring(0, DATE_SUBSTRING_END)
                            println("Comparando JSON: $dateStr con DIA CLICADO: $date")
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
                // 3. Si hay, pintamos una Card por cada uno
                exercisesForToday.forEach { ex ->
                    Card(
                        modifier =
                            Modifier
                                .padding(vertical = padding8)
                                .fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(padding16)) {
                            val template = ex.template

                            if (template != null) {
                                // 1. Obtenemos los IDs de los textos usando el "nom" (que ahora es nuestro código)
                                val titleResId =
                                    com.airgo.airgoApp.features.home.helpers.TemplateMapper
                                        .getTitleResId(template.nom)
                                val descResId =
                                    com.airgo.airgoApp.features.home.helpers.TemplateMapper
                                        .getDescResId(template.nom)

                                // 2. Pintamos los textos traduciéndolos con stringResource
                                Text(text = stringResource(R.string.calendar_template, stringResource(titleResId)))
                                Text(text = stringResource(descResId))
                                Text(text = stringResource(R.string.calendar_type, template.tipusExercici))
                            } else {
                                Text(text = "Activitat Planificada") // Fallback
                            }
                        }
                    }
                }
            }
        }
    }
}
