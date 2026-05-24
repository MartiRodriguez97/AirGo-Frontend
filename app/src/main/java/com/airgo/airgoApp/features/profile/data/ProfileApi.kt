package com.airgo.airgoApp.features.profile.data

import android.media.Image
import com.airgo.airgoApp.core.network.ApiEndpoints
import com.airgo.airgoApp.features.onboarding.data.UserProfileResponse
import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.Part
import retrofit2.http.Path

data class UpdateProfilePictureRequest(
    val image: Image,
)

data class ProfilePictureResponse(
    @SerializedName("profile_pic")
    val newPictureUrl: String,
)

data class BadgeDefinition(
    val id: Int,
    val nom: String,
    val descripcio: String,
    val icona: String?,
    val tipus: String,
    @SerializedName("valor_requerit")
    val valorRequerit: Int,
)

data class UserBadgeResponse(
    val insignia: BadgeDefinition,
    @SerializedName("data_guanyada")
    val dataGuanyada: String,
)

data class UpdateUserRequest(
    val titol: String? = null,
    val pes: Float? = null,
    val altura: Int? = null,
    val nom: String? = null,
    val idioma: String? = null,
)

interface ProfileApi {
    // Define API endpoints related to user profile here
    @GET(ApiEndpoints.USER_ACCOUNT_INFO)
    suspend fun getUserProfile(): UserProfileResponse

    @GET(ApiEndpoints.INSIGNIES)
    suspend fun getUserBadges(): List<UserBadgeResponse>

    @DELETE(ApiEndpoints.DELETE_ACCOUNT)
    suspend fun deleteAccount(): Response<Void>

    @Multipart
    @PATCH(ApiEndpoints.CHANGE_PROFILE_PICTURE)
    suspend fun updateProfilePicture(
        @Part image: MultipartBody.Part,
    ): Response<ProfilePictureResponse> // O la clase de resposta que te devuelvan (ej. ProfileResponse)

    @PATCH(ApiEndpoints.USER_PROFILE)
    suspend fun updateUser(
        @Path("id") id: Int,
        @Body request: UpdateUserRequest,
    ): Response<Void>
}
