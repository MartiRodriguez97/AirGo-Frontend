package com.airgo.airgoApp.features.tracking.data

import com.airgo.airgoApp.core.network.ApiEndpoints
import retrofit2.http.Body
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface TrackingApi {
    @POST(ApiEndpoints.SAVE_ACTIVITY)
    suspend fun saveActivity(
        @Body request: SaveActivityRequest,
    ): SaveActivityResponse

    @PATCH(ApiEndpoints.USER_PROFILE)
    suspend fun updateUserStats(
        @Path("id") id: Int,
        @Body request: UpdateUserStatsRequest,
    )
}
