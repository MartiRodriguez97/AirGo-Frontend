package com.airgo.airgoApp.features.map.domain

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airgo.airgoApp.R
import com.airgo.airgoApp.core.network.RetrofitProvider
import com.airgo.airgoApp.core.utils.UiText
import com.airgo.airgoApp.features.map.data.RouteRequest
import com.airgo.airgoApp.features.map.data.RouteResponse
import com.airgo.airgoApp.features.map.data.TransportProfile
import com.airgo.airgoApp.features.map.helpers.NominatimResult
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import retrofit2.HttpException
import java.io.IOException

private const val LOG_TAG = "RouteViewModel"

class RouteViewModel : ViewModel() {
    var startPoint by mutableStateOf<GeoPoint?>(null)
    var endPoint by mutableStateOf<GeoPoint?>(null)
    var waypoints by mutableStateOf<List<GeoPoint>>(emptyList())
    var transportProfile by mutableStateOf(TransportProfile.BIKE)
    var showBicing by mutableStateOf(true)
    var showPollution by mutableStateOf(true)

    var currentRoute by mutableStateOf<RouteResponse?>(null)
        private set
    var errorMessage by mutableStateOf<UiText?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set

    internal val favoriteHandler =
        FavoriteRouteHandler(
            scope = viewModelScope,
            onError = { errorMessage = it },
        )
    val addressHandler = AddressSearchHandler(viewModelScope)
    private val selectionHandler =
        PointSelectionHandler(
            scope = viewModelScope,
            onStartSelected = { point, address ->
                startPoint = point
                currentRoute = null
                addressHandler.startAddressQuery = address
            },
            onEndSelected = { point, address ->
                endPoint = point
                currentRoute = null
                addressHandler.endAddressQuery = address
            },
            onWaypointSelected = { point, address, index ->
                if (index >= waypoints.size) {
                    waypoints = waypoints + point
                } else {
                    waypoints = waypoints.toMutableList().also { it[index] = point }
                }
                currentRoute = null
                addressHandler.updateWaypointAddress(index, address)
            },
        )

    val isFavorite get() = favoriteHandler.isFavorite
    val favoriteMessage get() = favoriteHandler.favoriteMessage
    val selectionMode get() = selectionHandler.selectionMode
    val editingWaypointIndex get() = selectionHandler.editingWaypointIndex
    val startAddressQuery get() = addressHandler.startAddressQuery
    val endAddressQuery get() = addressHandler.endAddressQuery
    val startSuggestions get() = addressHandler.startSuggestions
    val endSuggestions get() = addressHandler.endSuggestions
    val isSearchingAddress get() = addressHandler.isSearchingAddress
    val waypointAddresses get() = addressHandler.waypointAddresses

    fun onPointSelected(point: org.osmdroid.util.GeoPoint) = selectionHandler.onPointSelected(point, waypoints.size)

    fun setWaypointEditMode(index: Int) = selectionHandler.setWaypointEditMode(index)

    fun onSetSelectionMode(mode: MapSelectionMode) {
        selectionHandler.selectionMode = mode
    }

    fun removeWaypoint(index: Int) {
        waypoints = waypoints.toMutableList().also { it.removeAt(index) }
        addressHandler.removeWaypointAddress(index)
        currentRoute = null
    }

    fun clearRoute() {
        startPoint = null
        endPoint = null
        waypoints = emptyList()
        currentRoute = null
        selectionHandler.selectionMode = MapSelectionMode.NONE
        selectionHandler.editingWaypointIndex = -1
        favoriteHandler.reset()
        addressHandler.clear()
    }

    fun calculateRoute() {
        errorMessage = null
        currentRoute = null
        val start = startPoint
        val end = endPoint
        if (start == null || end == null) {
            errorMessage = UiText.StringResource(R.string.error_route_selection_missing)
            return
        }
        viewModelScope.launch {
            isLoading = true
            favoriteHandler.reset()
            try {
                errorMessage = null
                val allPoints =
                    buildList {
                        add(listOf(start.latitude, start.longitude))
                        waypoints.forEach { add(listOf(it.latitude, it.longitude)) }
                        add(listOf(end.latitude, end.longitude))
                    }
                val request = RouteRequest(profile = transportProfile.value, points = allPoints)
                currentRoute = RetrofitProvider.routeGenerationApi.getCleanRoute(request)
                if (currentRoute?.status != "success") {
                    errorMessage = UiText.StringResource(R.string.error_fetching_route)
                }
            } catch (e: IOException) {
                errorMessage = UiText.StringResource(R.string.error_fetching_route)
                Log.e(LOG_TAG, "Network failure: ${e.message}", e)
            } catch (e: HttpException) {
                errorMessage = UiText.StringResource(R.string.error_fetching_route)
                Log.e(LOG_TAG, "HTTP failure: ${e.message()}", e)
            } finally {
                isLoading = false
            }
        }
    }

    fun clearError() {
        errorMessage = null
    }

    fun toggleFavorite() {
        favoriteHandler.toggle(currentRoute)
    }

    fun selectStartAddress(result: NominatimResult) {
        startPoint = addressHandler.selectStart(result)
        currentRoute = null
    }

    fun selectEndAddress(result: NominatimResult) {
        endPoint = addressHandler.selectEnd(result)
        currentRoute = null
    }

    fun selectWaypointAddress(
        result: NominatimResult,
        index: Int,
    ) {
        val point = addressHandler.selectWaypoint(result, index)
        if (index >= waypoints.size) {
            waypoints = waypoints + point
        } else {
            waypoints = waypoints.toMutableList().also { it[index] = point }
        }
        currentRoute = null
    }
}
