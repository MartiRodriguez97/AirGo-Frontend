package com.airgo.airgoApp.features.map.domain

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.airgo.airgoApp.R
import com.airgo.airgoApp.core.network.RetrofitProvider
import com.airgo.airgoApp.core.utils.UiText
import com.airgo.airgoApp.features.map.data.RouteResponse
import com.airgo.airgoApp.features.map.data.SaveFavoriteRouteRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

private const val LOG_TAG = "FavoriteRouteHandler"
private const val OK = 201
private const val JA_EXISTEIX = 409

class FavoriteRouteHandler(
    private val scope: CoroutineScope,
    private val onError: (UiText) -> Unit,
) {
    var isFavorite by mutableStateOf(false)
        private set

    var favoriteMessage by mutableStateOf<String?>(null)
        private set

    fun toggle(currentRoute: RouteResponse?) {
        if (isFavorite) remove(currentRoute) else add(currentRoute)
    }

    fun reset() {
        isFavorite = false
    }

    fun clearMessage() {
        favoriteMessage = null
    }

    private fun add(currentRoute: RouteResponse?) {
        val routeId = currentRoute?.routeId ?: return
        scope.launch {
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
                    JA_EXISTEIX -> onError(UiText.StringResource(R.string.error_route_already_saved))
                    else -> {
                        onError(UiText.StringResource(R.string.error_saving_route))
                        Log.e(LOG_TAG, "Save favorite failed: ${response.code()}")
                    }
                }
            } catch (e: IOException) {
                onError(UiText.StringResource(R.string.error_saving_route))
                Log.e(LOG_TAG, "Network error: ${e.message}", e)
            } catch (e: HttpException) {
                onError(UiText.StringResource(R.string.error_saving_route))
                Log.e(LOG_TAG, "HTTP error: ${e.code()}", e)
            }
        }
    }

    private fun remove(currentRoute: RouteResponse?) {
        val routeId = currentRoute?.routeId ?: return
        scope.launch {
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
}
