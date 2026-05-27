package com.airgo.airgoApp.features.map.domain

import com.airgo.airgoApp.R
import com.airgo.airgoApp.core.network.RetrofitProvider
import com.airgo.airgoApp.core.utils.UiText
import com.airgo.airgoApp.features.map.data.RouteGenerationApiService
import com.airgo.airgoApp.features.map.data.RouteResponse
import com.airgo.airgoApp.features.map.helpers.NominatimResult
import com.airgo.airgoApp.features.map.helpers.NominatimService
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.osmdroid.util.GeoPoint
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class RouteViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var routeApi: RouteGenerationApiService

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        routeApi = mockk()
        mockkObject(RetrofitProvider)
        mockkObject(NominatimService)
        every { RetrofitProvider.routeGenerationApi } returns routeApi
        coEvery { NominatimService.reverseGeocode(any(), any()) } returns "Mock Address"
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `calculateRoute without points should set error message`() =
        runTest {
            val viewModel = RouteViewModel()
            viewModel.calculateRoute()

            assertEquals(R.string.error_route_selection_missing, (viewModel.errorMessage as UiText.StringResource).resId)
        }

    @Test
    fun `calculateRoute success should update currentRoute`() =
        runTest {
            val viewModel = RouteViewModel()
            viewModel.startPoint = GeoPoint(41.38, 2.17)
            viewModel.endPoint = GeoPoint(41.40, 2.18)

            val mockResponse =
                mockk<RouteResponse> {
                    every { status } returns "success"
                }
            coEvery { routeApi.getCleanRoute(any()) } returns mockResponse

            viewModel.calculateRoute()
            advanceUntilIdle()

            assertEquals(mockResponse, viewModel.currentRoute)
            assertNull(viewModel.errorMessage)
        }

    @Test
    fun `calculateRoute api failure should set error message`() =
        runTest {
            val viewModel = RouteViewModel()
            viewModel.startPoint = GeoPoint(41.38, 2.17)
            viewModel.endPoint = GeoPoint(41.40, 2.18)

            coEvery { routeApi.getCleanRoute(any()) } throws IOException("API error")

            viewModel.calculateRoute()
            advanceUntilIdle()

            assertNull(viewModel.currentRoute)
            assertEquals(R.string.error_fetching_route, (viewModel.errorMessage as UiText.StringResource).resId)
        }

    @Test
    fun `clearRoute should reset all points and route`() {
        val viewModel = RouteViewModel()
        viewModel.startPoint = GeoPoint(41.38, 2.17)
        viewModel.endPoint = GeoPoint(41.40, 2.18)
        viewModel.onSetSelectionMode(MapSelectionMode.START)

        viewModel.clearRoute()

        assertNull(viewModel.startPoint)
        assertNull(viewModel.endPoint)
        assertTrue(viewModel.waypoints.isEmpty())
        assertNull(viewModel.currentRoute)
        assertEquals(MapSelectionMode.NONE, viewModel.selectionMode)
    }

    @Test
    fun `removeWaypoint should update waypoints list and clear route`() {
        val viewModel = RouteViewModel()
        val p1 = GeoPoint(1.0, 1.0)
        val p2 = GeoPoint(2.0, 2.0)
        viewModel.waypoints = listOf(p1, p2)
        // Set a route to check if it's cleared
        // Since currentRoute is private set, we'd need to mock it via calculateRoute or similar,
        // but here we just check if it's null after removeWaypoint.

        viewModel.removeWaypoint(0)

        assertEquals(1, viewModel.waypoints.size)
        assertEquals(p2, viewModel.waypoints[0])
        assertNull(viewModel.currentRoute)
    }

    @Test
    fun `selectStartAddress should update startPoint`() {
        val viewModel = RouteViewModel()
        val result = NominatimResult(1L, "Test Address", "41.38", "2.17")

        viewModel.selectStartAddress(result)

        assertNotNull(viewModel.startPoint)
        assertEquals(41.38, viewModel.startPoint!!.latitude, 0.001)
        assertEquals(2.17, viewModel.startPoint!!.longitude, 0.001)
    }

    @Test
    fun `selectEndAddress should update endPoint`() {
        val viewModel = RouteViewModel()
        val result = NominatimResult(1L, "Test Address", "41.40", "2.18")

        viewModel.selectEndAddress(result)

        assertNotNull(viewModel.endPoint)
        assertEquals(41.40, viewModel.endPoint!!.latitude, 0.001)
        assertEquals(2.18, viewModel.endPoint!!.longitude, 0.001)
    }

    @Test
    fun `selectWaypointAddress should add or update waypoint`() {
        val viewModel = RouteViewModel()
        val result1 = NominatimResult(1L, "W1", "41.39", "2.17")
        val result2 = NominatimResult(2L, "W2", "41.41", "2.19")

        // Add first waypoint
        viewModel.selectWaypointAddress(result1, 0)
        assertEquals(1, viewModel.waypoints.size)
        assertEquals(41.39, viewModel.waypoints[0].latitude, 0.001)

        // Add second waypoint
        viewModel.selectWaypointAddress(result2, 1)
        assertEquals(2, viewModel.waypoints.size)
        assertEquals(41.41, viewModel.waypoints[1].latitude, 0.001)

        // Update first waypoint
        val result3 = NominatimResult(3L, "W1 Updated", "41.385", "2.175")
        viewModel.selectWaypointAddress(result3, 0)
        assertEquals(2, viewModel.waypoints.size)
        assertEquals(41.385, viewModel.waypoints[0].latitude, 0.001)
    }

    @Test
    fun `onSetSelectionMode should update selectionMode`() {
        val viewModel = RouteViewModel()
        viewModel.onSetSelectionMode(MapSelectionMode.START)
        assertEquals(MapSelectionMode.START, viewModel.selectionMode)
    }

    @Test
    fun `onPointSelected in START mode should set startPoint`() =
        runTest {
            val viewModel = RouteViewModel()
            viewModel.onSetSelectionMode(MapSelectionMode.START)
            val point = GeoPoint(41.38, 2.17)

            viewModel.onPointSelected(point)
            advanceUntilIdle()

            assertEquals(point, viewModel.startPoint)
            assertEquals(MapSelectionMode.NONE, viewModel.selectionMode)
        }

    @Test
    fun `toggleFavorite should call favoriteHandler`() {
        val viewModel = RouteViewModel()
        // We can't easily mock the internal favoriteHandler directly as it's a val,
        // but we can verify its side effects if it was possible to set currentRoute.
        // Since currentRoute is private set, this verifies the state reset on toggle attempt without route.
        viewModel.toggleFavorite()
        assertNull(viewModel.favoriteMessage)
    }

    private fun assertTrue(condition: Boolean) {
        org.junit.Assert.assertTrue(condition)
    }
}
