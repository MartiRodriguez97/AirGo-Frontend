package com.airgo.airgoApp.features.social.data

import com.airgo.airgoApp.features.social.data.model.UserSearchResult

class SocialRepository(private val socialApi: SocialApi) {

    suspend fun searchUsers(query: String): Result<List<UserSearchResult>> {
        return try {
            val results = socialApi.searchUsers(query)
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}