package com.airgo.airgoApp.features.map

import com.airgo.airgoApp.core.network.RetrofitProvider
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class PollutionViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: PollutionViewModel

    private val mockStations =
        listOf(
            PollutionPoint(
                zone = "Eixample",
                geoPoint = GeoPointResponse(lat = 41.385, lon = 2.173),
                aqi = 42,
            ),
            PollutionPoint(
                zone = "Gràcia",
                geoPoint = GeoPointResponse(lat = 41.403, lon = 2.156),
                aqi = 110,
            ),
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkObject(RetrofitProvider)
        viewModel = PollutionViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkObject(RetrofitProvider)
    }

    // region fetchBarcelonaData

    @Test
    fun `fetchBarcelonaData success sets stations`() =
        runTest {
            coEvery { RetrofitProvider.airQualityApi.getPollutionData(any(), any(), any()) } returns mockStations

            viewModel.fetchBarcelonaData()
            advanceUntilIdle()

            assertEquals(mockStations, viewModel.stations)
        }

    @Test
    fun `fetchBarcelonaData success clears errorMessage`() =
        runTest {
            coEvery { RetrofitProvider.airQualityApi.getPollutionData(any(), any(), any()) } returns mockStations

            viewModel.fetchBarcelonaData()
            advanceUntilIdle()

            assertNull(viewModel.errorMessage)
        }

    @Test
    fun `fetchBarcelonaData IOException sets errorMessage`() =
        runTest {
            coEvery { RetrofitProvider.airQualityApi.getPollutionData(any(), any(), any()) } throws IOException()

            viewModel.fetchBarcelonaData()
            advanceUntilIdle()

            assertNotNull(viewModel.errorMessage)
        }

    @Test
    fun `fetchBarcelonaData HttpException sets errorMessage`() =
        runTest {
            val httpException = mockk<HttpException>(relaxed = true)
            coEvery { RetrofitProvider.airQualityApi.getPollutionData(any(), any(), any()) } throws httpException

            viewModel.fetchBarcelonaData()
            advanceUntilIdle()

            assertNotNull(viewModel.errorMessage)
        }

    @Test
    fun `fetchBarcelonaData IOException does not update stations`() =
        runTest {
            coEvery { RetrofitProvider.airQualityApi.getPollutionData(any(), any(), any()) } throws IOException()

            viewModel.fetchBarcelonaData()
            advanceUntilIdle()

            assertTrue(viewModel.stations.isEmpty())
        }

    // endregion

    // region clearError

    @Test
    fun `clearError sets errorMessage to null`() =
        runTest {
            coEvery { RetrofitProvider.airQualityApi.getPollutionData(any(), any(), any()) } throws IOException()
            viewModel.fetchBarcelonaData()
            advanceUntilIdle()
            assertNotNull(viewModel.errorMessage)

            viewModel.clearError()

            assertNull(viewModel.errorMessage)
        }

    // endregion
}
