package com.airgo.airgoApp.map

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
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

    // Configuració de Retrofit per accedir a l'API de qualitat de l'aire
    private val retrofit =
        Retrofit
            .Builder()
            .baseUrl("http://nattech.fib.upc.edu:40330/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    // Creació de la instància del servei de l'API
    private val apiService = retrofit.create(AirQualityApiService::class.java)

    // Funció per obtenir les dades de contaminació de Barcelona
    fun fetchBarcelonaData() {
        viewModelScope.launch {
            try {
                errorMessage = null // Netegem errors previs
                stations = apiService.getPollutionData(BCN_LAT, BCN_LON)
            } catch (e: IOException) {
                // Error de xarxa (ex: no hi ha internet o servidor apagat)
                errorMessage = "Error de connexió al servidor"
                Log.e(LOG_TAG, "Network failure: ${e.message}", e)
            } catch (e: HttpException) {
                // El servidor ha respost però amb un error (ex: 404 o 500)
                errorMessage = "El servidor ha donat un error: ${e.code()}"
                Log.e(LOG_TAG, "HTTP failure: ${e.message()}", e)
            }
        }
    }
}
