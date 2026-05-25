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

    fun fetchRoute(
        start: GeoPoint,
        end: GeoPoint,
    ) {
        viewModelScope.launch {
            isLoading = true
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
            }
            MapSelectionMode.END -> {
                endPoint = point
                currentRoute = null
                selectionMode = MapSelectionMode.NONE
            }
            MapSelectionMode.WAYPOINT -> {
                waypoints = waypoints + point
                currentRoute = null
                selectionMode = MapSelectionMode.NONE
            }
            MapSelectionMode.NONE -> {}
        }
    }

    fun addWaypoint() {
        selectionMode = MapSelectionMode.WAYPOINT
    }

    fun removeWaypoint(index: Int) {
        waypoints = waypoints.toMutableList().also { it.removeAt(index) }
        currentRoute = null
    }

    fun clearRoute() {
        startPoint = null
        endPoint = null
        waypoints = emptyList()
        currentRoute = null
        selectionMode = MapSelectionMode.NONE
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

    fun clearFavoriteMessage() {
        favoriteMessage = null
    }
}
