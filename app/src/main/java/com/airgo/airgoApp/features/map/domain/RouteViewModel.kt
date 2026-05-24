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
import com.airgo.airgoApp.features.map.data.SaveFavoriteRouteRequest
import com.airgo.airgoApp.features.map.data.TransportProfile
import com.airgo.airgoApp.features.map.helpers.NominatimResult
import com.airgo.airgoApp.features.map.helpers.NominatimService
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import retrofit2.HttpException
import java.io.IOException

private const val LOG_TAG = "RouteViewModel"
private const val OK = 201
private const val JA_EXISTEIX = 409

class RouteViewModel : ViewModel() {
    var selectionMode by mutableStateOf(MapSelectionMode.NONE)
    var startPoint by mutableStateOf<GeoPoint?>(null)
    var endPoint by mutableStateOf<GeoPoint?>(null)
    var waypoints by mutableStateOf<List<GeoPoint>>(emptyList())
    var transportProfile by mutableStateOf(TransportProfile.BIKE) // nuevo

    var currentRoute by mutableStateOf<RouteResponse?>(null)
        private set
    var errorMessage by mutableStateOf<UiText?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var isFavorite by mutableStateOf(false)
        private set
    var favoriteMessage by mutableStateOf<String?>(null)
        private set
    var startAddressQuery by mutableStateOf("")
    var endAddressQuery by mutableStateOf("")
    var startSuggestions by mutableStateOf<List<NominatimResult>>(emptyList())
    var endSuggestions by mutableStateOf<List<NominatimResult>>(emptyList())
    var isSearchingAddress by mutableStateOf(false)
    var waypointAddresses by mutableStateOf<List<String>>(emptyList())
    var editingWaypointIndex by mutableStateOf(-1)

    fun fetchRoute(
        start: GeoPoint,
        end: GeoPoint,
    ) {
        viewModelScope.launch {
            isLoading = true
            isFavorite = false
            try {
                errorMessage = null

                val allPoints =
                    buildList {
                        add(listOf(start.latitude, start.longitude))
                        waypoints.forEach { add(listOf(it.latitude, it.longitude)) }
                        add(listOf(end.latitude, end.longitude))
                    }

                val request =
                    RouteRequest(
                        profile = transportProfile.value,
                        points = allPoints,
                    )

                currentRoute = RetrofitProvider.routeGenerationApi.getCleanRoute(request)
                if (currentRoute?.status !== "success") {
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

    fun onPointSelected(point: GeoPoint) {
        when (selectionMode) {
            MapSelectionMode.START -> {
                startPoint = point
                currentRoute = null
                selectionMode = MapSelectionMode.NONE
                viewModelScope.launch {
                    startAddressQuery = NominatimService.reverseGeocode(point.latitude, point.longitude) ?: ""
                }
            }
            MapSelectionMode.END -> {
                endPoint = point
                currentRoute = null
                selectionMode = MapSelectionMode.NONE
                viewModelScope.launch {
                    endAddressQuery = NominatimService.reverseGeocode(point.latitude, point.longitude) ?: ""
                }
            }
            MapSelectionMode.WAYPOINT -> {
                val index = if (editingWaypointIndex >= 0) editingWaypointIndex else waypoints.size
                if (editingWaypointIndex >= 0 && editingWaypointIndex < waypoints.size) {
                    waypoints = waypoints.toMutableList().also { it[index] = point }
                } else {
                    waypoints = waypoints + point
                }
                currentRoute = null
                editingWaypointIndex = -1
                selectionMode = MapSelectionMode.NONE
                viewModelScope.launch {
                    val address = NominatimService.reverseGeocode(point.latitude, point.longitude) ?: ""
                    waypointAddresses = waypointAddresses.toMutableList().also {
                        if (index < it.size) it[index] = address else it.add(address)
                    }
                }
            }
            MapSelectionMode.NONE -> {}
        }
    }

    fun addWaypoint() {
        selectionMode = MapSelectionMode.WAYPOINT
    }

    fun removeWaypoint(index: Int) {
        waypoints = waypoints.toMutableList().also { it.removeAt(index) }
        waypointAddresses = waypointAddresses.toMutableList().also {
            if (index < it.size) it.removeAt(index)
        }
        currentRoute = null
    }

    fun clearRoute() {
        startPoint = null
        endPoint = null
        waypoints = emptyList()
        waypointAddresses = emptyList()
        currentRoute = null
        selectionMode = MapSelectionMode.NONE
        isFavorite = false
    }

    fun calculateRoute() {
        currentRoute = null
        val start = startPoint
        val end = endPoint
        if (start != null && end != null) {
            fetchRoute(start, end)
        } else {
            errorMessage = UiText.StringResource(R.string.error_route_selection_missing)
        }
    }

    fun clearError() {
        errorMessage = null
    }

    fun addToFavorites() {
        val routeId = currentRoute?.routeId ?: return
        viewModelScope.launch {
            try {
                val response =
                    RetrofitProvider.favoriteRouteApi.saveFavoriteRoute(
                        SaveFavoriteRouteRequest(routeId),
                    )
                when (response.code()) {
                    OK -> {
                        isFavorite = true
                        favoriteMessage = null
                    }
                    JA_EXISTEIX -> errorMessage = UiText.StringResource(R.string.error_route_already_saved)
                    else -> {
                        errorMessage = UiText.StringResource(R.string.error_saving_route)
                        Log.e(LOG_TAG, "Save favorite failed: ${response.code()}")
                    }
                }
            } catch (e: IOException) {
                Log.e(LOG_TAG, "Network error fetching routes: ${e.message}", e)
                errorMessage = UiText.StringResource(R.string.error_saving_route)
            } catch (e: HttpException) {
                Log.e(LOG_TAG, "HTTP error fetching routes: ${e.code()}", e)
                errorMessage = UiText.StringResource(R.string.error_saving_route)
            }
        }
    }

    fun toggleFavorite() {
        if (isFavorite) removeFromFavorites() else addToFavorites()
    }

    private fun removeFromFavorites() {
        val routeId = currentRoute?.routeId ?: return
        viewModelScope.launch {
            try {
                RetrofitProvider.favoriteRouteApi.deleteFavoriteRoute(routeId)
                isFavorite = false
            } catch (e: IOException) {
                Log.e(LOG_TAG, "Network error removing favorite: ${e.message}", e)
            } catch (e: HttpException) {
                Log.e(LOG_TAG, "HTTP error removing favorite: ${e.code()}", e)
            }
        }
    }

    fun searchStartAddress(query: String) {
        startAddressQuery = query
        if (query.length < 3) { startSuggestions = emptyList(); return }
        viewModelScope.launch {
            isSearchingAddress = true
            startSuggestions = NominatimService.searchAddress(query)
            isSearchingAddress = false
        }
    }

    fun searchEndAddress(query: String) {
        endAddressQuery = query
        if (query.length < 3) { endSuggestions = emptyList(); return }
        viewModelScope.launch {
            isSearchingAddress = true
            endSuggestions = NominatimService.searchAddress(query)
            isSearchingAddress = false
        }
    }

    fun selectStartAddress(result: NominatimResult) {
        startPoint = GeoPoint(result.lat.toDouble(), result.lon.toDouble())
        startAddressQuery = result.displayName
        startSuggestions = emptyList()
        currentRoute = null
    }

    fun selectEndAddress(result: NominatimResult) {
        endPoint = GeoPoint(result.lat.toDouble(), result.lon.toDouble())
        endAddressQuery = result.displayName
        endSuggestions = emptyList()
        currentRoute = null
    }

    fun selectWaypointAddress(result: NominatimResult, index: Int) {
        val point = GeoPoint(result.lat.toDouble(), result.lon.toDouble())
        if (index >= waypoints.size) {
            waypoints = waypoints + point
            waypointAddresses = waypointAddresses + result.displayName
        } else {
            waypoints = waypoints.toMutableList().also { it[index] = point }
            waypointAddresses = waypointAddresses.toMutableList().also { it[index] = result.displayName }
        }
        currentRoute = null
    }

    fun clearFavoriteMessage() {
        favoriteMessage = null
    }

    fun setWaypointEditMode(index: Int) {
        editingWaypointIndex = index
        selectionMode = MapSelectionMode.WAYPOINT
    }
}
