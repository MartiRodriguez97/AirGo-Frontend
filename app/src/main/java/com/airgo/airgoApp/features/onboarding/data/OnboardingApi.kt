package com.airgo.airgoApp.features.onboarding.data

import com.airgo.airgoApp.core.network.ApiEndpoints
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

interface OnboardingApi {
    @GET(ApiEndpoints.USER_ACCOUNT_INFO)
    suspend fun getUserProfile(): UserProfileResponse

    @PATCH(ApiEndpoints.ONBOARDING_UPDATE)
    suspend fun updateUserProfile(
        @Path("id") id: Int,
        @Body request: OnboardingRequest,
    ): UserProfileResponse
}