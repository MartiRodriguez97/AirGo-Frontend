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
import org.osmdroid.views.overlay.Polygon

@RunWith(AndroidJUnit4::class)
class PollutionHelperTest {
    private lateinit var mapView: MapView

    private fun makeStation(
        aqi: Int,
        zone: String = "TestZone",
    ) = PollutionPoint(
        zone = zone,
        geoPoint = GeoPointResponse(lat = 41.385, lon = 2.173),
        aqi = aqi,
    )

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
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
        val overlay = PollutionHelper.createPollutionOverlay(mapView, emptyList())

        assertTrue(overlay.items.isEmpty())
    }

    @Test
    fun overlayHasSameNumberOfCirclesAsStations() {
        val stations = listOf(makeStation(30), makeStation(80), makeStation(130))

        val overlay = PollutionHelper.createPollutionOverlay(mapView, stations)

        assertEquals(stations.size, overlay.items.size)
    }

    // endregion

    // region Colors AQI

    @Test
    fun aqiGoodReturnsGreenColor() {
        val overlay = PollutionHelper.createPollutionOverlay(mapView, listOf(makeStation(aqi = 50)))
        val polygon = overlay.items.first() as Polygon

        assertEquals(Color.GREEN and 0x00FFFFFF, polygon.fillPaint.color and 0x00FFFFFF)
    }

    @Test
    fun aqiModerateReturnsYellowColor() {
        val overlay = PollutionHelper.createPollutionOverlay(mapView, listOf(makeStation(aqi = 100)))
        val polygon = overlay.items.first() as Polygon

        assertEquals(Color.YELLOW and 0x00FFFFFF, polygon.fillPaint.color and 0x00FFFFFF)
    }

    @Test
    fun aqiSensitiveReturnsOrangeColor() {
        val expectedOrange = 0xFFFFA500.toInt()
        val overlay = PollutionHelper.createPollutionOverlay(mapView, listOf(makeStation(aqi = 150)))
        val polygon = overlay.items.first() as Polygon

        assertEquals(0xFFFFA500.toInt() and 0x00FFFFFF, polygon.fillPaint.color and 0x00FFFFFF)
    }

    @Test
    fun aqiUnhealthyReturnsRedColor() {
        val overlay = PollutionHelper.createPollutionOverlay(mapView, listOf(makeStation(aqi = 151)))
        val polygon = overlay.items.first() as Polygon

        assertEquals(Color.RED and 0x00FFFFFF, polygon.fillPaint.color and 0x00FFFFFF)
    }

    // endregion

    // region Nom de l'overlay

    @Test
    fun overlayNameIsPollutionFolder() {
        val overlay = PollutionHelper.createPollutionOverlay(mapView, emptyList())

        assertEquals("PollutionFolder", overlay.name)
    }

    // endregion
}