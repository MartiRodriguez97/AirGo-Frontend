package com.airgo.airgoApp.features.map

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Interfíce de Retrofit per accedir a l'API de qualitat de l'aire.
 * Aquesta interfíce defineix una funció per obtenir les dades de contaminació basades en la latitud i longitud.
 */
interface AirQualityApiService {
    @GET("air-quality/")
    suspend fun getPollutionData(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
    ): List<PollutionPoint>
}
