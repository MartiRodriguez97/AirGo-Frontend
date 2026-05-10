package com.airgo.airgoApp.features.onboarding.data

import com.airgo.airgoApp.core.network.ApiEndpoints
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

interface OnboardingApi {
    // Endpoint per obtenir la informació del perfil de l'usuari
    @GET(ApiEndpoints.USER_ACCOUNT_INFO)
    suspend fun getUserProfile(): UserProfileResponse

    // Endpoint per actualitzar el perfil de l'usuari després de completar l'onboarding
    @PATCH(ApiEndpoints.USER_PROFILE)
    suspend fun updateUserProfile(
        @Path("id") id: Int,
        @Body request: OnboardingRequest,
    ): UserProfileResponse
}
