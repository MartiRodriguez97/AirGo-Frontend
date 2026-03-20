package com.airgo.airgoApp.features.profile.data

import android.media.Image
import com.airgo.airgoApp.core.network.ApiEndpoints
import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.Part

data class UpdateProfilePictureRequest(
    val image: Image,
)

data class ProfilePictureResponse(
    @SerializedName("profile_pic")
    val newPictureUrl: String,
)

interface ProfileApi {
    // Define API endpoints related to user profile here
    @DELETE(ApiEndpoints.DELETE_ACCOUNT)
    suspend fun deleteAccount(): Response<Unit>

    @Multipart
    @PATCH(ApiEndpoints.CHANGE_PROFILE_PICTURE)
    suspend fun updateProfilePicture(
        @Part image: MultipartBody.Part,
    ): Response<ProfilePictureResponse> // O la clase de respuesta que te devuelvan (ej. ProfileResponse)
}
