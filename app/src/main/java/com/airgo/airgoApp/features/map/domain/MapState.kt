package com.airgo.airgoApp.features.map.domain

import com.airgo.airgoApp.features.map.data.PollutionPoint
import com.airgo.airgoApp.features.map.data.RouteResponse
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

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
    val onAddToFavorites: () -> Unit,
    val isFavorite: Boolean = false,
    val favoriteMessage: String? = null,
    val onNavigateToFavorites: () -> Unit,
)
