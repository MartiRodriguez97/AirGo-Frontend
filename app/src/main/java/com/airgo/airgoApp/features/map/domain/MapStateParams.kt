package com.airgo.airgoApp.features.map.domain

import com.airgo.airgoApp.features.tracking.domain.TrackingViewModel
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay

/**
 * Paràmetres relacionats amb les capes i la vista del mapa.
 */
data class MapLayerParams(
    val mapView: MapView,
    val locationOverlay: org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay,
    val bicingOverlay: FolderOverlay,
    val pollutionOverlay: FolderOverlay,
    val hasLocationPermission: Boolean,
)

/**
 * Paràmetres relacionats amb els ViewModels i la navegació.
 */
data class MapViewModelParams(
    val pollutionViewModel: PollutionViewModel,
    val routeViewModel: RouteViewModel,
    val trackingViewModel: TrackingViewModel,
    val onNavigateToFavorites: () -> Unit,
    val onNavigateToTrackingSummary: () -> Unit,
    val onNavigateToAddressSearch: (String) -> Unit = {},
)

/**
 * Classe que encapsula els paràmetres necessaris per construir l'estat del mapa.
 */
data class MapStateParams(
    val layerParams: MapLayerParams,
    val viewModelParams: MapViewModelParams,
)
