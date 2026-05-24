package com.airgo.airgoApp.features.calendar

import android.widget.Toast
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.airgo.airgoApp.R
import com.airgo.airgoApp.features.calendar.details.calendarDetailsScreen
import com.airgo.airgoApp.features.calendar.domain.viewmodel.CalendarViewModel
import com.airgo.airgoApp.features.home.data.Exercise
import com.airgo.airgoApp.features.home.data.TrainingPlanResponse
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeParseException

private const val DAYS_IN_WEEK = 7
private const val DATE_SUBSTRING_END = 10
private const val ALPHA_BG = 0.3f
private const val ASPECT_RATIO_SQUARE = 1f
private const val ROUNDED_CORNER_RADIUS = 8
private const val PADDING_VERTICAL_CELL = 1
private const val HEIGHT_4 = 4
private const val HEIGHT_2 = 2
private const val COLOR_LIGHT_GREEN = 0xFFA5D6A7
private const val COLOR_LIGHT_RED = 0xFFEF9A9A

@Composable
fun calendarScreen(viewModel: CalendarViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "calendar_home",
    ) {
        composable(
            route = "calendar_home",
            enterTransition = { slideInHorizontally { -it } + fadeIn() },
            exitTransition = { slideOutHorizontally { -it } + fadeOut() },
        ) {
            calendarHomeContent(viewModel) { date ->
                navController.navigate("calendar_details/$date")
            }
        }

        composable(
            route = "calendar_details/{date}",
            arguments = listOf(navArgument("date") { type = NavType.StringType }),
            enterTransition = { slideInHorizontally { it } + fadeIn() },
            exitTransition = { slideOutHorizontally { -it } + fadeOut() },
            popEnterTransition = { slideInHorizontally { -it } + fadeIn() },
            popExitTransition = { slideOutHorizontally { it } + fadeOut() },
        ) { backStackEntry ->
            val dateString = backStackEntry.arguments?.getString("date") ?: ""
            val selectedDate =
                try {
                    LocalDate.parse(dateString)
                } catch (e: DateTimeParseException) {
                    LocalDate.now()
                }
            val plaList by viewModel.plaEntrenamentList.collectAsState()

            calendarDetailsScreen(
                date = selectedDate,
                plaList = plaList,
                onBackClick = { navController.popBackStack() },
            )
        }
    }
}

@Composable
fun calendarHomeContent(
    viewModel: CalendarViewModel,
    onDateSelected: (LocalDate) -> Unit,
) {
    val plaList by viewModel.plaEntrenamentList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorResId by viewModel.errorResId.collectAsState()

    val context = LocalContext.current
    val padding16 = 16.dp

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    LaunchedEffect(Unit) {
        viewModel.fetchPlaEntrenament()
    }

    LaunchedEffect(errorResId) {
        errorResId?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(padding16),
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (errorResId != null) {
            errorResId?.let {
                Text(text = stringResource(it), color = MaterialTheme.colorScheme.error)
            }
        } else {
            calendarHeader(
                currentMonth = currentMonth,
                onPreviousMonth = { currentMonth = currentMonth.minusMonths(1) },
                onNextMonth = { currentMonth = currentMonth.plusMonths(1) },
            )
            Spacer(modifier = Modifier.height(padding16))
            calendarGrid(
                modifier = Modifier.weight(1f),
                currentMonth = currentMonth,
                plaList = plaList,
                onDateSelected = onDateSelected,
            )
        }
    }
}

@Composable
fun calendarHeader(
    currentMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    val fontSize20 = 20.sp
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(onClick = onPreviousMonth) { Text(stringResource(R.string.calendar_btn_prev)) }

        val formatTextStyle = java.time.format.TextStyle.FULL
        val monthName = currentMonth.month.getDisplayName(formatTextStyle, LocalLocale.current.platformLocale)
        val formattedMonth = monthName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

        Text(
            text = "$formattedMonth ${currentMonth.year}",
            fontWeight = FontWeight.Bold,
            fontSize = fontSize20,
        )
        Button(onClick = onNextMonth) { Text(stringResource(R.string.calendar_btn_next)) }
    }
}

private const val MAX_DENSITY = 1.1f

@Composable
fun calendarGrid(
    modifier: Modifier = Modifier,
    currentMonth: YearMonth,
    plaList: List<TrainingPlanResponse>,
    onDateSelected: (LocalDate) -> Unit,
) {
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfMonth = currentMonth.atDay(1).dayOfWeek.value % DAYS_IN_WEEK
    val padding8 = 8.dp

    val allExercises =
        plaList.flatMap { pla ->
            pla.templates.flatMap { template ->
                template.exercicis.orEmpty()
            }
        }

    val currentDensity = LocalDensity.current
    val customDensity =
        Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale.coerceAtMost(MAX_DENSITY),
        )

    // 2. Envolvemos el grid
    CompositionLocalProvider(LocalDensity provides customDensity) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(DAYS_IN_WEEK),
            modifier = modifier.fillMaxWidth(),
        ) {
            val daysOfWeek =
                listOf(
                    R.string.calendar_day_sun,
                    R.string.calendar_day_mon,
                    R.string.calendar_day_tue,
                    R.string.calendar_day_wed,
                    R.string.calendar_day_thu,
                    R.string.calendar_day_fri,
                    R.string.calendar_day_sat,
                )
            items(DAYS_IN_WEEK) { index ->
                Text(
                    text = stringResource(daysOfWeek[index]),
                    modifier = Modifier.padding(padding8),
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    maxLines = 1, // 3. Obligamos a que se quede en una sola línea
                    overflow = TextOverflow.Ellipsis, // Por si acaso sigue sin caber
                )
            }

            items(firstDayOfMonth) {
                Box(modifier = Modifier.aspectRatio(ASPECT_RATIO_SQUARE))
            }

            items(daysInMonth) { day ->
                val date = currentMonth.atDay(day + 1)
                val exercisesToday =
                    allExercises.filter { exer ->
                        val exerDate =
                            if (exer.dataInici != null && exer.dataInici.length >= DATE_SUBSTRING_END) {
                                try {
                                    val dateStr = exer.dataInici.substring(0, DATE_SUBSTRING_END)
                                    LocalDate.parse(dateStr)
                                } catch (_: DateTimeParseException) {
                                    null
                                }
                            } else {
                                null
                            }

                        exerDate == date
                    }
                dayCell(
                    date = date,
                    exercises = exercisesToday,
                    onClick = { onDateSelected(date) },
                )
            }
        }
    }
}

@Composable
fun dayCell(
    date: LocalDate,
    exercises: List<Exercise>,
    onClick: () -> Unit,
) {
    val padding4 = 4.dp
    val fontSize16 = 16.sp

    androidx.compose.material3.Surface(
        onClick = onClick,
        shape = RoundedCornerShape(ROUNDED_CORNER_RADIUS.dp),
        modifier =
            Modifier
                .aspectRatio(ASPECT_RATIO_SQUARE)
                .padding(padding4),
        color = Color.LightGray.copy(alpha = ALPHA_BG),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(padding4),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = date.dayOfMonth.toString(), fontSize = fontSize16)
            Spacer(modifier = Modifier.height(padding4))
            exercises.forEach { ex ->
                val color = if (ex.completat) COLOR_LIGHT_GREEN else COLOR_LIGHT_RED
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(HEIGHT_4.dp)
                            .background(Color(color))
                            .padding(vertical = PADDING_VERTICAL_CELL.dp),
                )
                Spacer(modifier = Modifier.height(HEIGHT_2.dp))
            }
        }
    }
}
