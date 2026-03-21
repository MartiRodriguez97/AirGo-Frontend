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
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

private const val BCN_LATITUDE = 41.3851 // Latitud inicial
private const val BCN_LONGITUDE = 2.1734 // Longitud inicial
private const val INITIAL_ZOOM_LEVEL = 15.0 // Zoom inicial

private const val TILE_THREADS: Short = 8
private const val CACHE_TILE_COUNT: Short = 1000
private const val MEGABYTES_TO_BYTES = 1024L * 1024L
private const val MAX_CACHE_MB = 100L
private const val MAX_CACHE_BYTES = MAX_CACHE_MB * MEGABYTES_TO_BYTES

// Límits del mapa
private const val LAT_NORTH = 41.47
private const val LAT_SOUTH = 41.31
private const val LNG_WEST = 2.0
private const val LNG_EAST = 2.3

private const val MIN_ZOOM = 12.0
private const val MAX_ZOOM = 19.0

private const val CACHE_CAPACITY = 120

@Composable
fun osmView(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // Conectar-se a OMS a través d'un user agent
    Configuration.getInstance().userAgentValue = context.packageName

    // Optimització: Donar permís per utilitzar més threads i més memòria
    remember {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            tileDownloadThreads = TILE_THREADS
            cacheMapTileCount = CACHE_TILE_COUNT
            tileFileSystemCacheMaxBytes = MAX_CACHE_BYTES
        }
    }

    // Vista del mapa
    val mapView = remember { MapView(context).apply { configureMapView(this) } }

    // Capa d'ubicació. Crida a LocationHelper que retorna la capa (overlay) amb la localització
    val locationOverlay = remember { LocationHelper.createLocationOverlay(context, mapView) }

    // Capa de contaminació
    // ############################ polutionHelper ####################################

    // Gestió de permissos
    var hasLocationPermission by remember { mutableStateOf(checkInitialLocationPermission(context)) }
    val requestLocationPermission = rememberLocationPermissions { granted -> hasLocationPermission = granted }

    // Si no tenia permissos, demanar-los
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) requestLocationPermission()
    }

    // Desactivar les capes quan se surt de la pantalla
    DisposableEffect(Unit) {
        onDispose {
            locationOverlay.disableMyLocation()
            // ################# Desactivar capa de presentació quan n'hi hagi ###############################
            mapView.onPause()
        }
    }

    // Pintar el mapa
    osmMapContainer(
        mapView = mapView,
        locationOverlay = locationOverlay,
        hasLocationPermission = hasLocationPermission,
        modifier = modifier,
    )
}

/**
 * Configuració de la vista del mapa
 */
private fun configureMapView(mapView: MapView) {
    // Utilitzar més cache per evitar veure el mapa pixelat
    val cache = mapView.tileProvider.tileCache
    cache?.ensureCapacity(CACHE_CAPACITY)

    // Optimització hardware
    mapView.isTilesScaledToDpi = true
    mapView.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

    // Limitar el mapa a Barcelona
    mapView.minZoomLevel = MIN_ZOOM
    mapView.maxZoomLevel = MAX_ZOOM
    mapView.setScrollableAreaLimitLatitude(LAT_NORTH, LAT_SOUTH, 0)
    mapView.setScrollableAreaLimitLongitude(LNG_WEST, LNG_EAST, 0)
}

/**
 * Lògica de permissos
 */
@Composable
fun rememberLocationPermissions(onPermissionGranted: (Boolean) -> Unit): () -> Unit {
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

/**
 * Comprovar si l'aplicació té permissos de localització
 */
private fun checkInitialLocationPermission(context: android.content.Context): Boolean =
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED

/**
 * Funció per pintar el mapa
 */
@Composable
fun osmMapContainer(
    mapView: MapView,
    locationOverlay: MyLocationNewOverlay,
    hasLocationPermission: Boolean,
    modifier: Modifier = Modifier,
) {
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
