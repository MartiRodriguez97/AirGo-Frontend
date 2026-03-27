package com.airgo.airgoApp.features.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
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

// Classe per encapsular l'estat del mapa i les capes
data class MapState(
    val mapView: MapView,
    val locationOverlay: MyLocationNewOverlay,
    val bicingOverlay: FolderOverlay,
    val pollutionOverlay: FolderOverlay,
    val hasLocationPermission: Boolean,
    val stations: List<PollutionPoint>,
)

@Composable
fun osmView(
    modifier: Modifier = Modifier,
    viewModel: PollutionViewModel =
        viewModel(),
) {
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
    // Capa de Bicing
    val bicingOverlay = remember { BicingHelper.createBicingOverlay(context, mapView) }

    // Capa de contaminació. Es torna a crear cada cop que canvien les estacions, per refrescar les dades al mapa
    val pollutionOverlay =
        remember(viewModel.stations) {
            PollutionHelper.createPollutionOverlay(mapView, viewModel.stations)
        }

    // Gestió de permissos
    var hasLocationPermission by remember { mutableStateOf(checkInitialLocationPermission(context)) }
    val requestLocationPermission = rememberLocationPermissions { granted -> hasLocationPermission = granted }

    // Demanar permisos i carregar les dades de Barcelona al iniciar la pantalla
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) requestLocationPermission()
        viewModel.fetchBarcelonaData()
    }

    // Desactivar les capes quan se surt de la pantalla
    DisposableEffect(Unit) {
        onDispose {
            locationOverlay.disableMyLocation()
            mapView.onPause()
        }
    }

    // Encapsular tot l'estat del mapa en una sola classe per passar-la a la funció de renderitzat
    val mapState =
        remember(hasLocationPermission, viewModel.stations, pollutionOverlay) {
            MapState(
                mapView = mapView,
                locationOverlay = locationOverlay,
                bicingOverlay = bicingOverlay,
                pollutionOverlay = pollutionOverlay,
                hasLocationPermission = hasLocationPermission,
                stations = viewModel.stations,
            )
        }
    // Funció que pinta el mapa i les capes
    osmMapContainer(
        state = mapState,
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
    mapView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

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
private fun checkInitialLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED

/**
 * Funció per pintar el mapa
 */
@Composable
fun osmMapContainer(
    state: MapState,
    modifier: Modifier = Modifier,
) {
    // Variable per saber si s'activa/desactiva la capa de Bicing
    var showBicing by remember { mutableStateOf(true) }
    // Variable per saber si s'activa/desactiva la capa de qualitat de l'aire
    var showPollution by remember { mutableStateOf(true) }

    Box(modifier = modifier.fillMaxSize()) {
        // Pont entre OSM i Jetpack Compose
        AndroidView(
            modifier = modifier,
            factory = {
                state.mapView.apply {
                    setTileSource(TileSourceFactory.MAPNIK) // Estil del mapa per defecte
                    setMultiTouchControls(true) // Zoom amb dos dits
                    controller.setZoom(INITIAL_ZOOM_LEVEL) // Zoom incial
                    // Centrar inicialment a un punt definit
                    controller.setCenter(GeoPoint(BCN_LATITUDE, BCN_LONGITUDE))

                    // Deseleccionar marcador que s'ha pres, al presinar al mapa
                    val mapEventsReceiver =
                        object : MapEventsReceiver {
                            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                state.bicingOverlay.items.forEach { item ->
                                    if (item is Marker) {
                                        item.closeInfoWindow()
                                        if (item is BicingHelper.BicingMarker) item.setSelected(false, state.mapView)
                                    }
                                }
                                return true
                            }

                            override fun longPressHelper(p: GeoPoint?): Boolean = false
                        }
                    // Capa per detectar els taps al mapa i deseleccionar els marcadors
                    val mapEventsOverlay =
                        MapEventsOverlay(mapEventsReceiver)
                    overlays.add(mapEventsOverlay)
                    overlays.add(state.bicingOverlay)
                    overlays.add(state.locationOverlay)
                }
            },
            update = { map ->
                map.onResume()

                // Eliminar la capa de contaminació anterior per evitar duplicats quan arriben dades noves
                map.overlays.removeAll { it is FolderOverlay && it.name == "PollutionFolder" }

                // Quan arriben les dades de contaminació, crear una nova capa i afegir-la al mapa
                if (state.stations.isNotEmpty()) {
                    state.pollutionOverlay.name = "PollutionFolder"
                    if (!map.overlays.contains(state.pollutionOverlay)) {
                        map.overlays.add(0, state.pollutionOverlay)
                    }
                    map.invalidate()
                }
                if (state.hasLocationPermission && !state.locationOverlay.isMyLocationEnabled) {
                    state.locationOverlay.enableMyLocation()
                }
                // Mostrar o ocultar les capes segons les variables d'estat
                state.bicingOverlay.isEnabled = showBicing
                state.pollutionOverlay.isEnabled = showPollution

                map.invalidate()
            },
        )

        // Botó per seleccionar les capes
        layerSelector(
            showBicing = showBicing,
            onToggleBicing = { showBicing = it },
            showPollution = showPollution,
            onTogglePollution = { showPollution = it },
        )
    }
}

@Composable
private fun BoxScope.layerSelector(
    showBicing: Boolean,
    onToggleBicing: (Boolean) -> Unit,
    showPollution: Boolean,
    onTogglePollution: (Boolean) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    // Colors per indicar si la capa està activa o no
    val activeBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    val inactiveBg = Color.Transparent

    Box(
        modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 100.dp, end = 16.dp),
    ) {
        FloatingActionButton(
            onClick = { expanded = true },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            Icon(Icons.Default.Layers, contentDescription = "Capas")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            // Capa Bicing
            DropdownMenuItem(
                modifier = Modifier.background(if (showBicing) activeBg else inactiveBg),
                text = {
                    Text(
                        text = "Estacions Bicing",
                        color = if (showBicing) MaterialTheme.colorScheme.primary else Color.Unspecified,
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.AutoMirrored.Filled.DirectionsBike,
                        contentDescription = null,
                        tint = if (showBicing) MaterialTheme.colorScheme.primary else Color.Gray,
                    )
                },
                onClick = { onToggleBicing(!showBicing) },
            )

            HorizontalDivider()

            // Capa qualitat de l'aire
            DropdownMenuItem(
                modifier = Modifier.background(if (showPollution) activeBg else inactiveBg),
                text = {
                    Text(
                        text = "Qualitat de l'aire",
                        color = if (showPollution) MaterialTheme.colorScheme.primary else Color.Unspecified,
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Cloud,
                        contentDescription = null,
                        tint = if (showPollution) MaterialTheme.colorScheme.primary else Color.Gray,
                    )
                },
                onClick = { onTogglePollution(!showPollution) },
            )
        }
    }
}
