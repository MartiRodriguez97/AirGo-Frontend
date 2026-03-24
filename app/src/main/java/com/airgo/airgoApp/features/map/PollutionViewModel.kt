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

private const val BCN_LAT = 41.3851
private const val BCN_LON = 2.1734

private const val LOG_TAG = "PollutionViewModel"

/**
 * ViewModel per gestionar les dades de contaminació.
 * S'encarrega de fer la crida a l'API i mantenir l'estat de les estacions.
 * Utilitza Retrofit per fer les peticions HTTP i Gson per convertir les respostes JSON en objectes Kotlin.
 */
class PollutionViewModel : ViewModel() {
    // Llista de les estacions de contaminació obtingudes de l'API
    var stations by mutableStateOf<List<PollutionPoint>>(emptyList())
        private set

    // Missatge d'error en cas que la crida a l'API falli
    var errorMessage by mutableStateOf<String?>(null)
        private set

    // Funció per obtenir les dades de contaminació de Barcelona
    fun fetchBarcelonaData() {
        viewModelScope.launch {
            try {
                errorMessage = null // Netegem errors previs
                stations = RetrofitProvider.airQualityApi.getPollutionData(BCN_LAT, BCN_LON)
                Log.d("Conexió exitosa", "Nombre d'estacions de qualitat d'aire: ${stations.size} estacions")
            } catch (e: IOException) {
                errorMessage = "Error de connexió"
                Log.e(LOG_TAG, "Network failure", e)
            } catch (e: HttpException) {
                errorMessage = "Error: ${e.code()}"
                Log.e(LOG_TAG, "HTTP failure", e)
            }
        }
    }
}
