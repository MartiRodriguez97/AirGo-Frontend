package com.airgo.airgoApp.features.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.widget.Toast
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

private const val BLUE_DOT_SIZE = 40 // Mida del punt
private const val BLUE_DOT_COLOR = 0xFF4285F4.toInt() // Color del punt
private const val WHITE_BORDER_COLOR = 0xFFFFFFFF.toInt() // Vora del punt
private const val INNER_CIRCLE_RATIO = 2.5f

// Límits del mapa (Barcelona)
private const val LAT_NORTH = 41.47
private const val LAT_SOUTH = 41.31
private const val LNG_WEST = 2.0
private const val LNG_EAST = 2.3

object LocationHelper {
    fun createLocationOverlay(
        context: Context,
        map: MapView,
    ): MyLocationNewOverlay {
        val provider = GpsMyLocationProvider(context)
        val locationOverlay = MyLocationNewOverlay(provider, map)

        // Configuració de l'estil del punt de la ubicació
        val dot = createDotBitmap()
        locationOverlay.setPersonIcon(dot) // Icona quan estàs aturat
        locationOverlay.setDirectionIcon(dot) // Icona quan t'estàs movent
        locationOverlay.setPersonHotspot(BLUE_DOT_SIZE / 2f, BLUE_DOT_SIZE / 2f)

        // Desplaçar el mapa a la teva ubicació (si s'han donat permnissos i està a Barcelona)
        locationOverlay.runOnFirstFix {
            locationOverlay.myLocation?.let { loc ->
                // Comprovem si està dins dels límits de Barcelona
                val isInsideBcn =
                    loc.latitude in LAT_SOUTH..LAT_NORTH &&
                        loc.longitude in LNG_WEST..LNG_EAST

                if (isInsideBcn) {
                    // Si està a Barcelona movem el mapa
                    map.post {
                        map.controller.animateTo(loc)
                    }
                } else {
                    // Si està fora mostrem el missatge d'avís
                    map.post {
                        Toast
                            .makeText(
                                context,
                                "La teva ubicació està fora de la zona de servei d'AirGo",
                                Toast.LENGTH_LONG,
                            ).show()
                    }
                }
            }
        }

        return locationOverlay
    }

    /**
     * Funció auxiliar per dibuixar el punt d'ubicació
     */
    private fun createDotBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(BLUE_DOT_SIZE, BLUE_DOT_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { isAntiAlias = true }

        // Dibuixar vora
        paint.color = WHITE_BORDER_COLOR
        canvas.drawCircle(BLUE_DOT_SIZE / 2f, BLUE_DOT_SIZE / 2f, BLUE_DOT_SIZE / 2f, paint)

        // Dibuixar el punt
        paint.color = BLUE_DOT_COLOR
        canvas.drawCircle(
            BLUE_DOT_SIZE / 2f,
            BLUE_DOT_SIZE / 2f,
            BLUE_DOT_SIZE / INNER_CIRCLE_RATIO,
            paint,
        )

        return bitmap
    }
}
