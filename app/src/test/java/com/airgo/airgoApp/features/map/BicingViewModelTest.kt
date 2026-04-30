package com.airgo.airgoApp.features.map

import com.airgo.airgoApp.core.network.RetrofitProvider
import com.airgo.airgoApp.features.map.data.BicingStation
import com.airgo.airgoApp.features.map.data.GeoPoint
import com.airgo.airgoApp.features.map.domain.BicingViewModel
import io.mockk.coEvery
import io.mockk.every
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
class BicingViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: BicingViewModel

    private val mockStations =
        listOf(
            BicingStation(
                id = "1",
                name = "Estació Test",
                mechanicalBikes = 3,
                electricBikes = 1,
                freeSlots = 5,
                status = "IN_SERVICE",
                geoPoint = GeoPoint(lat = 41.385, lon = 2.173),
            ),
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkObject(RetrofitProvider)
        viewModel = BicingViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkObject(RetrofitProvider)
    }

    // region fetchStations

    @Test
    fun `fetchStations success sets stations`() =
        runTest {
            coEvery { RetrofitProvider.bicingApi.getBicingStations(any(), any(), any()) } returns mockStations

            viewModel.fetchStations(41.385, 2.173)
            advanceUntilIdle()

            assertEquals(mockStations, viewModel.stations)
        }

    @Test
    fun `fetchStations success clears errorMessage`() =
        runTest {
            coEvery { RetrofitProvider.bicingApi.getBicingStations(any(), any(), any()) } returns mockStations

            viewModel.fetchStations(41.385, 2.173)
            advanceUntilIdle()

            assertNull(viewModel.errorMessage)
        }

    @Test
    fun `fetchStations IOException sets errorMessage`() =
        runTest {
            coEvery { RetrofitProvider.bicingApi.getBicingStations(any(), any(), any()) } throws IOException()

            viewModel.fetchStations(41.385, 2.173)
            advanceUntilIdle()

            assertNotNull(viewModel.errorMessage)
        }

    @Test
    fun `fetchStations HttpException sets errorMessage`() =
        runTest {
            val httpException = mockk<HttpException>(relaxed = true)
            every { httpException.code() } returns 503
            coEvery { RetrofitProvider.bicingApi.getBicingStations(any(), any(), any()) } throws httpException

            viewModel.fetchStations(41.385, 2.173)
            advanceUntilIdle()

            assertNotNull(viewModel.errorMessage)
        }

    @Test
    fun `fetchStations IOException does not update stations`() =
        runTest {
            coEvery { RetrofitProvider.bicingApi.getBicingStations(any(), any(), any()) } throws IOException()

            viewModel.fetchStations(41.385, 2.173)
            advanceUntilIdle()

            assertTrue(viewModel.stations.isEmpty())
        }

    // endregion

    // region clearError

    @Test
    fun `clearError sets errorMessage to null`() =
        runTest {
            coEvery { RetrofitProvider.bicingApi.getBicingStations(any(), any(), any()) } throws IOException()
            viewModel.fetchStations(41.385, 2.173)
            advanceUntilIdle()
            assertNotNull(viewModel.errorMessage)

            viewModel.clearError()

            assertNull(viewModel.errorMessage)
        }

    // endregion
}
