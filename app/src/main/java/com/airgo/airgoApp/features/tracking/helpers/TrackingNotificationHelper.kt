package com.airgo.airgoApp.features.tracking.helpers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.airgo.airgoApp.MainActivity
import com.airgo.airgoApp.R

internal const val NOTIFICATION_ID = 1001
private const val CHANNEL_ID = "tracking_channel"
private const val MS_TO_KMH = 3.6

internal object TrackingNotificationHelper {
    @RequiresApi(Build.VERSION_CODES.O)
    fun createChannel(context: Context) {
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                "Tracking",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Canal per al tracking d'activitats"
            }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    fun buildNotification(
        context: Context,
        subtitle: String = "Tracking actiu",
    ): Notification {
        val pendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        return NotificationCompat
            .Builder(context, CHANNEL_ID)
            .setContentTitle("AirGo Tracking")
            .setContentText(subtitle)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    fun updateNotification(
        context: Context,
        speedMs: Float,
    ) {
        val subtitle = "%.1f km/h".format(speedMs * MS_TO_KMH)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(context, subtitle))
    }
}
