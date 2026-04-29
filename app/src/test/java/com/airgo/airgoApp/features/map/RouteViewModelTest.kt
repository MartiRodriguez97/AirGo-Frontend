package com.airgo.airgoApp.features.map

import com.airgo.airgoApp.core.network.RetrofitProvider
import com.airgo.airgoApp.core.utils.UiText
import com.airgo.airgoApp.features.map.data.RouteGeometry
import com.airgo.airgoApp.features.map.data.RouteResponse
import com.airgo.airgoApp.features.map.data.RouteSummary
import com.airgo.airgoApp.features.map.domain.MapSelectionMode
import com.airgo.airgoApp.features.map.domain.RouteViewModel
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.osmdroid.util.GeoPoint
import retrofit2.HttpException
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class RouteViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: RouteViewModel

    private val startPoint = GeoPoint(41.385, 2.173)
    private val endPoint = GeoPoint(41.390, 2.180)

    private val mockRoute =
        RouteResponse(
            status = "ok",
            routeId = 1,
            summary =
                RouteSummary(
                    durationMinutes = 10.0,
                    distanceMeters = 1500.0,
                    durationSeconds = 600,
                    aqiStationsDetected = 2,
                ),
            geometry =
                RouteGeometry(
                    type = "LineString",
                    coordinates = listOf(listOf(41.38, 2.17), listOf(41.39, 2.18)),
                ),
            pollutionDetails = emptyList(),
            stationsInfo = emptyList(),
            routeId = TODO(),
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkObject(RetrofitProvider)
        viewModel = RouteViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkObject(RetrofitProvider)
    }

    // region calculateRoute

    @Test
    fun `calculateRoute with null start sets errorMessage`() {
        viewModel.endPoint = endPoint

        viewModel.calculateRoute()

        assertNotNull(viewModel.errorMessage)
    }

    @Test
    fun `calculateRoute with null end sets errorMessage`() {
        viewModel.startPoint = startPoint

        viewModel.calculateRoute()

        assertNotNull(viewModel.errorMessage)
    }

    @Test
    fun `calculateRoute with both points calls API and sets currentRoute`() =
        runTest {
            coEvery { RetrofitProvider.routeGenerationApi.getCleanRoute(any()) } returns mockRoute
            viewModel.startPoint = startPoint
            viewModel.endPoint = endPoint

            viewModel.calculateRoute()
            advanceUntilIdle()

            assertEquals(mockRoute, viewModel.currentRoute)
        }

    @Test
    fun `calculateRoute success sets isLoading to false`() =
        runTest {
            coEvery { RetrofitProvider.routeGenerationApi.getCleanRoute(any()) } returns mockRoute
            viewModel.startPoint = startPoint
            viewModel.endPoint = endPoint

            viewModel.calculateRoute()
            advanceUntilIdle()

            assertFalse(viewModel.isLoading)
        }

    @Test
    fun `calculateRoute IOException sets connection errorMessage`() =
        runTest {
            coEvery { RetrofitProvider.routeGenerationApi.getCleanRoute(any()) } throws IOException()
            viewModel.startPoint = startPoint
            viewModel.endPoint = endPoint

            viewModel.calculateRoute()
            advanceUntilIdle()

            assertNotNull(viewModel.errorMessage)
            assertTrue(viewModel.errorMessage is UiText.StringResource)
            assertFalse(viewModel.isLoading)
        }

    @Test
    fun `calculateRoute HttpException sets server errorMessage with code`() =
        runTest {
            val httpException = mockk<HttpException>(relaxed = true)
            every { httpException.code() } returns 503
            coEvery { RetrofitProvider.routeGenerationApi.getCleanRoute(any()) } throws httpException
            viewModel.startPoint = startPoint
            viewModel.endPoint = endPoint

            viewModel.calculateRoute()
            advanceUntilIdle()

            assertNotNull(viewModel.errorMessage)
            assertTrue(viewModel.errorMessage is UiText.StringResource)
            assertFalse(viewModel.isLoading)
        }

    // endregion

    // region onPointSelected

    @Test
    fun `onPointSelected START mode sets startPoint and resets mode to NONE`() {
        viewModel.selectionMode = MapSelectionMode.START

        viewModel.onPointSelected(startPoint)

        assertEquals(startPoint, viewModel.startPoint)
        assertEquals(MapSelectionMode.NONE, viewModel.selectionMode)
    }

    @Test
    fun `onPointSelected START mode clears currentRoute`() =
        runTest {
            coEvery { RetrofitProvider.routeGenerationApi.getCleanRoute(any()) } returns mockRoute
            viewModel.startPoint = startPoint
            viewModel.endPoint = endPoint
            viewModel.calculateRoute()
            advanceUntilIdle()

            viewModel.selectionMode = MapSelectionMode.START
            viewModel.onPointSelected(GeoPoint(41.0, 2.0))

            assertNull(viewModel.currentRoute)
        }

    @Test
    fun `onPointSelected END mode sets endPoint and resets mode to NONE`() {
        viewModel.selectionMode = MapSelectionMode.END

        viewModel.onPointSelected(endPoint)

        assertEquals(endPoint, viewModel.endPoint)
        assertEquals(MapSelectionMode.NONE, viewModel.selectionMode)
    }

    @Test
    fun `onPointSelected END mode clears currentRoute`() =
        runTest {
            coEvery { RetrofitProvider.routeGenerationApi.getCleanRoute(any()) } returns mockRoute
            viewModel.startPoint = startPoint
            viewModel.endPoint = endPoint
            viewModel.calculateRoute()
            advanceUntilIdle()

            viewModel.selectionMode = MapSelectionMode.END
            viewModel.onPointSelected(GeoPoint(41.0, 2.0))

            assertNull(viewModel.currentRoute)
        }

    @Test
    fun `onPointSelected NONE mode does not change startPoint`() {
        viewModel.startPoint = startPoint
        viewModel.selectionMode = MapSelectionMode.NONE

        viewModel.onPointSelected(GeoPoint(41.0, 2.0))

        assertEquals(startPoint, viewModel.startPoint)
    }

    // endregion

    // region clearRoute

    @Test
    fun `clearRoute resets all state to initial values`() =
        runTest {
            coEvery { RetrofitProvider.routeGenerationApi.getCleanRoute(any()) } returns mockRoute
            viewModel.startPoint = startPoint
            viewModel.endPoint = endPoint
            viewModel.calculateRoute()
            advanceUntilIdle()

            viewModel.clearRoute()

            assertNull(viewModel.startPoint)
            assertNull(viewModel.endPoint)
            assertNull(viewModel.currentRoute)
            assertEquals(MapSelectionMode.NONE, viewModel.selectionMode)
        }

    // endregion

    // region clearError

    @Test
    fun `clearError sets errorMessage to null`() {
        viewModel.calculateRoute() // sense punts → genera error
        assertNotNull(viewModel.errorMessage)

        viewModel.clearError()

        assertNull(viewModel.errorMessage)
    }

    // endregion
}
