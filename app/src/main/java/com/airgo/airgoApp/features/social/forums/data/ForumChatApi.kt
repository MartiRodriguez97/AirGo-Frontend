package com.airgo.airgoApp.features.social.forums.data

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ForumChatApi {
    @GET("api/forums/{id}/messages/")
    suspend fun getMessages(
        @Path("id") forumId: Int,
        @Query("before") before: Int? = null,
        @Query("limit") limit: Int? = null,
    ): List<ForumMessage>
}
