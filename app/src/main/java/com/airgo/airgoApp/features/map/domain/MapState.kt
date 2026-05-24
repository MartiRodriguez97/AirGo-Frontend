package com.airgo.airgoApp.features.map.domain

import com.airgo.airgoApp.features.map.data.PollutionPoint
import com.airgo.airgoApp.features.map.data.RouteResponse
import com.airgo.airgoApp.features.map.data.TransportProfile
import com.airgo.airgoApp.features.map.helpers.NominatimResult
import com.airgo.airgoApp.features.tracking.data.TrackingData
import org.osmdroid.util.GeoPoint
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
    val startPoint: GeoPoint?,
    val endPoint: GeoPoint?,
    val waypoints: List<GeoPoint> = emptyList(),
    val waypointAddresses: List<String> = emptyList(),
    val currentRoute: RouteResponse?,
    val isCalculatingRoute: Boolean,
    val transportProfile: TransportProfile = TransportProfile.BIKE,
    val onPointSelected: (GeoPoint) -> Unit,
    val onCalculateRoute: () -> Unit,
    val onClearRoute: () -> Unit,
    val onSetSelectionMode: (MapSelectionMode) -> Unit,
    val onToggleFavorite: () -> Unit,
    val onRemoveWaypoint: (Int) -> Unit = {},
    val onTransportProfileChange: (TransportProfile) -> Unit = {},
    val isFavorite: Boolean = false,
    val favoriteMessage: String? = null,
    val onNavigateToFavorites: () -> Unit,
    val isTracking: Boolean = false,
    val trackingData: TrackingData = TrackingData(),
    val trackPoints: List<GeoPoint> = emptyList(),
    val onStartTracking: () -> Unit = {},
    val onStopTracking: () -> Unit = {},
    val onNavigateToTrackingSummary: () -> Unit = {},
    val startAddressQuery: String = "",
    val endAddressQuery: String = "",
    val startSuggestions: List<NominatimResult> = emptyList(),
    val endSuggestions: List<NominatimResult> = emptyList(),
    val isSearchingAddress: Boolean = false,
    val onSearchStartAddress: (String) -> Unit = {},
    val onSearchEndAddress: (String) -> Unit = {},
    val onSelectStartAddress: (NominatimResult) -> Unit = {},
    val onSelectEndAddress: (NominatimResult) -> Unit = {},
    val onNavigateToAddressSearch: (String) -> Unit = {},
    val onSetWaypointEditMode: (Int) -> Unit = {},
    val editingWaypointIndex: Int = -1,
)
