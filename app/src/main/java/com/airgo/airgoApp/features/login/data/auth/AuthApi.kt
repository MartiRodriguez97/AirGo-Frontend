package com.airgo.airgoApp.features.login.data.auth

import com.airgo.airgoApp.core.network.ApiEndpoints
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

data class AuthResponse(
    val user: String?,
    val picture: String?,
    val access: String?,
    val refresh: String?,
)

data class GoogleTokenRequest(
    val token: String,
)

data class UserDetailResponse(
    val id: Int,
    @com.google.gson.annotations.SerializedName("profile_pic")
    val profilePic: String?,
    val username: String,
    val punts: Int,
    val teBici: Boolean,
    val pes: Double,
    val altura: Double,
    val ratxa: Int,
    val idioma: String,
    val limitRutes: Int,
    val titol: String,
    val insignies: String?,
)

interface AuthApi {
    @POST(ApiEndpoints.GOOGLE_LOGIN)
    suspend fun loginWithGoogle(
        @Body body: GoogleTokenRequest,
    ): Response<AuthResponse>

    @GET(ApiEndpoints.USERS_LIST)
    suspend fun searchUsers(
        @Query("search") search: String,
    ): Response<List<UserDetailResponse>>
}
