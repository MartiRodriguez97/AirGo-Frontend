package com.airgo.airgoApp.features.map.components

import android.Manifest
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.airgo.airgoApp.features.map.domain.BicingViewModel
import com.airgo.airgoApp.features.map.domain.PollutionViewModel
import com.airgo.airgoApp.features.map.domain.RouteViewModel

private const val DEFAULT_LATITUDE = 41.385
private const val DEFAULT_LONGITUDE = 2.153

@Composable
internal fun errorToastHandlers(
    context: Context,
    pollutionViewModel: PollutionViewModel,
    routeViewModel: RouteViewModel,
    bicingViewModel: BicingViewModel,
) {
    LaunchedEffect(pollutionViewModel.errorMessage) {
        pollutionViewModel.errorMessage?.let { uiText ->
            Toast.makeText(context, uiText.asString(context), Toast.LENGTH_LONG).show()
            pollutionViewModel.clearError()
        }
    }
    LaunchedEffect(bicingViewModel.errorMessage) {
        bicingViewModel.errorMessage?.let { uiText ->
            Toast.makeText(context, uiText.asString(context), Toast.LENGTH_LONG).show()
            bicingViewModel.clearError()
        }
    }
    LaunchedEffect(routeViewModel.errorMessage) {
        routeViewModel.errorMessage?.let { uiText ->
            Toast.makeText(context, uiText.asString(context), Toast.LENGTH_LONG).show()
            routeViewModel.clearError()
        }
    }
    LaunchedEffect(routeViewModel.favoriteMessage) {
        routeViewModel.favoriteMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            routeViewModel.clearFavoriteMessage()
        }
    }
}

@Composable
internal fun fetchLocationDataEffect(
    hasLocationPermission: Boolean,
    requestLocationPermission: () -> Unit,
    pollutionViewModel: PollutionViewModel,
    bicingViewModel: BicingViewModel,
    context: Context,
) {
    LaunchedEffect(hasLocationPermission) {
        if (!hasLocationPermission) {
            requestLocationPermission()
            pollutionViewModel.fetchBarcelonaData()
            bicingViewModel.fetchStations(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
        } else {
            pollutionViewModel.fetchBarcelonaData()
            try {
                val locationManager =
                    context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
                val lastLoc =
                    locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                        ?: locationManager.getLastKnownLocation(
                            android.location.LocationManager.NETWORK_PROVIDER,
                        )

                if (lastLoc != null) {
                    bicingViewModel.fetchStations(lastLoc.latitude, lastLoc.longitude)
                } else {
                    bicingViewModel.fetchStations(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
                bicingViewModel.fetchStations(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
            }
        }
    }
}

/**
 * Lògica de permissos
 */
@Composable
internal fun rememberLocationPermissions(onPermissionGranted: (Boolean) -> Unit): () -> Unit {
    val context = LocalContext.current

    // Mirar si ja tenia permissos
    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
        ) { permissions ->
            val granted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            onPermissionGranted(granted)
        }

    // Missatge per demanar permissos
    return {
        launcher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
        )
    }
}
