package com.airgo.airgoApp

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.view.MotionEvent
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.Marker

object BicingHelper {

    // Colors constants
    private const val BICING_BLUE = "#0081C9"
    private const val EMPTY_RED = "#FF0000"

    //Exemples de dades
    private val mockBicingStations = listOf(
        BicingStation("1", "Pl. Catalunya", GeoPoint(41.3871, 2.1701), 10, 5, 12, "IN_SERVICE"),
        BicingStation("2", "Arc de Triomf", GeoPoint(41.3914, 2.1806), 2, 1, 25, "IN_SERVICE"),
        BicingStation("3", "Sagrada Família", GeoPoint(41.4036, 2.1744), 0, 0, 30, "IN_SERVICE")
    )

    fun createBicingOverlay(context: Context, map: MapView): FolderOverlay {
        val folder = FolderOverlay()

        mockBicingStations.forEach { station ->
            val hasBikes = (station.mechanicalBikes + station.electricBikes) > 0
            val baseColor = Color.parseColor(if (hasBikes) BICING_BLUE else EMPTY_RED)

            //Marcador d'estació Bicing
            val marker = BicingMarker(map, context, baseColor)
            marker.position = station.geoPoint
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

            marker.title = station.name
            marker.snippet = """
                🚲 Mecàniques: ${station.mechanicalBikes}
                ⚡ Elèctriques: ${station.electricBikes}
                🅿️ Llocs lliures: ${station.freeSlots}
            """.trimIndent()

            folder.add(marker)
        }
        return folder
    }


    class BicingMarker(mapView: MapView, val context: Context, val color: Int) : Marker(mapView) {

        init {
            // Aplicar icona inicial
            updateIcon(isSelected = false)
        }

        private fun updateIcon(isSelected: Boolean) {
            val drawable = ContextCompat.getDrawable(context, R.drawable.ic_bicing_marker) as LayerDrawable

            // Si està seleccionada es posa més transparent
            val finalColor = if (isInfoWindowOpen) {
                Color.argb(100, Color.red(color), Color.green(color), Color.blue(color))
            } else {
                color
            }

            // Pintar pin de localització i bicicleta segons les característiques pertinents
            val pin = drawable.findDrawableByLayerId(R.id.pin_layer)
            val bike = drawable.findDrawableByLayerId(R.id.bike_layer)

            DrawableCompat.setTint(pin, finalColor)
            DrawableCompat.setTint(bike, finalColor)

            val scale = if (isSelected) 1.2f else 1.0f
            this.icon = drawable
        }

        override fun onTouchEvent(event: MotionEvent, mapView: MapView): Boolean {
            if (hitTest(event, mapView)) {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        updateIcon(isSelected = true)
                        mapView.invalidate()
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        updateIcon(isSelected = false)
                        mapView.invalidate()
                    }
                }
            }
            return super.onTouchEvent(event, mapView)
        }
    }
}