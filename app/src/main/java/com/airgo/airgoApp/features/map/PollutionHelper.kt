package com.airgo.airgoApp.features.map

import android.graphics.Color
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.Polygon

// Constants per a la visualització de la contaminació
private const val POLLUTION_RADIUS = 300.0
private const val CIRCLE_ALPHA = 70

// Colors per a diferents nivells de contaminació
private const val AQI_GOOD = 50
private const val AQI_MODERATE = 100
private const val AQI_SENSITIVE = 150
private const val COLOR_ORANGE = 0xFFFFA500.toInt()

object PollutionHelper {
    fun createPollutionOverlay(
        map: MapView,
        stations: List<PollutionPoint>,
    ): FolderOverlay {
        val folder = FolderOverlay()

        stations.forEach { station ->
            // Crear cercle de la zona per al mapa
            val circle = Polygon(map)
            val point = GeoPoint(station.geoPoint.lat, station.geoPoint.lon)
            val circlePoints = Polygon.pointsAsCircle(point, POLLUTION_RADIUS)
            circle.points = circlePoints

            // Pintar el cercle amb les característiques corresponents
            val color = getAqiColor(station.aqi)
            circle.fillPaint.color = color
            circle.fillPaint.alpha = CIRCLE_ALPHA
            circle.outlinePaint.color = color
            circle.outlinePaint.strokeWidth = 0f

            circle.title = "${station.zone}: AQI ${station.aqi}"
            folder.add(circle)
        }

        return folder
    }

    /**
     * Funció per escollir color segons l'aqi
     */
    private fun getAqiColor(aqi: Int): Int =
        when {
            aqi <= AQI_GOOD -> Color.GREEN
            aqi <= AQI_MODERATE -> Color.YELLOW
            aqi <= AQI_SENSITIVE -> COLOR_ORANGE
            else -> Color.RED
        }
}
