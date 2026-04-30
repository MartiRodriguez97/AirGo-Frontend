package com.airgo.airgoApp.features.map

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.airgo.airgoApp.features.map.domain.MapSelectionMode
import com.airgo.airgoApp.features.map.helpers.createMapEventsOverlay
import com.airgo.airgoApp.features.map.helpers.isGpsEnabled
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay

@RunWith(AndroidJUnit4::class)
class MapUtilsTest {
    private lateinit var mapView: MapView
    private lateinit var context: android.content.Context

    private fun triggerTap(
        overlay: org.osmdroid.views.overlay.MapEventsOverlay,
        point: GeoPoint?,
    ) {
        val field = overlay.javaClass.getDeclaredField("mReceiver")
        field.isAccessible = true
        val receiver = field.get(overlay) as MapEventsReceiver
        receiver.singleTapConfirmedHelper(point)
    }

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

    // region createMapEventsOverlay

    @Test
    fun tapInSelectionModeCallsOnPointSelected() {
        val tapPoint = GeoPoint(41.385, 2.173)
        var selectedPoint: GeoPoint? = null

        val overlay =
            createMapEventsOverlay(
                mapView = mapView,
                bicingOverlay = FolderOverlay(),
                selectionModeProvider = { MapSelectionMode.START },
                onPointSelectedProvider = { { p -> selectedPoint = p } },
            )

        triggerTap(overlay, tapPoint)

        assertEquals(tapPoint, selectedPoint)
    }

    @Test
    fun tapInNoneModeDoesNotCallOnPointSelected() {
        val tapPoint = GeoPoint(41.385, 2.173)
        var selectedPoint: GeoPoint? = null

        val overlay =
            createMapEventsOverlay(
                mapView = mapView,
                bicingOverlay = FolderOverlay(),
                selectionModeProvider = { MapSelectionMode.NONE },
                onPointSelectedProvider = { { p -> selectedPoint = p } },
            )

        triggerTap(overlay, tapPoint)

        assertNull(selectedPoint)
    }

    @Test
    fun tapWithNullPointDoesNotCallOnPointSelected() {
        var selectedPoint: GeoPoint? = null

        val overlay =
            createMapEventsOverlay(
                mapView = mapView,
                bicingOverlay = FolderOverlay(),
                selectionModeProvider = { MapSelectionMode.START },
                onPointSelectedProvider = { { p -> selectedPoint = p } },
            )

        triggerTap(overlay, null)

        assertNull(selectedPoint)
    }

    @Test
    fun tapInEndModeCallsOnPointSelected() {
        val tapPoint = GeoPoint(41.390, 2.180)
        var selectedPoint: GeoPoint? = null

        val overlay =
            createMapEventsOverlay(
                mapView = mapView,
                bicingOverlay = FolderOverlay(),
                selectionModeProvider = { MapSelectionMode.END },
                onPointSelectedProvider = { { p -> selectedPoint = p } },
            )

        triggerTap(overlay, tapPoint)

        assertEquals(tapPoint, selectedPoint)
    }

    // endregion

    // region isGpsEnabled

    @Test
    fun isGpsEnabledReturnsWithoutCrashing() {
        val result = isGpsEnabled(context)
        assertNotNull(result)
    }

    // endregion
}
