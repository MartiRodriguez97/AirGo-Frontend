package com.airgo.airgoApp.features.map

import android.Manifest
import android.content.Context
import android.widget.Toast
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

private const val BCN_LATITUDE = 41.3851 // Latitud inicial
private const val BCN_LONGITUDE = 2.1734 // Longitud inicial
private const val INITIAL_ZOOM_LEVEL = 15.0 // Zoom inicial

private const val TILE_THREADS: Short = 8
private const val CACHE_TILE_COUNT: Short = 1000
private const val MEGABYTES_TO_BYTES = 1024L * 1024L
private const val MAX_CACHE_MB = 100L
private const val MAX_CACHE_BYTES = MAX_CACHE_MB * MEGABYTES_TO_BYTES

/**
 * Classe que encapsula tot l'estat necessari per renderitzar el mapa i les seves capes.
 */
data class MapState(
    val mapView: MapView,
    val locationOverlay: MyLocationNewOverlay,
    val bicingOverlay: FolderOverlay,
    val pollutionOverlay: FolderOverlay,
    val hasLocationPermission: Boolean,
    val stations: List<PollutionPoint>,
    val selectionMode: MapSelectionMode,
    val startPoint: org.osmdroid.util.GeoPoint?,
    val endPoint: org.osmdroid.util.GeoPoint?,
    val currentRoute: RouteResponse?,
    val isCalculatingRoute: Boolean,
    val onPointSelected: (org.osmdroid.util.GeoPoint) -> Unit,
    val onCalculateRoute: () -> Unit,
    val onClearRoute: () -> Unit,
    val onSetSelectionMode: (MapSelectionMode) -> Unit,
)

/**
 * Composable principal que mostra el mapa i les capes.
 * Gestiona els permissos, la configuració del mapa i la interacció amb el ViewModel.
 */
@Composable
fun osmView(
    modifier: Modifier = Modifier,
    pollutionViewModel: PollutionViewModel = viewModel(),
    routeViewModel: RouteViewModel = viewModel(),
    bicingViewModel: BicingViewModel = viewModel(),
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
    val bicingOverlay = remember { FolderOverlay() }
    LaunchedEffect(bicingViewModel.stations) {
        bicingOverlay.items.clear()
        bicingOverlay.items.addAll(
            BicingHelper.createBicingOverlay(context, mapView, bicingViewModel.stations).items,
        )
        mapView.invalidate()
    }

    // Capa de contaminació
    val pollutionOverlay = remember { FolderOverlay() }
    LaunchedEffect(pollutionViewModel.stations) {
        pollutionOverlay.items.clear()
        pollutionOverlay.items.addAll(
            PollutionHelper.createPollutionOverlay(mapView, pollutionViewModel.stations).items,
        )
        mapView.invalidate()
    }

    // Gestió de permissos
    var hasLocationPermission by remember { mutableStateOf(checkInitialLocationPermission(context)) }
    val requestLocationPermission = rememberLocationPermissions { granted -> hasLocationPermission = granted }

    // Gestió d'errors
    errorToastHandlers(context, pollutionViewModel, routeViewModel, bicingViewModel)

    // Si no tenia permissos, demanar-los
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) requestLocationPermission()
        pollutionViewModel.fetchBarcelonaData()
        bicingViewModel.fetchStations(BCN_LATITUDE, BCN_LONGITUDE)
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
        remember(
            hasLocationPermission,
            pollutionViewModel.stations,
            routeViewModel.selectionMode,
            routeViewModel.startPoint,
            routeViewModel.endPoint,
            routeViewModel.currentRoute,
            routeViewModel.isLoading,
        ) {
            MapState(
                mapView = mapView,
                locationOverlay = locationOverlay,
                bicingOverlay = bicingOverlay,
                pollutionOverlay = pollutionOverlay,
                hasLocationPermission = hasLocationPermission,
                stations = pollutionViewModel.stations,
                selectionMode = routeViewModel.selectionMode,
                startPoint = routeViewModel.startPoint,
                endPoint = routeViewModel.endPoint,
                currentRoute = routeViewModel.currentRoute,
                isCalculatingRoute = routeViewModel.isLoading,
                onPointSelected = { routeViewModel.onPointSelected(it) },
                onCalculateRoute = { routeViewModel.calculateRoute() },
                onClearRoute = { routeViewModel.clearRoute() },
                onSetSelectionMode = { routeViewModel.selectionMode = it },
            )
        }
    // Funció que pinta el mapa i les capes
    osmMapContainer(
        state = mapState,
        modifier = modifier,
    )
}

@Composable
private fun errorToastHandlers(
    context: Context,
    pollutionViewModel: PollutionViewModel,
    routeViewModel: RouteViewModel,
    bicingViewModel: BicingViewModel,
) {
    LaunchedEffect(pollutionViewModel.errorMessage) {
        pollutionViewModel.errorMessage?.let { errorMsg ->
            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
            pollutionViewModel.clearError()
        }
    }
    LaunchedEffect(bicingViewModel.errorMessage) {
        bicingViewModel.errorMessage?.let { errorMsg ->
            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
            bicingViewModel.clearError()
        }
    }
    LaunchedEffect(routeViewModel.errorMessage) {
        routeViewModel.errorMessage?.let { errorMsg ->
            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
            routeViewModel.clearError()
        }
    }
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

    val selectionModeState = rememberUpdatedState(state.selectionMode)
    val onPointSelectedState = rememberUpdatedState(state.onPointSelected)

    val context = LocalContext.current
    var isGpsOff by remember { mutableStateOf(!isGpsEnabled(context)) }
    DisposableEffect(context) {
        val reciver =
            object : android.content.BroadcastReceiver() {
                override fun onReceive(
                    context: Context?,
                    intent: android.content.Intent?,
                ) {
                    if (intent?.action == android.location.LocationManager.PROVIDERS_CHANGED_ACTION) {
                        isGpsOff = !isGpsEnabled(context!!)
                    }
                }
            }
        val filter = android.content.IntentFilter(android.location.LocationManager.PROVIDERS_CHANGED_ACTION)
        context.registerReceiver(reciver, filter)
        onDispose { context.unregisterReceiver(reciver) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Pont entre OSM i Jetpack Compose
        AndroidView(
            modifier = modifier.fillMaxSize(),
            factory = { context ->
                state.mapView.apply {
                    setTileSource(TileSourceFactory.MAPNIK) // Estil del mapa per defecte
                    setMultiTouchControls(true) // Zoom amb dos dits
                    controller.setZoom(INITIAL_ZOOM_LEVEL) // Zoom incial
                    // Centrar inicialment a un punt definit
                    controller.setCenter(GeoPoint(BCN_LATITUDE, BCN_LONGITUDE))

                    overlays.add(state.pollutionOverlay)
                    val eventsOverlay =
                        createMapEventsOverlay(
                            mapView = this,
                            bicingOverlay = state.bicingOverlay,
                            selectionModeProvider = { selectionModeState.value },
                            onPointSelectedProvider = { onPointSelectedState.value },
                        )
                    overlays.add(eventsOverlay)
                    overlays.add(state.bicingOverlay)
                    overlays.add(state.locationOverlay)
                }
            },
            update = { map ->
                // Refrescar la ubicació
                if (state.hasLocationPermission && !state.locationOverlay.isMyLocationEnabled) {
                    state.locationOverlay.enableMyLocation()
                }
                // Mostrar o ocultar les capes segons les variables d'estat
                state.bicingOverlay.isEnabled = showBicing
                state.pollutionOverlay.isEnabled = showPollution

                // Actualitzar la ruta i els punts d'inici/destí
                updateRouteElements(map, state)

                map.invalidate()
            },
        )

        if (isGpsOff) {
            gpsDisabledWarning(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 220.dp), // Sota el panell de rutes
            )
        }

        // Panell de selecció de ruta
        routeSelectionPanel(
            state = state,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 30.dp),
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

/**
 * Composable per al selector de capes, on l'usuari pot activar o desactivar les capes de Bicing i qualitat de l'aire.
 * Activa i desactiva un menú desplegable amb les opcions de capes.
 */
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
