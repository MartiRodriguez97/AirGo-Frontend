package com.airgo.airgoApp.features.map

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

    fun createBicingOverlay(
        context: Context,
        map: MapView,
        stations: List<BicingStation>,
    ): FolderOverlay {
        val folder = FolderOverlay()
        println("Creant marcadors per ${stations.size} estacions de Bicing")
        stations.forEach { station ->
            val lat = station.lat
            val lon = station.lon

            if (lat != null && lon != null) {
                val hasBikes = station.numberBikes > 0
                val baseColor = Color.parseColor(if (hasBikes) BICING_BLUE else EMPTY_RED)

                val marker = BicingMarker(map, context, baseColor)
                marker.position = GeoPoint(lat, lon)
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                marker.title = station.name
                marker.snippet =
                    """
                    🚲 Mecàniques: ${station.numberBikes}
                    🅿️ Capacitat: ${station.capacity}
                    🟢 Llocs lliures: ${'$'}{station.freeSlots}
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
