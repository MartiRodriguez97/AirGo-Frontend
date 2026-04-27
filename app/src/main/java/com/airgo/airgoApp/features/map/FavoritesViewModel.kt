package com.airgo.airgoApp.features.map

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airgo.airgoApp.core.network.RetrofitProvider
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

    var errorMessage by mutableStateOf<String?>(null)
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
                Log.d(LOG_TAG, "Rutes preferides rebudes: ${routes.size}")
            } catch (e: IOException) {
                errorMessage = "Error de connexió al carregar les rutes preferides"
                Log.e(LOG_TAG, "Network failure: ${e.message}", e)
            } catch (e: HttpException) {
                errorMessage = "Error del servidor: ${e.code()}"
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
                    Log.d(LOG_TAG, "Ruta $routeId eliminada de preferits")
                } else {
                    errorMessage = "Error al eliminar la ruta"
                    Log.e(LOG_TAG, "Delete failed: ${response.code()}")
                }
            } catch (e: IOException) {
                errorMessage = "Error de connexió al eliminar la ruta"
                Log.e(LOG_TAG, "Network failure: ${e.message}", e)
            } catch (e: HttpException) {
                errorMessage = "Error del servidor: ${e.code()}"
                Log.e(LOG_TAG, "HTTP failure: ${e.message()}", e)
            }
        }
    }

    fun clearError() {
        errorMessage = null
    }
}
