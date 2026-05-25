package com.airgo.airgoApp.features.calendar.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

    @Before
    fun setUp() {
        viewModel = CalendarViewModel(apiService)
    }

    @Test
    fun testCalendarNavigation() {
        val today = LocalDate.now()
        // Ensure month and day are 2 digits for ISO format
        val monthStr = if (today.monthValue < 10) "0${today.monthValue}" else "${today.monthValue}"
        val dayStr = if (today.dayOfMonth < 10) "0${today.dayOfMonth}" else "${today.dayOfMonth}"
        val isoDate = "${today.year}-$monthStr-$dayStr"
        val dateStr = isoDate + "T10:00:00Z"

        val mockData =
            listOf(
                TrainingPlanResponse(
                    id = 1,
                    diesDurada = 30,
                    numEntrenamentsSetmanals = 3,
                    templates =
                        listOf(
                            TrainingTemplate(
                                id = 1,
                                nom = "Test Template",
                                descripcio = "Test Description",
                                tipusExercici = "CAM",
                                exercicisResposta =
                                    listOf(
                                        Exercise(
                                            id = 1,
                                            dataInici = dateStr,
                                            completat = false,
                                            distanciaObjectiu = 10.0,
                                            distanciaFeta = 0.0,
                                        ),
                                    ),
                            ),
                        ),
                ),
            )

        coEvery { apiService.getPlaEntrenament() } returns Response.success(mockData)

        composeTestRule.setContent {
            com.airgo.airgoApp.features.calendar
                .calendarScreen(viewModel = viewModel)
        }

        // Check if calendar is displayed
        composeTestRule.onNodeWithText(today.dayOfMonth.toString()).assertIsDisplayed()

        // Click on today's date
        composeTestRule.onNodeWithText(today.dayOfMonth.toString()).performClick()

        // Check if details screen is displayed
        composeTestRule.onNodeWithText("Test Template", substring = true).assertIsDisplayed()
    }
}
