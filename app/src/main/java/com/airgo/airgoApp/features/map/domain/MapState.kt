package com.airgo.airgoApp.features.map.domain

import com.airgo.airgoApp.features.map.data.PollutionPoint
import com.airgo.airgoApp.features.map.data.RouteResponse
import com.airgo.airgoApp.features.map.data.TransportProfile
import com.airgo.airgoApp.features.tracking.data.TrackingData
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
    val waypoints: List<org.osmdroid.util.GeoPoint> = emptyList(),
    val currentRoute: RouteResponse?,
    val isCalculatingRoute: Boolean,
    val transportProfile: TransportProfile = TransportProfile.BIKE,  // nuevo
    val onPointSelected: (org.osmdroid.util.GeoPoint) -> Unit,
    val onCalculateRoute: () -> Unit,
    val onClearRoute: () -> Unit,
    val onSetSelectionMode: (MapSelectionMode) -> Unit,
    val onAddToFavorites: () -> Unit,
    val onAddWaypoint: () -> Unit = {},
    val onRemoveWaypoint: (Int) -> Unit = {},
    val onTransportProfileChange: (TransportProfile) -> Unit = {},  // nuevo
    val isFavorite: Boolean = false,
    val favoriteMessage: String? = null,
    val onNavigateToFavorites: () -> Unit,
    val isTracking: Boolean = false,
    val trackingData: TrackingData = TrackingData(),
    val trackPoints: List<org.osmdroid.util.GeoPoint> = emptyList(),
    val onStartTracking: () -> Unit = {},
    val onStopTracking: () -> Unit = {},
)
