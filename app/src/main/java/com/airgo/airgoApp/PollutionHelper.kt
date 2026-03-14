package com.airgo.airgoApp

import android.graphics.Color
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.Polygon

object PollutionHelper {

    //Exemple de dades
    private val mockStatus = listOf(
        PollutionPoint("Gràcia", GeoPoint(41.3987, 2.1533), 40),    // Bo (verd)
        PollutionPoint("Eixample", GeoPoint(41.3853, 2.1534), 85),  // Moderat (groc)
        PollutionPoint("Poblenou", GeoPoint(41.4039, 2.2045), 120), // Insalubre per a persones sensibles (taronja)
        PollutionPoint("Sants", GeoPoint(41.3784, 2.1392), 160)     // Insalubre (vermell)
    )

    fun createPollutionOverlay(map: MapView): FolderOverlay {
        val folder = FolderOverlay()

        mockStatus.forEach { status ->
            //Crear cercle de la zona per al mapa
            val circle = Polygon(map)
            val circlePoints = Polygon.pointsAsCircle(status.geoPoint, 300.0)
            circle.points = circlePoints

            //Pintar el cercle amb les característiques correponents
            val color = getAqiColor(status.aqi)
            circle.fillPaint.color = color
            circle.fillPaint.alpha = 70 
            circle.outlinePaint.color = color
            circle.outlinePaint.strokeWidth = 0f

            circle.title = "${status.zone}: AQI ${status.aqi}"
            folder.add(circle)
        }

        return folder
    }

    //Funció per escollir color segons l'aqi
    private fun getAqiColor(aqi: Int): Int {
        return when {
            aqi <= 50 -> Color.GREEN
            aqi <= 100 -> Color.YELLOW
            aqi <= 150 -> 0xFFFFA500.toInt()
            else -> Color.RED
        }
    }
}