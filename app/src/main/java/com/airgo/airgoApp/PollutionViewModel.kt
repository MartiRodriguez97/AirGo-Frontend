package com.airgo.airgoApp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private const val BCN_Lat = 41.3851
private const val BCN_Lon = 2.1734
/**
 * ViewModel per gestionar les dades de contaminació.
 * S'encarrega de fer la crida a l'API i mantenir l'estat de les estacions.
 * Utilitza Retrofit per fer les peticions HTTP i Gson per convertir les respostes JSON en objectes Kotlin.
 */
class PollutionViewModel : ViewModel() {
    // Llista de les estacions de contaminació obtingudes de l'API
    var stations by mutableStateOf<List<PollutionPoint>>(emptyList())
        private set

    // Configuració de Retrofit per accedir a l'API de qualitat de l'aire
    private val retrofit = Retrofit.Builder()
        .baseUrl("http://localhost:8080/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // Creació de l'instància del servei de l'API
    private val apiService = retrofit.create(AirQualityApiService::class.java)

    // Funció per obtenir les dades de contaminació de Barcelona
    fun fetchBarcelonaData() {
        viewModelScope.launch {
            try {
                stations = apiService.getPollutionData(BCN_Lat, BCN_Lon)
            } catch (e: Exception) {
                // Gestionar error de connexió
            }
        }
    }
}