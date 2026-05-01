package com.airgo.airgoApp.features.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airgo.airgoApp.R
import com.airgo.airgoApp.features.home.components.stepDateSelection
import com.airgo.airgoApp.features.home.components.stepDaysSelection
import com.airgo.airgoApp.features.home.components.stepLevelSelection
import com.airgo.airgoApp.features.home.components.stepSportSelection
import com.airgo.airgoApp.features.home.domain.HomeViewModel

internal const val STEP_SPORT = 0
internal const val STEP_LEVEL = 1
internal const val STEP_DAYS = 2
internal const val STEP_DATE = 3
private const val TOTAL_STEPS = 4
private const val MILLIS_IN_DAY = 86400000L
private const val TARGET_MAX_STEP = 3

@OptIn(ExperimentalMaterial3Api::class)
internal data class QuestionnaireFormState(
    val selectedSport: String?,
    val onSportSelected: (String) -> Unit,
    val selectedLevel: String?,
    val onLevelSelected: (String) -> Unit,
    val selectedDays: SnapshotStateList<String>,
    val datePickerState: DatePickerState,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun questionnaireScreen(
    onClose: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    var selectedSport by remember { mutableStateOf<String?>(null) }
    var selectedLevel by remember { mutableStateOf<String?>(null) }
    val selectedDays = remember { mutableStateListOf<String>() }
    var currentStep by remember { mutableStateOf(0) }
    val datePickerState = rememberDateState()

    val isNextEnabled =
        checkNextEnabled(
            currentStep,
            selectedSport,
            selectedLevel,
            selectedDays,
            datePickerState,
        )

    BackHandler(enabled = true) {
        if (currentStep > 0) currentStep-- else onClose()
    }

    Scaffold(
        topBar = {
            questionnaireTopBar(currentStep, onBack = { currentStep-- }, onClose = onClose)
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            questionnaireProgressBar(currentStep = currentStep)
            val formState =
                QuestionnaireFormState(
                    selectedSport = selectedSport,
                    onSportSelected = { selectedSport = it },
                    selectedLevel = selectedLevel,
                    onLevelSelected = { selectedLevel = it },
                    selectedDays = selectedDays,
                    datePickerState = datePickerState,
                )
            questionnaireStepContent(
                currentStep = currentStep,
                formState = formState,
                modifier = Modifier.weight(1f),
            )

            questionnaireBottomButton(
                currentStep = currentStep,
                isNextEnabled = isNextEnabled,
                onNextStep = { currentStep++ },
                onFinish = {
                    val endDateMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    val diffMillis = endDateMillis - System.currentTimeMillis()
                    val daysDuration = (diffMillis / MILLIS_IN_DAY).toInt().coerceAtLeast(0)
                    val workoutsPerWeek = selectedDays.size

                    viewModel.createPlan(
                        daysDuration = daysDuration,
                        workoutsPerWeek = workoutsPerWeek,
                        level = selectedLevel,
                        onSuccess = {
                            onClose()
                        },
                    )
                },
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun questionnaireStepContent(
    currentStep: Int,
    formState: QuestionnaireFormState,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = currentStep,
        transitionSpec = {
            if (targetState > initialState) {
                (slideInHorizontally { it } + fadeIn()).togetherWith(
                    slideOutHorizontally { -it } + fadeOut(),
                )
            } else {
                (slideInHorizontally { -it } + fadeIn()).togetherWith(
                    slideOutHorizontally { it } + fadeOut(),
                )
            }
        },
        label = "anim",
        modifier = modifier,
    ) { step ->
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when (step) {
                STEP_SPORT -> stepSportSelection(formState.selectedSport, formState.onSportSelected)
                STEP_LEVEL -> stepLevelSelection(formState.selectedLevel, formState.onLevelSelected)
                STEP_DAYS -> stepDaysSelection(formState.selectedDays)
                STEP_DATE -> stepDateSelection(formState.datePickerState)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun questionnaireTopBar(
    currentStep: Int,
    onBack: () -> Unit,
    onClose: () -> Unit,
) {
    TopAppBar(
        title = { Text(stringResource(R.string.plan_create_title)) },
        navigationIcon = {
            if (currentStep > 0) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.acc_back_button))
                }
            } else {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.acc_close))
                }
            }
        },
    )
}

@Composable
private fun questionnaireProgressBar(currentStep: Int) {
    val progress = (currentStep + 1) / TOTAL_STEPS.toFloat()
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")

    LinearProgressIndicator(
        progress = { animatedProgress },
        modifier =
            Modifier
                .fillMaxWidth()
                .height(8.dp),
    )
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun questionnaireBottomButton(
    currentStep: Int,
    isNextEnabled: Boolean,
    onNextStep: () -> Unit,
    onFinish: () -> Unit,
) {
    Button(
        onClick = {
            if (currentStep < TARGET_MAX_STEP) {
                onNextStep()
            } else {
                onFinish()
            }
        },
        enabled = isNextEnabled,
        modifier = Modifier.fillMaxWidth(),
    ) {
        // Los textos de las etiquetas SÍ pueden usar stringResource porque están en el mundo UI
        Text(
            text =
                if (currentStep == TARGET_MAX_STEP) {
                    stringResource(R.string.plan_btn_finish)
                } else {
                    stringResource(R.string.plan_btn_continue)
                },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun rememberDateState(): DatePickerState {
    return rememberDatePickerState(
        selectableDates =
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val limit = System.currentTimeMillis() - MILLIS_IN_DAY
                    return utcTimeMillis >= limit
                }

                override fun isSelectableYear(year: Int): Boolean {
                    val currentYear =
                        java.util.Calendar
                            .getInstance()
                            .get(java.util.Calendar.YEAR)
                    return year >= currentYear
                }
            },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
private fun checkNextEnabled(
    currentStep: Int,
    selectedSport: String?,
    selectedLevel: String?,
    selectedDays: SnapshotStateList<String>,
    datePickerState: DatePickerState,
): Boolean =
    when (currentStep) {
        STEP_SPORT -> selectedSport != null
        STEP_LEVEL -> selectedLevel != null
        STEP_DAYS -> selectedDays.isNotEmpty()
        STEP_DATE -> datePickerState.selectedDateMillis != null
        else -> false
    }
