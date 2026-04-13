package com.airgo.airgoApp.features.map

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airgo.airgoApp.core.network.RetrofitProvider
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import retrofit2.HttpException
import java.io.IOException

private const val LOG_TAG = "RouteViewModel"

class RouteViewModel : ViewModel() {
    var selectionMode by mutableStateOf(MapSelectionMode.NONE)
    var startPoint by mutableStateOf<GeoPoint?>(null)
    var endPoint by mutableStateOf<GeoPoint?>(null)

    // Ruta actual obtinguda de l'API
    var currentRoute by mutableStateOf<RouteResponse?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    /**
     * Funció per fer la crida a l'API i obtenir la ruta entre els punts seleccionats.
     */
    fun fetchRoute(
        start: GeoPoint,
        end: GeoPoint,
    ) {
        viewModelScope.launch {
            isLoading = true
            try {
                errorMessage = null // Netegem errors previs
                val request = RouteRequest(start.latitude, start.longitude, end.latitude, end.longitude)
                currentRoute = RetrofitProvider.routeGenerationApi.getCleanRoute(request)
                Log.d("Conexió exitosa", "La distància és de: ${currentRoute?.summary?.distanceMeters} metres")
            } catch (e: IOException) {
                // Error de xarxa (ex: no hi ha internet o servidor apagat)
                errorMessage = "Error de connexió al servidor"
                Log.e(LOG_TAG, "Network failure: ${e.message}", e)
            } catch (e: HttpException) {
                // El servidor ha respost però amb un error (ex: 404 o 500)
                errorMessage = "El servidor ha donat un error: ${e.code()}"
                Log.e(LOG_TAG, "HTTP failure: ${e.message()}", e)
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * Funció per gestionar la selecció de punts inici o final al mapa.
     */
    fun onPointSelected(point: GeoPoint) {
        if (selectionMode == MapSelectionMode.START) {
            startPoint = point
            selectionMode = MapSelectionMode.NONE // Tornem a mode normal
        } else if (selectionMode == MapSelectionMode.END) {
            endPoint = point
            selectionMode = MapSelectionMode.NONE
        }
    }

    /**
     * Funció per netejar la ruta actual i els punts seleccionats, tornant a l'estat inicial.
     */
    fun clearRoute() {
        startPoint = null
        endPoint = null
        currentRoute = null
        selectionMode = MapSelectionMode.NONE
    }

    /**
     * Funció per calcular la ruta entre els punts seleccionats.
     * Si ambdós punts estan definits, es fa la crida a l'API per obtenir la ruta.
     */
    fun calculateRoute() {
        val start = startPoint
        val end = endPoint
        if (start != null && end != null) {
            fetchRoute(start, end)
        } else {
            errorMessage = "Selecciona un punt d'origen i un de destí per calcular la ruta."
        }
    }

    /**
     * Netejar l'error per no mostrar el missatge múltiples vegades
     */
    fun clearError() {
        errorMessage = null
    }
}
