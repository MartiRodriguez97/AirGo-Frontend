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
import com.airgo.airgoApp.features.map.data.FavoriteRouteDetail
import com.airgo.airgoApp.features.map.data.FavoriteRouteItem
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

private const val LOG_TAG = "FavoritesViewModel"

private const val OK = 204

class FavoritesViewModel : ViewModel() {
    var routes by mutableStateOf<List<FavoriteRouteItem>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<UiText?>(null)
        private set

    var selectedRoute by mutableStateOf<FavoriteRouteDetail?>(null)
        private set

    /**
     * Obté les rutes preferides de l'usuari. Opcionalment filtra per cerca.
     */
    fun fetchFavoriteRoutes(search: String? = null) {
        viewModelScope.launch {
            isLoading = true
            try {
                errorMessage = null
                routes = RetrofitProvider.favoriteRouteApi.getFavoriteRoutes(search)
            } catch (e: IOException) {
                errorMessage = UiText.StringResource(R.string.error_fetching_routes)
                Log.e(LOG_TAG, "Network failure: ${e.message}", e)
            } catch (e: HttpException) {
                errorMessage = UiText.StringResource(R.string.error_fetching_routes)
                Log.e(LOG_TAG, "HTTP failure: ${e.message()}", e)
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * Elimina una ruta de preferits per ID.
     */
    fun deleteFavoriteRoute(routeId: Int) {
        viewModelScope.launch {
            try {
                val response = RetrofitProvider.favoriteRouteApi.deleteFavoriteRoute(routeId)
                if (response.code() == OK) {
                    routes = routes.filter { it.route.id != routeId }
                } else {
                    errorMessage = UiText.StringResource(R.string.error_deleting_favourite_route)
                }
            } catch (e: IOException) {
                errorMessage = UiText.StringResource(R.string.error_deleting_favourite_route)
                Log.e(LOG_TAG, "Network failure: ${e.message}", e)
            } catch (e: HttpException) {
                errorMessage = UiText.StringResource(R.string.error_deleting_favourite_route)
                Log.e(LOG_TAG, "HTTP failure: ${e.message()}", e)
            }
        }
    }

    fun clearError() {
        errorMessage = null
    }

    fun selectRoute(route: FavoriteRouteDetail) {
        selectedRoute = route
    }

    fun clearSelectedRoute() {
        selectedRoute = null
    }
}
