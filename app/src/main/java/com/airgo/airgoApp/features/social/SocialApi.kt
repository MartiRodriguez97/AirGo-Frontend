package com.airgo.airgoApp.features.social

import com.airgo.airgoApp.core.network.ApiEndpoints
import com.airgo.airgoApp.features.social.UserSearchResult
import retrofit2.http.GET
import retrofit2.http.Query

interface SocialApi {
    @GET(ApiEndpoints.SEARCH_USERS)
    suspend fun searchUsers(
        @Query("search") query: String,
    ): List<UserSearchResult>
}
