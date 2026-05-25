package com.airgo.airgoApp.features.social

import android.annotation.SuppressLint
import com.airgo.airgoApp.core.network.ApiEndpoints
import com.airgo.airgoApp.features.social.data.SocialApi
import com.airgo.airgoApp.features.social.data.UserSearchResult
import com.airgo.airgoApp.features.social.friends.data.FriendRequestBody
import com.airgo.airgoApp.features.social.friends.data.FriendRequestResponse
import com.airgo.airgoApp.features.social.friends.data.FriendResponse
import com.airgo.airgoApp.features.social.friends.data.RespondFriendRequestBody
import retrofit2.HttpException
import java.io.IOException

class SocialRepository(
    private val socialApi: SocialApi,
) {
    suspend fun getFriends(search: String? = null): Result<List<FriendResponse>> =
        try {
            Result.success(socialApi.getFriends(search = search?.takeIf { it.isNotBlank() }))
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: HttpException) {
            Result.failure(e)
        }

    suspend fun searchUsers(query: String): Result<List<UserSearchResult>> =
        try {
            Result.success(socialApi.searchUsers(query))
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: HttpException) {
            Result.failure(e)
        }

    private val httpMethodNotAllowed = java.net.HttpURLConnection.HTTP_BAD_METHOD

    @SuppressLint("RestrictedApi")
    suspend fun sendFriendRequest(receptorId: Int): Result<Unit> =
        try {
            val response = socialApi.sendFriendRequest(FriendRequestBody(receptorId))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error ${response.code()}"))
            }
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: HttpException) {
            Result.failure(e)
        }

    suspend fun deleteFriend(amicId: Int): Result<Unit> =
        try {
            val response = socialApi.deleteFriend(amicId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error ${response.code()}"))
            }
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: HttpException) {
            Result.failure(e)
        }

    suspend fun getFriendRequests(): Result<List<FriendRequestResponse>> =
        try {
            Result.success(socialApi.getFriendRequests())
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: HttpException) {
            if (e.code() == httpMethodNotAllowed) {
                Result.success(emptyList())
            } else {
                Result.failure(e)
            }
        }

    suspend fun respondFriendRequest(
        amistatId: Int,
        accio: String,
    ): Result<Unit> =
        try {
            val response =
                socialApi.respondFriendRequest(
                    url = ApiEndpoints.getFriendRespondEndpoint(amistatId),
                    body = RespondFriendRequestBody(accio),
                )
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error ${response.code()}"))
            }
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: HttpException) {
            Result.failure(e)
        }
}
