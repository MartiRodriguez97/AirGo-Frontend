package com.airgo.airgoApp.features.map

import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.views.MapView

@RunWith(AndroidJUnit4::class)
class BicingHelperTest {
    private lateinit var mapView: MapView
    private lateinit var context: android.content.Context

    private fun makeStation(
        mechanicalBikes: Int,
        electricBikes: Int,
    ) = BicingStation(
        id = "1",
        name = "Estació Test",
        mechanicalBikes = mechanicalBikes,
        electricBikes = electricBikes,
        freeSlots = 5,
        status = "IN_SERVICE",
        geoPoint = GeoPoint(lat = 41.385, lon = 2.173),
    )

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            mapView = MapView(context)
        }
    }

    @After
    fun tearDown() {
        if (::mapView.isInitialized) {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                mapView.onDetach()
            }
        }
    }

    // region Nombre d'elements

    @Test
    fun emptyStationsReturnsEmptyOverlay() {
        val overlay = BicingHelper.createBicingOverlay(context, mapView, emptyList())

        assertTrue(overlay.items.isEmpty())
    }

    @Test
    fun overlayHasSameNumberOfMarkersAsStations() {
        val stations = listOf(makeStation(3, 1), makeStation(0, 0), makeStation(1, 0))

        val overlay = BicingHelper.createBicingOverlay(context, mapView, stations)

        assertEquals(stations.size, overlay.items.size)
    }

    // endregion

    // region Colors

    @Test
    fun stationWithBikesHasBlueMarker() {
        val expectedBlue = Color.parseColor("#0081C9")
        val station = makeStation(mechanicalBikes = 2, electricBikes = 0)

        val overlay = BicingHelper.createBicingOverlay(context, mapView, listOf(station))
        val marker = overlay.items.first() as BicingHelper.BicingMarker

        assertEquals(expectedBlue, marker.color)
    }

    @Test
    fun stationWithNoBikesHasRedMarker() {
        val expectedRed = Color.parseColor("#FF0000")
        val station = makeStation(mechanicalBikes = 0, electricBikes = 0)

        val overlay = BicingHelper.createBicingOverlay(context, mapView, listOf(station))
        val marker = overlay.items.first() as BicingHelper.BicingMarker

        assertEquals(expectedRed, marker.color)
    }

    @Test
    fun stationWithOnlyElectricBikesHasBlueMarker() {
        val expectedBlue = Color.parseColor("#0081C9")
        val station = makeStation(mechanicalBikes = 0, electricBikes = 3)

        val overlay = BicingHelper.createBicingOverlay(context, mapView, listOf(station))
        val marker = overlay.items.first() as BicingHelper.BicingMarker

        assertEquals(expectedBlue, marker.color)
    }

    // endregion
}
