package com.airgo.airgoApp.features.map

import com.airgo.airgoApp.core.network.ApiEndpoints
import retrofit2.http.GET
import retrofit2.http.Query

interface BicingApiService {
    @GET(ApiEndpoints.BICING)
    suspend fun getBicingStations(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("radio") radio: Int = 5,
    ): List<BicingStation>
}
