package com.airgo.airgoApp.features.map

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airgo.airgoApp.R
import com.airgo.airgoApp.features.map.components.errorToastHandlers
import com.airgo.airgoApp.features.map.components.fetchLocationDataEffect
import com.airgo.airgoApp.features.map.components.gpsDisabledWarning
import com.airgo.airgoApp.features.map.components.layerSelector
import com.airgo.airgoApp.features.map.components.modeSelector
import com.airgo.airgoApp.features.map.components.rememberLocationPermissions
import com.airgo.airgoApp.features.map.components.routeSelectionPanel
import com.airgo.airgoApp.features.map.domain.BicingViewModel
import com.airgo.airgoApp.features.map.domain.MapState
import com.airgo.airgoApp.features.map.domain.MapStateParams
import com.airgo.airgoApp.features.map.domain.PollutionViewModel
import com.airgo.airgoApp.features.map.domain.RouteViewModel
import com.airgo.airgoApp.features.map.helpers.BicingHelper
import com.airgo.airgoApp.features.map.helpers.LocationHelper
import com.airgo.airgoApp.features.map.helpers.PollutionHelper
import com.airgo.airgoApp.features.map.helpers.checkInitialLocationPermission
import com.airgo.airgoApp.features.map.helpers.configureMapView
import com.airgo.airgoApp.features.map.helpers.createMapEventsOverlay
import com.airgo.airgoApp.features.map.helpers.rememberGpsEnabledState
import com.airgo.airgoApp.features.map.helpers.updateRouteElements
import com.airgo.airgoApp.features.map.helpers.updateTrackingPolyline
import com.airgo.airgoApp.features.tracking.components.TrackingFabState
import com.airgo.airgoApp.features.tracking.components.trackingFab
import com.airgo.airgoApp.features.tracking.components.trackingPanel
import com.airgo.airgoApp.features.tracking.domain.TrackingViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay

private const val BCN_LATITUDE = 41.3851 // Latitud inicial
private const val BCN_LONGITUDE = 2.1734 // Longitud inicial
private const val INITIAL_ZOOM_LEVEL = 15.0 // Zoom inicial

private const val TILE_THREADS: Short = 8
private const val CACHE_TILE_COUNT: Short = 1000
private const val MEGABYTES_TO_BYTES = 1024L * 1024L
private const val MAX_CACHE_MB = 100L
private const val MAX_CACHE_BYTES = MAX_CACHE_MB * MEGABYTES_TO_BYTES
private const val TRACKING_INTERVAL_MS = 1000L
private const val TRACKING_MIN_DISTANCE_M = 2f
private const val TRACKING_ZOOM_LEVEL = 18.0

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
    onNavigateToFavorites: () -> Unit = {},
    trackingViewModel: TrackingViewModel = viewModel(),
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
    val bicingOverlay = rememberBicingOverlay(context, mapView, bicingViewModel)

    // Capa de contaminació
    val pollutionOverlay = rememberPollutionOverlay(mapView, pollutionViewModel)

    // Gestió de permissos
    var hasLocationPermission by remember { mutableStateOf(checkInitialLocationPermission(context)) }
    val requestLocationPermission = rememberLocationPermissions { granted -> hasLocationPermission = granted }

    // Gestió d'errors
    errorToastHandlers(context, pollutionViewModel, routeViewModel, bicingViewModel)

    // Feedback visual per tracking
    LaunchedEffect(Unit) {
        trackingViewModel.uiEvent.collect { event ->
            when (event) {
                is com.airgo.airgoApp.features.tracking.domain.TrackingUiEvent.SaveSuccess -> {
                    val message =
                        if (event.completed) {
                            "Entrenament completat i guardat!"
                        } else {
                            "Progress guardat"
                        }
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
                is com.airgo.airgoApp.features.tracking.domain.TrackingUiEvent.SaveError -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fetchLocationDataEffect(
        hasLocationPermission = hasLocationPermission,
        requestLocationPermission = requestLocationPermission,
        pollutionViewModel = pollutionViewModel,
        bicingViewModel = bicingViewModel,
        context = context,
    )

    // Desactivar les capes quan se surt de la pantalla
    DisposableEffect(Unit) {
        onDispose {
            locationOverlay.disableMyLocation()
            mapView.onPause()
        }
    }

    trackingLocationEffect(
        isTracking = trackingViewModel.isTracking,
        hasLocationPermission = hasLocationPermission,
        context = context,
        onNewLocation = { trackingViewModel.onNewLocation(it) },
    )

    // Encapsular tot l'estat del mapa en una sola classe per passar-la a la funció de renderitzat
    val mapState =
        rememberMapState(
            MapStateParams(
                hasLocationPermission = hasLocationPermission,
                mapView = mapView,
                locationOverlay = locationOverlay,
                bicingOverlay = bicingOverlay,
                pollutionOverlay = pollutionOverlay,
                pollutionViewModel = pollutionViewModel,
                routeViewModel = routeViewModel,
                trackingViewModel = trackingViewModel,
                onNavigateToFavorites = onNavigateToFavorites,
            ),
        )
    // Funció que pinta el mapa i les capes
    osmMapContainer(state = mapState, modifier = modifier)
}

@Composable
private fun rememberBicingOverlay(
    context: android.content.Context,
    mapView: MapView,
    bicingViewModel: BicingViewModel,
): FolderOverlay {
    val overlay = remember { FolderOverlay() }
    LaunchedEffect(bicingViewModel.stations) {
        overlay.items.clear()
        overlay.items.addAll(BicingHelper.createBicingOverlay(context, mapView, bicingViewModel.stations).items)
        mapView.invalidate()
    }
    return overlay
}

@Composable
private fun rememberPollutionOverlay(
    mapView: MapView,
    pollutionViewModel: PollutionViewModel,
): FolderOverlay {
    val overlay = remember { FolderOverlay() }
    LaunchedEffect(pollutionViewModel.stations) {
        overlay.items.clear()
        overlay.items.addAll(PollutionHelper.createPollutionOverlay(mapView, pollutionViewModel.stations).items)
        mapView.invalidate()
    }
    return overlay
}

@Composable
private fun trackingLocationEffect(
    isTracking: Boolean,
    hasLocationPermission: Boolean,
    context: android.content.Context,
    onNewLocation: (android.location.Location) -> Unit,
) {
    DisposableEffect(isTracking) {
        val locationManager =
            context.getSystemService(
                android.content.Context.LOCATION_SERVICE,
            ) as android.location.LocationManager
        val listener = android.location.LocationListener { onNewLocation(it) }

        if (isTracking && hasLocationPermission) {
            try {
                locationManager.requestLocationUpdates(
                    android.location.LocationManager.GPS_PROVIDER,
                    TRACKING_INTERVAL_MS,
                    TRACKING_MIN_DISTANCE_M,
                    listener,
                )
            } catch (e: SecurityException) {
                android.util.Log.e("Tracking", "No location permission", e)
            }
        }
        onDispose { locationManager.removeUpdates(listener) }
    }
}

@Composable
private fun rememberMapState(params: MapStateParams): MapState =
    remember(
        params.hasLocationPermission,
        params.pollutionViewModel.stations,
        params.routeViewModel.selectionMode,
        params.routeViewModel.startPoint,
        params.routeViewModel.endPoint,
        params.routeViewModel.currentRoute,
        params.routeViewModel.isLoading,
        params.routeViewModel.isFavorite,
        params.routeViewModel.favoriteMessage,
        params.trackingViewModel.isTracking,
        params.trackingViewModel.data,
        params.trackingViewModel.trackPoints,
    ) {
        MapState(
            mapView = params.mapView,
            locationOverlay = params.locationOverlay,
            bicingOverlay = params.bicingOverlay,
            pollutionOverlay = params.pollutionOverlay,
            hasLocationPermission = params.hasLocationPermission,
            stations = params.pollutionViewModel.stations,
            selectionMode = params.routeViewModel.selectionMode,
            startPoint = params.routeViewModel.startPoint,
            endPoint = params.routeViewModel.endPoint,
            currentRoute = params.routeViewModel.currentRoute,
            isCalculatingRoute = params.routeViewModel.isLoading,
            onPointSelected = { params.routeViewModel.onPointSelected(it) },
            onCalculateRoute = { params.routeViewModel.calculateRoute() },
            onClearRoute = { params.routeViewModel.clearRoute() },
            onSetSelectionMode = { params.routeViewModel.selectionMode = it },
            onAddToFavorites = { params.routeViewModel.addToFavorites() },
            isFavorite = params.routeViewModel.isFavorite,
            favoriteMessage = params.routeViewModel.favoriteMessage,
            onNavigateToFavorites = params.onNavigateToFavorites,
            isTracking = params.trackingViewModel.isTracking,
            trackingData = params.trackingViewModel.data,
            trackPoints = params.trackingViewModel.trackPoints,
            onStartTracking = { params.trackingViewModel.startTracking() },
            onStopTracking = { params.trackingViewModel.stopTracking() },
        )
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
    var isBikingMode by remember { mutableStateOf(false) }
    val isGpsEnabled by rememberGpsEnabledState(context)

    Box(modifier = modifier.fillMaxSize()) {
        mapRenderer(
            state = state,
            showBicing = showBicing,
            showPollution = showPollution,
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
        )

        trackingMapEffects(state)

        if (!isGpsEnabled) {
            gpsDisabledWarning(
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(bottom = 170.dp, end = 16.dp),
            )
        }

        // Panell de selecció de ruta i botons només si no estem en mode tracking
        if (!state.isTracking) {
            mapNormalModeUi(
                state = state,
                showBicing = showBicing,
                showPollution = showPollution,
                isBikingMode = isBikingMode,
                onToggleBicing = { showBicing = it },
                onTogglePollution = { showPollution = it },
                onModeChange = { isBikingMode = it },
            )
        }

        // Panell de tracking només si estem en tracking
        if (state.isTracking) {
            trackingPanel(
                data = state.trackingData,
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 16.dp),
            )
        }

        // Botó de tracking
        trackingFab(
            state =
                TrackingFabState(
                    isTracking = state.isTracking,
                    hasLocationPermission = state.hasLocationPermission,
                    isGpsOn = isGpsEnabled,
                    onStartTracking = state.onStartTracking,
                    onStopTracking = state.onStopTracking,
                    onShowPermissionError = {
                        Toast
                            .makeText(
                                context,
                                "Cal activar els permisos de localització",
                                Toast.LENGTH_SHORT,
                            ).show()
                    },
                    onShowGpsError = {
                        Toast
                            .makeText(
                                context,
                                "Cal activar el GPS per fer tracking",
                                Toast.LENGTH_SHORT,
                            ).show()
                    },
                ),
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 100.dp),
        )
    }
}

@Composable
private fun trackingMapEffects(state: MapState) {
    LaunchedEffect(state.isTracking) {
        if (state.isTracking) {
            state.locationOverlay.myLocation?.let { location ->
                state.mapView.controller.animateTo(location)
                state.mapView.controller.setZoom(TRACKING_ZOOM_LEVEL)
            }
        }
    }
    LaunchedEffect(state.trackPoints.lastOrNull()) {
        if (state.isTracking && state.trackPoints.isNotEmpty()) {
            state.mapView.controller.animateTo(state.trackPoints.last())
        }
    }
}

@Composable
private fun BoxScope.mapNormalModeUi(
    state: MapState,
    showBicing: Boolean,
    showPollution: Boolean,
    isBikingMode: Boolean,
    onToggleBicing: (Boolean) -> Unit,
    onTogglePollution: (Boolean) -> Unit,
    onModeChange: (Boolean) -> Unit,
) {
    // Panell de selecció de ruta
    routeSelectionPanel(
        state = state,
        modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 16.dp),
    )

    // Botó per canviar entre mode bici i mode a peu
    modeSelector(
        isBiking = isBikingMode,
        onModeChange = onModeChange,
        modifier =
            Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(bottom = 240.dp, end = 16.dp),
    )

    // Selector de capes (Bicing i contaminació)
    layerSelector(
        showBicing = showBicing,
        onToggleBicing = onToggleBicing,
        showPollution = showPollution,
        onTogglePollution = onTogglePollution,
    )

    // Botó per navegar a rutes favorites
    FloatingActionButton(
        onClick = { state.onNavigateToFavorites() },
        modifier =
            Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(bottom = 100.dp, start = 16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
    ) {
        Icon(imageVector = Icons.Default.Favorite, contentDescription = stringResource(R.string.view_favorites_routes))
    }
}

@Composable
private fun mapRenderer(
    state: MapState,
    showBicing: Boolean,
    showPollution: Boolean,
    modifier: Modifier = Modifier,
) {
    val selectionModeState = rememberUpdatedState(state.selectionMode)
    val onPointSelectedState = rememberUpdatedState(state.onPointSelected)

    AndroidView(
        modifier = modifier,
        factory = { context ->
            state.mapView.apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(INITIAL_ZOOM_LEVEL)
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
            if (state.hasLocationPermission && !state.locationOverlay.isMyLocationEnabled) {
                state.locationOverlay.enableMyLocation()
            }
            state.bicingOverlay.isEnabled = showBicing
            state.pollutionOverlay.isEnabled = showPollution

            // Dibuixar ruta de tracking
            updateTrackingPolyline(map, state.trackPoints)

            updateRouteElements(map, state)
            map.invalidate()
        },
    )
}
