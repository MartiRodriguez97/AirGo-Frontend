package com.airgo.airgoApp.features.map.data

import com.airgo.airgoApp.core.network.ApiEndpoints
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Interfíce de Retrofit per accedir a l'API de qualitat de l'aire.
 * Aquesta interfíce defineix una funció per obtenir les dades de contaminació basades en la latitud i longitud.
 */
interface AirQualityApiService {
    @GET(ApiEndpoints.AIR_QUALITY)
    suspend fun getPollutionData(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("radio") radio: Double,
    ): List<PollutionPoint>
}
