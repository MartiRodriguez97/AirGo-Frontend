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

class BicingViewModel : ViewModel() {
    var stations by mutableStateOf<List<BicingStation>>(emptyList())
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun fetchStations(
        lat: Double,
        lon: Double,
        radio: Int = 5,
    ) {
        viewModelScope.launch {
            try {
                errorMessage = null
                stations = RetrofitProvider.bicingApi.getBicingStations(lat, lon, radio)
                Log.d("BicingViewModel", "Conexió exitosa, stations rebuts: ${stations.size}")
                if (stations.isNotEmpty()) {
                    Log.d("BicingViewModel", "Primera estació rebuda: ${stations.first()}")
                }
            } catch (e: IOException) {
                errorMessage = "Error de connexió al carregar el Bicing. Revisa la teva xarxa."
                e.printStackTrace()
            } catch (e: HttpException) {
                errorMessage = "Error del servidor al carregar el Bicing: ${e.code()}"
                e.printStackTrace()
            } catch (e: Exception) {
                errorMessage = "S'ha produït un error inesperat carregant el Bicing."
                e.printStackTrace()
            }
        }
    }

    fun clearError() {
        errorMessage = null
    }
}
