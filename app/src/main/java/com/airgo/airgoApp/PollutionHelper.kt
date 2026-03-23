package com.airgo.airgoApp

import android.graphics.Color
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.Polygon

// Constants per a les estacions de contaminació simulades
private const val GRACIA_LAT = 41.3987
private const val GRACIA_LNG = 2.1533
private const val GRACIA_AQI = 40

private const val EIXAMPLE_LAT = 41.3853
private const val EIXAMPLE_LNG = 2.1534
private const val EIXAMPLE_AQI = 85

private const val POBLENOU_LAT = 41.4039
private const val POBLENOU_LNG = 2.2045
private const val POBLENOU_AQI = 120

private const val SANTS_LAT = 41.3784
private const val SANTS_LNG = 2.1392
private const val SANTS_AQI = 160

// Constants per a la visualització de la contaminació
private const val POLLUTION_RADIUS = 300.0
private const val CIRCLE_ALPHA = 70

// Colors per a diferents nivells de contaminació
private const val AQI_GOOD = 50
private const val AQI_MODERATE = 100
private const val AQI_SENSITIVE = 150
private const val COLOR_ORANGE = 0xFFFFA500.toInt()

object PollutionHelper {
    // Simulació de dades de contaminació per a diferents zones de Barcelona
    private val mockStatus =
        listOf(
            PollutionPoint("Gràcia", GeoPoint(GRACIA_LAT, GRACIA_LNG), GRACIA_AQI),
            PollutionPoint("Eixample", GeoPoint(EIXAMPLE_LAT, EIXAMPLE_LNG), EIXAMPLE_AQI),
            PollutionPoint("Poblenou", GeoPoint(POBLENOU_LAT, POBLENOU_LNG), POBLENOU_AQI),
            PollutionPoint("Sants", GeoPoint(SANTS_LAT, SANTS_LNG), SANTS_AQI),
        )

    fun createPollutionOverlay(map: MapView): FolderOverlay {
        val folder = FolderOverlay()

        mockStatus.forEach { station ->
            // Crear cercle de la zona per al mapa
            val circle = Polygon(map)
            val circlePoints = Polygon.pointsAsCircle(station.geoPoint, POLLUTION_RADIUS)
            circle.points = circlePoints

            // Pintar el cercle amb les característiques correponents
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
