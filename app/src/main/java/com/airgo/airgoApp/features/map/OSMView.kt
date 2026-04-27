package com.airgo.airgoApp.features.map

import androidx.compose.foundation.layout.Box
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
        bicingOverlay.items.addAll(BicingHelper.createBicingOverlay(context, mapView, bicingViewModel.stations).items)
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
            routeViewModel.isFavorite,
            routeViewModel.favoriteMessage,
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
                onAddToFavorites = { routeViewModel.addToFavorites() },
                isFavorite = routeViewModel.isFavorite,
                favoriteMessage = routeViewModel.favoriteMessage,
                onNavigateToFavorites = onNavigateToFavorites,
            )
        }
    // Funció que pinta el mapa i les capes
    osmMapContainer(state = mapState, modifier = modifier)
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
    val isGpsOff by rememberGpsEnabledState(context)

    Box(modifier = modifier.fillMaxSize()) {
        mapRenderer(
            state = state,
            showBicing = showBicing,
            showPollution = showPollution,
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
        )

        if (isGpsOff) {
            gpsDisabledWarning(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 180.dp),
            )
        }

        // Panell de selecció de ruta
        routeSelectionPanel(
            state = state,
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 16.dp),
        )
        // botó per seleccionar mode (peu-caminar)
        modeSelector(
            isBiking = isBikingMode,
            onModeChange = { isBikingMode = it },
            modifier =
                Modifier
                    .align(Alignment.BottomCenter) // Centrado abajo
                    .navigationBarsPadding() // Respetamos el menú inferior
                    .padding(bottom = 100.dp), // Lo subimos para que no tape la barra del sistema
        )

        // Botó per seleccionar les capes
        layerSelector(
            showBicing = showBicing,
            onToggleBicing = { showBicing = it },
            showPollution = showPollution,
            onTogglePollution = { showPollution = it },
        )

        // Botó de preferits
        FloatingActionButton(
            onClick = { state.onNavigateToFavorites() },
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 100.dp, start = 16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = stringResource(R.string.view_favorites_routes),
            )
        }
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

            updateRouteElements(map, state)
            map.invalidate()
        },
    )
}
