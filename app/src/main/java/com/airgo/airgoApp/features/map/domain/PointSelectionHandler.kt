package com.airgo.airgoApp.features.map.domain

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.airgo.airgoApp.features.map.helpers.NominatimService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

class PointSelectionHandler(
    private val scope: CoroutineScope,
    private val onStartSelected: (GeoPoint, String) -> Unit,
    private val onEndSelected: (GeoPoint, String) -> Unit,
    private val onWaypointSelected: (GeoPoint, String, Int) -> Unit,
) {
    var selectionMode by mutableStateOf(MapSelectionMode.NONE)
    var editingWaypointIndex by mutableStateOf(-1)

    fun onPointSelected(
        point: GeoPoint,
        currentWaypointCount: Int,
    ) {
        when (selectionMode) {
            MapSelectionMode.START -> {
                selectionMode = MapSelectionMode.NONE
                scope.launch {
                    val address = NominatimService.reverseGeocode(point.latitude, point.longitude) ?: ""
                    onStartSelected(point, address)
                }
            }
            MapSelectionMode.END -> {
                selectionMode = MapSelectionMode.NONE
                scope.launch {
                    val address = NominatimService.reverseGeocode(point.latitude, point.longitude) ?: ""
                    onEndSelected(point, address)
                }
            }
            MapSelectionMode.WAYPOINT -> {
                val index = if (editingWaypointIndex >= 0) editingWaypointIndex else currentWaypointCount
                editingWaypointIndex = -1
                selectionMode = MapSelectionMode.NONE
                scope.launch {
                    val address = NominatimService.reverseGeocode(point.latitude, point.longitude) ?: ""
                    onWaypointSelected(point, address, index)
                }
            }
            MapSelectionMode.NONE -> {}
        }
    }

    fun setWaypointEditMode(index: Int) {
        editingWaypointIndex = index
        selectionMode = MapSelectionMode.WAYPOINT
    }
}
