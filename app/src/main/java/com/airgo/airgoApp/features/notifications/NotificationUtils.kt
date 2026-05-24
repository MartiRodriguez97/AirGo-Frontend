package com.airgo.airgoApp.features.notifications

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.airgo.airgoApp.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.min

object NotificationUtils {
    private const val DST_WIDTH = 256
    private const val DST_HEIGHT = 256
    private const val TIMEOUT_MS = 5000
    fun getBitmapFromUrl(urlString: String?): Bitmap? {
        if (urlString.isNullOrEmpty()) return null
        val finalUrl =
            if (urlString.startsWith("http")) {
                urlString
            } else {
                val baseUrl = BuildConfig.BACKEND_URL.trimEnd('/')
                if (urlString.startsWith("/")) "$baseUrl$urlString" else "$baseUrl/$urlString"
            }
        return try {
            val url = URL(finalUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.doInput = true
            connection.connect()
            val bitmap =
                connection.inputStream.use {
                    BitmapFactory.decodeStream(it)
                }
            connection.disconnect()
            bitmap
        } catch (e: Exception) {
            Log.e("AIRGO_NOTIF", "Error descargando imagen ($finalUrl): ${e.message}")
            null
        }
    }

    fun getCircularBitmap(bitmap: Bitmap): Bitmap {
        val size = min(bitmap.width, bitmap.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(output)
        val paint =
            android.graphics.Paint().apply {
                isAntiAlias = true
            }
        val rect =
            android.graphics.Rect(
                (bitmap.width - size) / 2,
                (bitmap.height - size) / 2,
                (bitmap.width + size) / 2,
                (bitmap.height + size) / 2,
            )
        val outputRect = android.graphics.Rect(0, 0, size, size)
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, outputRect, paint)

        return Bitmap.createScaledBitmap(output, DST_WIDTH, DST_HEIGHT, true)
    }
}
