package com.airgo.airgoApp.features.map.helpers

import android.graphics.Color
import android.graphics.Paint
import com.airgo.airgoApp.features.map.data.RouteResponse
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.Polyline

object RouteHelper {
    private const val ROUTE_WIDTH = 12f // Ample de la línia de la ruta
    private const val GOOD_POLLUTION_LEVEL = 0.2
    private const val MODERATE_POLLUTION_LEVEL = 0.5
    private const val COLOR_ORANGE_R = 255
    private const val COLOR_ORANGE_G = 165
    private const val COLOR_ORANGE_B = 0
    private val ORANGE = Color.rgb(COLOR_ORANGE_R, COLOR_ORANGE_G, COLOR_ORANGE_B)

    /**
     * Funció per crear una capa de ruta a partir de la resposta de l'API.
     * Processa els segments de la ruta i els nivells de contaminació per pintar la línia amb el color corresponent.
     */
    fun createRouteOverlay(
        map: MapView,
        response: RouteResponse,
    ): FolderOverlay {
        val routeFolder = FolderOverlay().apply { name = "RouteLayer" }
        val coords = response.geometry.coordinates

        if (response.pollutionDetails.isEmpty()) {
            val allPoints = coords.map { GeoPoint(it[1], it[0]) } // GeoJSON es [lon, lat]
            if (allPoints.size >= 2) {
                val polyline =
                    Polyline(map).apply {
                        setPoints(allPoints)
                        outlinePaint.color = Color.GREEN
                        outlinePaint.strokeWidth = ROUTE_WIDTH
                        outlinePaint.strokeCap = Paint.Cap.ROUND
                    }
                routeFolder.add(polyline)
            }
            return routeFolder
        }

        response.pollutionDetails.forEach { detail ->
            val startIndex = detail[0].toInt()
            val endIndex = detail[1].toInt()
            val pollutionValue = detail[2]

            val segmentPoints = mutableListOf<GeoPoint>()
            for (i in startIndex..endIndex) {
                if (i < coords.size) {
                    segmentPoints.add(GeoPoint(coords[i][1], coords[i][0]))
                }
            }

            if (segmentPoints.size >= 2) {
                val polyline =
                    Polyline(map).apply {
                        setPoints(segmentPoints)
                        outlinePaint.color = getColorForPollution(pollutionValue)
                        outlinePaint.strokeWidth = ROUTE_WIDTH
                        outlinePaint.strokeCap = Paint.Cap.ROUND
                    }
                routeFolder.add(polyline)
            }
        }
        return routeFolder
    }

    /**
     * Funció per determinar el color de la línia de la ruta segons el nivell de contaminació.
     */
    private fun getColorForPollution(value: Double): Int =
        when {
            value < GOOD_POLLUTION_LEVEL -> Color.GREEN
            value < MODERATE_POLLUTION_LEVEL -> ORANGE
            else -> Color.RED
        }
}
