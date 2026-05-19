package com.airgo.airgoApp.features.activity.data

import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Body
import retrofit2.http.PATCH
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
        @Body request: UpdateSensationRequest
    ): Response<Void>
}
