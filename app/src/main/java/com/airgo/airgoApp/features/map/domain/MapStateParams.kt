package com.airgo.airgoApp.features.map.domain

import com.airgo.airgoApp.features.tracking.domain.TrackingViewModel
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay

/**
 * Classe que encapsula els paràmetres necessaris per construir l'estat del mapa.
 * Aquesta classe és útil per passar tots els paràmetres necessaris a la funció que construeix l'estat del mapa
 * sense haver de passar-los individualment.
 */
class MapStateParams(
    val hasLocationPermission: Boolean,
    val mapView: MapView,
    val locationOverlay: org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay,
    val bicingOverlay: FolderOverlay,
    val pollutionOverlay: FolderOverlay,
    val pollutionViewModel: PollutionViewModel,
    val routeViewModel: RouteViewModel,
    val trackingViewModel: TrackingViewModel,
    val onNavigateToFavorites: () -> Unit,
)
