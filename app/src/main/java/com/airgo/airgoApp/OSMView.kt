package com.airgo.airgoApp

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

private const val BCN_LATITUDE = 41.3851 // Latitud inicial
private const val BCN_LONGITUDE = 2.1734 // Longitud inicial
private const val INITIAL_ZOOM_LEVEL = 15.0 // Zoom inicial

@Composable
fun osmView(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // Conectar-se a OMS a través d'un user agent
    Configuration.getInstance().userAgentValue = context.packageName

    // Optimització: Donar permís per utilitzar més threads i més memòria
    remember {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            tileDownloadThreads = 8
            cacheMapTileCount = 1000.toShort()
            tileFileSystemCacheMaxBytes = 100L * 1024 * 1024
        }
    }

    // Vista del mapa
    val mapView = remember {
        MapView(context).apply {
            // Utilitzar més cache per evitar veure el mapa pixelat
            val cache = this.tileProvider.tileCache
            cache?.ensureCapacity(120)

            // Optimització hardware
            this.isTilesScaledToDpi = true
            this.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

            // Limitar el mapa a Barcelona
            this.minZoomLevel = 12.0
            this.setScrollableAreaLimitLatitude(41.47, 41.31, 0)
            this.setScrollableAreaLimitLongitude(2.0, 2.3, 0)
        }
    }

    // Capa d'ubicació. Crida a LocationHelper que retorna la capa (overlay) amb la localització
    val locationOverlay = remember { LocationHelper.createLocationOverlay(context, mapView) }

    // Capa de contaminació
    // ############################ polutionHelper ####################################

    // Gestió de permissos
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    // Mirar si ja tenia permissos
    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
        ) { permissions ->
            hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        }

    // Si no tenia permissos, demanar-los
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            launcher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            )
        }
    }

    // Descativar capa de localització i mapa quan se surt de la pantalla
    DisposableEffect(Unit) {
        onDispose {
            locationOverlay.disableMyLocation()
            // ################# Desactivar capa de presentació quan n'hi hagi ###############################
            mapView.onPause()
        }
    }

    // Pont entre OSM i Jetpack Compose
    AndroidView(
        modifier = modifier,
        factory = {
            mapView.apply {
                setTileSource(TileSourceFactory.MAPNIK) // Estil del mapa per defecte
                setMultiTouchControls(true) // Zoom amb dos dits
                controller.setZoom(INITIAL_ZOOM_LEVEL) // Zoom incial

                // Centrar inicialment a un punt definit
                controller.setCenter(GeoPoint(BCN_LATITUDE, BCN_LONGITUDE))

                // Pintar les capes (La última es mostra a sobre)
                // ############### Pintar la capa de contaminació quan estigui feta #####################
                overlays.add(locationOverlay)
            }
        },
        update = { map ->
            map.onResume()
            // Refrescar la ubicació
            if (hasLocationPermission && !locationOverlay.isMyLocationEnabled) {
                locationOverlay.enableMyLocation()
                locationOverlay.enableFollowLocation()
            }
        },
    )
}
