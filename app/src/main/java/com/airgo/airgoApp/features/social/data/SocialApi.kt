package com.airgo.airgoApp.features.social.data

import com.airgo.airgoApp.core.network.ApiEndpoints
import com.airgo.airgoApp.features.social.friends.data.FriendProfileData
import com.airgo.airgoApp.features.social.friends.data.FriendRequestBody
import com.airgo.airgoApp.features.social.friends.data.FriendRequestResponse
import com.airgo.airgoApp.features.social.friends.data.FriendResponse
import com.airgo.airgoApp.features.social.friends.data.RespondFriendRequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface SocialApi {
    @GET(ApiEndpoints.FRIENDS)
    suspend fun getFriends(
        @Query("search") search: String? = null,
    ): List<FriendResponse>

    @GET(ApiEndpoints.SEARCH_USERS)
    suspend fun searchUsers(
        @Query("search") query: String,
    ): List<UserSearchResult>

    @POST(ApiEndpoints.FRIEND_REQUEST)
    suspend fun sendFriendRequest(
        @Body request: FriendRequestBody,
    ): Response<Unit>

    @GET(ApiEndpoints.FRIEND_REQUESTS)
    suspend fun getFriendRequests(): List<FriendRequestResponse>

    @PATCH
    suspend fun respondFriendRequest(
        @Url url: String,
        @Body body: RespondFriendRequestBody,
    ): Response<Unit>

    @GET(ApiEndpoints.USER_PROFILE)
    suspend fun getPublicProfile(
        @Path("id") id: Int,
    ): FriendProfileData

    @DELETE(ApiEndpoints.DELETE_FRIEND)
    suspend fun deleteFriend(
        @Path("amic_id") amicId: Int,
    ): Response<Unit>
}
