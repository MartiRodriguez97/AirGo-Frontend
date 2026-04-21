package com.airgo.airgoApp.features.social

import com.airgo.airgoApp.features.social.UserSearchResult
import retrofit2.HttpException
import java.io.IOException

class SocialRepository(
    private val socialApi: SocialApi,
) {
    suspend fun searchUsers(query: String): Result<List<UserSearchResult>> =
        try {
            val results = socialApi.searchUsers(query)
            Result.success(results)
        } catch (e: IOException) {
            Result.failure(e) // network error
        } catch (e: HttpException) {
            Result.failure(e) // API error (non-2xx)
        }
}
