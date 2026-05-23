package com.airgo.airgoApp.features.tracking.data

import com.airgo.airgoApp.core.network.ApiEndpoints
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface TrackingApi {
    @POST(ApiEndpoints.SAVE_ACTIVITY)
    suspend fun saveActivity(
        @Body request: SaveActivityRequest,
    ): SaveActivityResponse

    @GET(ApiEndpoints.TITLES_AVAILABLE)
    suspend fun getAvailableTitles(): AvailableTitlesResponse

    @POST(ApiEndpoints.UNLOCK_TITLE)
    suspend fun unlockTitle(
        @Body request: UnlockTitleRequest,
    ): UnlockTitleResponse

    @PATCH(ApiEndpoints.USER_PROFILE)
    suspend fun updateUserStats(
        @Path("id") id: Int,
        @Body request: UpdateUserStatsRequest,
    )
}
