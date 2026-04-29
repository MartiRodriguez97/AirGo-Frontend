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
import com.airgo.airgoApp.features.map.data.WaypointRequest
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
    var waypoints by mutableStateOf<List<GeoPoint>>(emptyList()) // nuevo

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
                val request =
                    RouteRequest(
                        latStart = start.latitude,
                        lonStart = start.longitude,
                        latEnd = end.latitude,
                        lonEnd = end.longitude,
                        waypoints = waypoints.map { WaypointRequest(it.latitude, it.longitude) },
                    )
                currentRoute = RetrofitProvider.routeGenerationApi.getCleanRoute(request)
                Log.d("Connexió exitosa", "Distància: ${currentRoute?.summary?.distanceMeters} m")
            } catch (e: IOException) {
                errorMessage = UiText.StringResource(R.string.error_connection)
                Log.e(LOG_TAG, "Network failure: ${e.message}", e)
            } catch (e: HttpException) {
                errorMessage = UiText.StringResource(R.string.error_route_http, e.code())
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
                    JA_EXISTEIX -> favoriteMessage = "La ruta ja estava guardada"
                    else -> {
                        favoriteMessage = "Error al guardar la ruta: ${response.code()}"
                        Log.e(LOG_TAG, "Save favorite failed: ${response.code()}")
                    }
                }
            } catch (e: IOException) {
                favoriteMessage = "Error de connexió: ${e.message}"
            } catch (e: HttpException) {
                favoriteMessage = "Error del servidor: ${e.code()}"
            }
        }
    }

    fun clearFavoriteMessage() {
        favoriteMessage = null
    }
}
