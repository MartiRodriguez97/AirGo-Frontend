package com.airgo.airgoApp.features.home.domain

import com.airgo.airgoApp.core.network.RetrofitProvider
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.features.home.data.CreateTrainingPlanResponse
import com.airgo.airgoApp.features.home.data.Exercise
import com.airgo.airgoApp.features.home.data.TrainingPlanApi
import com.airgo.airgoApp.features.home.data.TrainingPlanResponse
import com.airgo.airgoApp.features.home.data.TrainingTemplate
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var mockApi: TrainingPlanApi
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockApi = mockk()
        mockkObject(RetrofitProvider)
        every { RetrofitProvider.trainingPlanApi } returns mockApi

        mockkObject(SessionManager)
        every { SessionManager.trainingPlans } returns null
        every { SessionManager.trainingPlans = any() } just Runs
        every { SessionManager.exercicis = any() } just Runs
        every { SessionManager.exerciciActiu = any() } just Runs
        every { SessionManager.nomTemplateActiu = any() } just Runs
        every { SessionManager.titolsPendentsFlow } returns kotlinx.coroutines.flow.MutableStateFlow(0)

        coEvery { mockApi.getTrainingPlans() } returns emptyList()
        coEvery { mockApi.getActivities() } returns emptyList()

        viewModel = HomeViewModel()

        clearMocks(mockApi)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun cacheLoadPlanAvailable() =
        runTest {
            val cachedPlans =
                listOf(
                    TrainingPlanResponse(
                        id = 1,
                        diesDurada = 30,
                        numEntrenamentsSetmanals = 3,
                        templates = emptyList(),
                    ),
                )
            every { SessionManager.trainingPlans } returns cachedPlans
            coEvery { mockApi.getTrainingPlans() } returns cachedPlans
            coEvery { mockApi.getActivities() } returns emptyList()

            viewModel.loadPlans()

            assertEquals(cachedPlans, viewModel.plans.value)
            coVerify(exactly = 1) { mockApi.getTrainingPlans() }
        }

    @Test
    fun cacheLoadPlanFetchAndSessionManager() =
        runTest {
            val rawPlans =
                listOf(
                    TrainingPlanResponse(
                        1,
                        30,
                        3,
                        listOf(
                            TrainingTemplate(
                                101,
                                "Test Template",
                                "Desc",
                                "Run",
                                emptyList(),
                            ),
                        ),
                    ),
                )

            coEvery { mockApi.getTrainingPlans() } returns rawPlans
            coEvery { mockApi.getActivities() } returns emptyList()

            viewModel.loadPlans()

            val expectedPlans = viewModel.plans.value
            assertEquals(1, expectedPlans.size)
            assertEquals("Test Template", expectedPlans[0].templates[0].nom)
            coVerify(exactly = 1) { mockApi.getTrainingPlans() }
            verify { SessionManager.trainingPlans = any() }
        }

    @Test
    fun loadPlanHandlerErrorNetwork() =
        runTest {
            coEvery { mockApi.getTrainingPlans() } throws
                HttpException(
                    Response.error<Any>(500, "Server Error".toResponseBody()),
                )

            viewModel.loadPlans()

            org.junit.Assert.assertNotNull(viewModel.errorMessage.value)
            coVerify(exactly = 1) { mockApi.getTrainingPlans() }
        }

    @Test
    fun filterExercisesByDate() =
        runTest {
            val today = LocalDate.now().toString()
            val yesterday = LocalDate.now().minusDays(1).toString()

            val dummyTemplate = TrainingTemplate(101, "T1", "D", "R", emptyList())

            val exerciseToday = Exercise(1, today, dummyTemplate, false, 5.0, 0.0)
            val exerciseYesterday = Exercise(2, yesterday, dummyTemplate, false, 5.0, 0.0)
            val exerciseCompleted = Exercise(3, today, dummyTemplate, true, 5.0, 0.0)

            val template =
                TrainingTemplate(
                    101,
                    "T1",
                    "D",
                    "R",
                    listOf(exerciseToday, exerciseYesterday, exerciseCompleted),
                )
            val plan =

                TrainingPlanResponse(1, 30, 3, listOf(template))

            every { SessionManager.trainingPlans } returns listOf(plan)

            viewModel.updateTodayExercises(listOf(exerciseToday, exerciseYesterday, exerciseCompleted))

            assertEquals(1, viewModel.exercises.value.size)
            assertEquals(1, viewModel.exercises.value[0].id)
        }

    @Test
    fun startExerciceSetActivInSessionManager() =
        runTest {
            val dummyTemplate = TrainingTemplate(101, "Target Template", "D", "R", emptyList())
            val exercise = Exercise(1, "2024-01-01", dummyTemplate, false, 5.0, 0.0)
            val template = TrainingTemplate(101, "Target Template", "D", "R", listOf(exercise))
            val plan =
                TrainingPlanResponse(1, 30, 3, listOf(template))

            every { SessionManager.trainingPlans } returns listOf(plan)
            coEvery { mockApi.getTrainingPlans() } returns listOf(plan)
            coEvery { mockApi.getActivities() } returns emptyList()
            viewModel.loadPlans()

            viewModel.startExercise(exercise)

            verify { SessionManager.exerciciActiu = exercise }
            verify { SessionManager.nomTemplateActiu = "Target Template" }
        }

    @Test
    fun createPlanAllStates() =
        runTest {
            val dummyPlanData =
                com.airgo.airgoApp.features.home.data.BackendPlanData(
                    id = 1,
                    diesDurada = 30,
                    numEntrenamentsSetmanals = 3,
                    esport = "CAM",
                    nivell = 1,
                    diesSetmana = listOf(1, 3, 5),
                    dataFi = "2024-12-31",
                    templates = emptyList(),
                )

            val rawResponse =
                CreateTrainingPlanResponse(
                    status = "OK",
                    plan = dummyPlanData,
                    exercicis = emptyList(),
                    numExercicisCreats = 0,
                )

            val planResponse = TrainingPlanResponse(1, 30, 3, emptyList())

            coEvery { mockApi.createTrainingPlan(any()) } returns rawResponse
            coEvery { mockApi.getTrainingPlans() } returns listOf(planResponse)
            coEvery { mockApi.getActivities() } returns emptyList()

            var successCalled = false
            viewModel.createPlan(
                onSuccess = {
                    successCalled = true
                },
                daysDuration = 30,
                workoutsPerWeek = 3,
                sport = "CAM",
                level = 1,
                daysOfWeek = listOf(1, 3, 5),
                endDate = "2024-12-31",
            )

            assertTrue(successCalled)
            coVerify { mockApi.createTrainingPlan(any()) }
            coVerify(exactly = 1) { mockApi.getTrainingPlans() }
        }

    @Test
    fun deletePlanFromSessionManager() =
        runTest {
            val plan1 =
                TrainingPlanResponse(1, 30, 3, emptyList())
            val plan2 =
                TrainingPlanResponse(2, 30, 3, emptyList())

            every { SessionManager.trainingPlans } returns listOf(plan1, plan2)
            coEvery { mockApi.getTrainingPlans() } returns listOf(plan1, plan2)
            coEvery { mockApi.getActivities() } returns emptyList()
            viewModel.loadPlans()

            coEvery { mockApi.deleteTrainingPlan(1) } just Runs

            var successCalled = false
            viewModel.deletePlan(1) {
                successCalled = true
            }

            assertTrue(successCalled)
            assertEquals(1, viewModel.plans.value.size)
            assertEquals(2, viewModel.plans.value[0].id)
            verify { SessionManager.trainingPlans = any() }
        }

    @Test
    fun clearErrorMessageToNull() =
        runTest {
            coEvery { mockApi.getTrainingPlans() } throws
                HttpException(
                    Response.error<Any>(500, "Server Error".toResponseBody()),
                )
            viewModel.loadPlans()

            org.junit.Assert.assertNotNull(viewModel.errorMessage.value)

            viewModel.clearErrorMessage()

            assertNull(viewModel.errorMessage.value)
        }
}
