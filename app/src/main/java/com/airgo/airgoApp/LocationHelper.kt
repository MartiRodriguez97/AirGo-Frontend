package com.airgo.airgoApp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

private const val BLUE_DOT_SIZE = 40 // Mida del punt
private const val BLUE_DOT_COLOR = 0xFF4285F4.toInt() // Color del punt
private const val WHITE_BORDER_COLOR = 0xFFFFFFFF.toInt() // Vora del punt
private const val INNER_CIRCLE_RATIO = 2.5f

object LocationHelper {
    fun createLocationOverlay(
        context: Context,
        map: MapView,
    ): MyLocationNewOverlay {
        val provider = GpsMyLocationProvider(context)
        val overlay = MyLocationNewOverlay(provider, map)

        // Configuració de l'estil del punt de la ubicació
        val dot = createDotBitmap()
        overlay.setPersonIcon(dot) // Icona quan estàs aturat
        overlay.setDirectionIcon(dot) // Icona quan t'estàs movent
        overlay.setPersonHotspot(BLUE_DOT_SIZE / 2f, BLUE_DOT_SIZE / 2f)

        // Desplaçar el mapa a la teva ubicació (si s'han donat permnissos)
        overlay.runOnFirstFix {
            overlay.myLocation?.let { loc ->
                map.post { map.controller.animateTo(loc) }
            }
        }

        return overlay
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
