package com.airgo.airgoApp.features.activity.data

import com.airgo.airgoApp.core.network.ApiEndpoints
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

interface ActivityApi {
    @GET(ApiEndpoints.SAVE_ACTIVITY)
    suspend fun getActivities(): List<ActivityResponse>

    @DELETE(ApiEndpoints.ACTIVITY)
    suspend fun deleteActivity(
        @Path("id") id: Int,
    ): Response<Void>

    @PATCH(ApiEndpoints.ACTIVITY)
    suspend fun updateActivitySensation(
        @Path("id") id: Int,
        @Body request: UpdateSensationRequest,
    ): Response<Void>

    @PATCH(ApiEndpoints.ACTIVITY)
    suspend fun updateActivityComment(
        @Path("id") id: Int,
        @Body request: UpdateCommentRequest,
    ): Response<Void>
}
