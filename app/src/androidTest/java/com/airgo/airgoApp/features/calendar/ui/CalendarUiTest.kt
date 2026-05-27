package com.airgo.airgoApp.features.calendar.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToLog
import com.airgo.airgoApp.features.calendar.calendarScreen
import com.airgo.airgoApp.features.calendar.data.remote.CalendarApiService
import com.airgo.airgoApp.features.calendar.domain.viewmodel.CalendarViewModel
import com.airgo.airgoApp.features.home.data.Exercise
import com.airgo.airgoApp.features.home.data.TrainingPlanResponse
import com.airgo.airgoApp.features.home.data.TrainingTemplate
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import java.time.LocalDate

class CalendarUiTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val apiService = mockk<CalendarApiService>(relaxed = true)
    private lateinit var viewModel: CalendarViewModel
    private lateinit var context: android.content.Context

    @Before
    fun setUp() {
        context =
            androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation()
                .targetContext
        viewModel = CalendarViewModel(apiService)
    }

    @Test
    fun testCalendarNavigation() {
        val today = LocalDate.now()
        val dateStr = today.toString() + "T10:00:00Z"

        val template =
            TrainingTemplate(
                id = 1,
                nom = "RUN_EASY_1",
                descripcio = "Test Description",
                tipusExercici = "CAM",
                exercicisResposta = emptyList(),
            )

        val exercise =
            Exercise(
                id = 1,
                dataInici = dateStr,
                completat = false,
                distanciaObjectiu = 10.0,
                distanciaFeta = 0.0,
                template = template,
            )

        val mockData =
            listOf(
                TrainingPlanResponse(
                    id = 1,
                    diesDurada = 30,
                    numEntrenamentsSetmanals = 3,
                    templates = listOf(template),
                    exercicis = listOf(exercise),
                ),
            )

        coEvery { apiService.getPlaEntrenament() } returns Response.success(mockData)

        composeTestRule.setContent {
            calendarScreen(viewModel = viewModel)
        }

        // Check if calendar is displayed
        composeTestRule.onNodeWithText(today.dayOfMonth.toString()).assertIsDisplayed()

        // Click on today's date
        composeTestRule.onNodeWithText(today.dayOfMonth.toString()).performClick()

        composeTestRule.waitForIdle()
        composeTestRule.onRoot().printToLog("CALENDAR_UI_TEST")

        // Check if details screen is displayed
        val expectedTitle = context.getString(com.airgo.airgoApp.R.string.template_run_easy_1_title)
        composeTestRule.onNodeWithText(expectedTitle, substring = true).assertIsDisplayed()
    }
}
