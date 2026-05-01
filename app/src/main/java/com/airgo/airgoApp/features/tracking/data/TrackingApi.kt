package com.airgo.airgoApp.features.tracking.data

import com.airgo.airgoApp.core.network.ApiEndpoints
import retrofit2.http.Body
import retrofit2.http.POST

interface TrackingApi {
    @POST(ApiEndpoints.SAVE_ACTIVITY)
    suspend fun saveActivity(
        @Body request: SaveActivityRequest,
    ): SaveActivityResponse
}
