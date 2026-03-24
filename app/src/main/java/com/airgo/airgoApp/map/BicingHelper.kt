package com.airgo.airgoApp.map

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.LayerDrawable
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.airgo.airgoApp.R
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.Marker

object BicingHelper {
    private const val BICING_BLUE = "#0081C9"
    private const val EMPTY_RED = "#FF0000"
    private const val MARKER_ALPHA = 100
    private const val IN_SERVICE = "IN_SERVICE"
    private const val PL_CATALUNYA_LAT = 41.3871
    private const val PL_CATALUNYA_LON = 2.1701

    private const val ARC_TRIOMF_LAT = 41.3914
    private const val ARC_TRIOMF_LON = 2.1806

    private const val SAGRADA_FAMILIA_LAT = 41.4036
    private const val SAGRADA_FAMILIA_LON = 2.1744

    private val PL_CATALUNYA_LOCATION = GeoPoint(PL_CATALUNYA_LAT, PL_CATALUNYA_LON)
    private val ARC_TRIOMF_LOCATION = GeoPoint(ARC_TRIOMF_LAT, ARC_TRIOMF_LON)
    private val SAGRADA_FAMILIA_LOCATION = GeoPoint(SAGRADA_FAMILIA_LAT, SAGRADA_FAMILIA_LON)
    private val mockBicingStations =
        listOf(
            BicingStation(
                id = "1",
                name = "Pl. Catalunya",
                geoPoint = PL_CATALUNYA_LOCATION,
                mechanicalBikes = 10,
                electricBikes = 5,
                freeSlots = 12,
                status = IN_SERVICE,
            ),
            BicingStation(
                id = "2",
                name = "Arc de Triomf",
                geoPoint = ARC_TRIOMF_LOCATION,
                mechanicalBikes = 2,
                electricBikes = 1,
                freeSlots = 25,
                status = IN_SERVICE,
            ),
            BicingStation(
                id = "3",
                name = "Sagrada Família",
                geoPoint = SAGRADA_FAMILIA_LOCATION,
                mechanicalBikes = 0,
                electricBikes = 0,
                freeSlots = 30,
                status = IN_SERVICE,
            ),
        )

    fun createBicingOverlay(
        context: Context,
        map: MapView,
    ): FolderOverlay {
        val folder = FolderOverlay()

        mockBicingStations.forEach { station ->
            val hasBikes = (station.mechanicalBikes + station.electricBikes) > 0
            val baseColor = Color.parseColor(if (hasBikes) BICING_BLUE else EMPTY_RED)

            val marker = BicingMarker(map, context, baseColor)
            marker.position = station.geoPoint
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

            marker.title = station.name
            marker.snippet =
                """
                🚲 Mecàniques: ${station.mechanicalBikes}
                ⚡ Elèctriques: ${station.electricBikes}
                🅿️ Llocs lliures: ${station.freeSlots}
                """.trimIndent()

            marker.setOnMarkerClickListener { m, _ ->
                // Deseleccionar els que abans s'han pres
                folder.items.forEach { item ->
                    if (item is BicingMarker) item.setSelected(false, map)
                }
                // Seleccionar l'actual
                (m as BicingMarker).setSelected(true, map)
                m.showInfoWindow()
                map.controller.animateTo(m.position) // Centrar el mapa a l'estació
                true
            }
            folder.add(marker)
        }
        return folder
    }

    class BicingMarker(
        mapView: MapView,
        val context: Context,
        val color: Int,
    ) : Marker(mapView) {
        private var isMarkerSelected: Boolean = false

        init {
            updateIcon()
        }

        // Canviar estat del marcador
        fun setSelected(
            selected: Boolean,
            mapView: MapView,
        ) {
            isMarkerSelected = selected
            updateIcon()
            mapView.invalidate()
        }

        // Actualitzar icona
        private fun updateIcon() {
            val drawable =
                ContextCompat.getDrawable(context, R.drawable.ic_bicing_marker)?.mutate()
                    as? LayerDrawable ?: return

            val finalColor =
                if (isMarkerSelected) { // Si s'ha pres el marcador, es fa més transparent
                    Color.argb(MARKER_ALPHA, Color.red(color), Color.green(color), Color.blue(color))
                } else {
                    color
                }

            val pin = drawable.findDrawableByLayerId(R.id.pin_layer)
            val bike = drawable.findDrawableByLayerId(R.id.bike_layer)

            pin?.let { DrawableCompat.setTint(it, finalColor) }
            bike?.let { DrawableCompat.setTint(it, finalColor) }

            this.icon = drawable
        }
    }
}
