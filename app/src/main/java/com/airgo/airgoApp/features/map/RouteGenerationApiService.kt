package com.airgo.airgoApp.features.map

import com.airgo.airgoApp.core.network.ApiEndpoints
import retrofit2.http.Body
import retrofit2.http.POST

interface RouteGenerationApiService {
    @POST(ApiEndpoints.ROUTE_GENERATION)
    suspend fun getCleanRoute(
        @Body request: RouteRequest,
    ): RouteResponse
}

