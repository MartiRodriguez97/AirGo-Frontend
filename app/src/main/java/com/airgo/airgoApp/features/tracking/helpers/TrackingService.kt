package com.airgo.airgoApp.features.tracking.helpers

import android.app.Service
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.localbroadcastmanager.content.LocalBroadcastManager

private const val TRACKING_INTERVAL_MS = 1000L
private const val TRACKING_MIN_DISTANCE_M = 2f
private const val LOG_TAG = "TrackingService"

class TrackingService : Service() {
    private lateinit var locationManager: LocationManager
    private var locationListener: LocationListener? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        TrackingNotificationHelper.createChannel(this)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action) {
            ACTION_START -> startTrackingService()
            ACTION_STOP -> stopTrackingService()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startTrackingService() {
        startForeground(NOTIFICATION_ID, TrackingNotificationHelper.buildNotification(this))
        startLocationUpdates()
        Log.d(LOG_TAG, "Tracking service started")
    }

    private fun stopTrackingService() {
        stopLocationUpdates()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.d(LOG_TAG, "Tracking service stopped")
    }

    private fun startLocationUpdates() {
        locationListener =
            LocationListener { location ->
                sendLocationBroadcast(location)
                TrackingNotificationHelper.updateNotification(this, location.speed)
            }
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                TRACKING_INTERVAL_MS,
                TRACKING_MIN_DISTANCE_M,
                locationListener!!,
            )
        } catch (e: SecurityException) {
            Log.e(LOG_TAG, "No location permission", e)
            stopTrackingService()
        }
    }

    private fun stopLocationUpdates() {
        locationListener?.let { locationManager.removeUpdates(it) }
        locationListener = null
    }

    private fun sendLocationBroadcast(location: Location) {
        Log.d(LOG_TAG, "Enviant ubicació: ${location.latitude}, ${location.longitude}")
        val intent =
            Intent(ACTION_LOCATION_UPDATE).apply {
                putExtra(EXTRA_LATITUDE, location.latitude)
                putExtra(EXTRA_LONGITUDE, location.longitude)
                putExtra(EXTRA_SPEED, location.speed)
                putExtra(EXTRA_TIME, location.time)
            }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    companion object {
        const val ACTION_START = "ACTION_START_TRACKING"
        const val ACTION_STOP = "ACTION_STOP_TRACKING"
        const val ACTION_LOCATION_UPDATE = "ACTION_LOCATION_UPDATE"
        const val EXTRA_LATITUDE = "EXTRA_LATITUDE"
        const val EXTRA_LONGITUDE = "EXTRA_LONGITUDE"
        const val EXTRA_SPEED = "EXTRA_SPEED"
        const val EXTRA_TIME = "EXTRA_TIME"
    }
}
