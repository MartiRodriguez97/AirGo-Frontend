package com.airgo.airgoApp.features.activity.domain

import com.airgo.airgoApp.R
import com.airgo.airgoApp.core.network.RetrofitProvider
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.features.activity.data.ActivityApi
import com.airgo.airgoApp.features.activity.data.ActivityResponse
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class ActivityViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var activityApi: ActivityApi

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        activityApi = mockk()
        mockkObject(RetrofitProvider)
        mockkObject(SessionManager)
        every { RetrofitProvider.activityApi } returns activityApi
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `loadActivities from cache should update UI state with Success`() =
        runTest {
            val cachedActivities =
                listOf(
                    ActivityResponse(id = 1, distanceMeters = 1000.0, durationSeconds = 300, avgSpeedKmh = 12.0),
                )
            every { SessionManager.activities } returns cachedActivities

            val viewModel = ActivityViewModel()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value is ActivityUiState.Success)
            assertEquals(cachedActivities, (viewModel.uiState.value as ActivityUiState.Success).activities)
        }

    @Test
    fun `loadActivities from network should update UI state with Success`() =
        runTest {
            every { SessionManager.activities } returns null
            val networkActivities =
                listOf(
                    ActivityResponse(id = 1, distanceMeters = 1000.0, durationSeconds = 300, avgSpeedKmh = 12.0),
                )
            coEvery { activityApi.getActivities() } returns networkActivities
            every { SessionManager.activities = any() } returns Unit

            val viewModel = ActivityViewModel()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value is ActivityUiState.Success)
            assertEquals(networkActivities, (viewModel.uiState.value as ActivityUiState.Success).activities)
        }

    @Test
    fun `loadActivities with empty list should update UI state to Empty`() =
        runTest {
            every { SessionManager.activities } returns null
            coEvery { activityApi.getActivities() } returns emptyList()
            every { SessionManager.activities = any() } returns Unit

            val viewModel = ActivityViewModel()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value is ActivityUiState.Empty)
        }

    @Test
    fun `deleteActivity should remove activity from list and update UI state`() =
        runTest {
            val activity1 = ActivityResponse(id = 1, distanceMeters = 1000.0, durationSeconds = 300, avgSpeedKmh = 12.0)
            val activity2 = ActivityResponse(id = 2, distanceMeters = 2000.0, durationSeconds = 600, avgSpeedKmh = 12.0)
            val initialActivities = listOf(activity2, activity1) // Sorted by ID descending

            every { SessionManager.activities } returns initialActivities
            coEvery { activityApi.deleteActivity(1) } returns Response.success(null)
            every { SessionManager.activities = any() } returns Unit

            val viewModel = ActivityViewModel()
            advanceUntilIdle()

            viewModel.deleteActivity(1)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value is ActivityUiState.Success)
            val result = (viewModel.uiState.value as ActivityUiState.Success).activities
            assertEquals(1, result.size)
            assertEquals(2, result[0].id)
        }

    @Test
    fun `updateActivitySensation should update sensation in list`() =
        runTest {
            val activity =
                ActivityResponse(
                    id = 1,
                    distanceMeters = 1000.0,
                    durationSeconds = 300,
                    avgSpeedKmh = 12.0,
                    sensacio = 1,
                )
            every { SessionManager.activities } returns listOf(activity)
            coEvery { activityApi.updateActivitySensation(1, any()) } returns Response.success(null)

            val viewModel = ActivityViewModel()
            advanceUntilIdle()

            viewModel.updateActivitySensation(1, 5)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value is ActivityUiState.Success)
            val result = (viewModel.uiState.value as ActivityUiState.Success).activities
            assertEquals(5, result[0].sensacio)
        }

    @Test
    fun `updateActivityComment should update comment in list`() =
        runTest {
            val activity =
                ActivityResponse(
                    id = 1,
                    distanceMeters = 1000.0,
                    durationSeconds = 300,
                    avgSpeedKmh = 12.0,
                    comentari = "Old",
                )
            every { SessionManager.activities } returns listOf(activity)
            coEvery { activityApi.updateActivityComment(1, any()) } returns Response.success(null)

            val viewModel = ActivityViewModel()
            advanceUntilIdle()

            viewModel.updateActivityComment(1, "New")
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value is ActivityUiState.Success)
            val result = (viewModel.uiState.value as ActivityUiState.Success).activities
            assertEquals("New", result[0].comentari)
        }

    @Test
    fun `loadActivities network error should update UI state to Error`() =
        runTest {
            every { SessionManager.activities } returns null
            coEvery { activityApi.getActivities() } throws java.io.IOException()

            val viewModel = ActivityViewModel()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value is ActivityUiState.Error)
            assertEquals(R.string.error_loading_activities, (viewModel.uiState.value as ActivityUiState.Error).message)
        }
}
