package com.airgo.airgoApp.features.map.domain

import com.airgo.airgoApp.R
import com.airgo.airgoApp.core.network.RetrofitProvider
import com.airgo.airgoApp.core.utils.UiText
import com.airgo.airgoApp.features.map.data.AirQualityApiService
import com.airgo.airgoApp.features.map.data.PollutionPoint
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException

@OptIn(ExperimentalCoroutinesApi::class)
class PollutionViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var airQualityApi: AirQualityApiService

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        airQualityApi = mockk()
        mockkObject(RetrofitProvider)
        every { RetrofitProvider.airQualityApi } returns airQualityApi
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `fetchBarcelonaData success should update stations list`() =
        runTest {
            val points =
                listOf(
                    PollutionPoint("Zone A", mockk(), 50),
                )
            coEvery { airQualityApi.getPollutionData(any(), any(), any()) } returns points

            val viewModel = PollutionViewModel()
            viewModel.fetchBarcelonaData()
            advanceUntilIdle()

            assertEquals(points, viewModel.stations)
            assertNull(viewModel.errorMessage)
        }

    @Test
    fun `fetchBarcelonaData timeout should set specific error message`() =
        runTest {
            coEvery { airQualityApi.getPollutionData(any(), any(), any()) } throws SocketTimeoutException()

            val viewModel = PollutionViewModel()
            viewModel.fetchBarcelonaData()
            advanceUntilIdle()

            assertEquals(R.string.error_pollution_timeout, (viewModel.errorMessage as UiText.StringResource).resId)
        }

    @Test
    fun `fetchBarcelonaData network error should set general error message`() =
        runTest {
            coEvery { airQualityApi.getPollutionData(any(), any(), any()) } throws IOException()

            val viewModel = PollutionViewModel()
            viewModel.fetchBarcelonaData()
            advanceUntilIdle()

            assertEquals(R.string.error_fetching_pollution, (viewModel.errorMessage as UiText.StringResource).resId)
        }

    @Test
    fun `clearError should set errorMessage to null`() =
        runTest {
            coEvery { airQualityApi.getPollutionData(any(), any(), any()) } throws IOException()
            val viewModel = PollutionViewModel()
            viewModel.fetchBarcelonaData()
            advanceUntilIdle()

            viewModel.clearError()
            assertNull(viewModel.errorMessage)
        }
}
