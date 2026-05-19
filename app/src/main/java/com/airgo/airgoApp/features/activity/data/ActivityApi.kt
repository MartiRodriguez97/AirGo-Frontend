package com.airgo.airgoApp.features.activity.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

interface ActivityApi {
    @GET("api/exercicis/")
    suspend fun getActivities(): List<ActivityResponse>

    @DELETE("api/exercicis/{id}/")
    suspend fun deleteActivity(
        @Path("id") id: Int,
    ): Response<Void>

    @PATCH("api/exercicis/{id}/")
    suspend fun updateActivitySensation(
        @Path("id") id: Int,
        @Body request: UpdateSensationRequest,
    ): Response<Void>

    @PATCH("api/exercicis/{id}/")
    suspend fun updateActivityComment(
        @Path("id") id: Int,
        @Body request: UpdateCommentRequest,
    ): Response<Void>
}
