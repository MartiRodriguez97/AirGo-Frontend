package com.airgo.airgoApp.features.social.chat.data

import com.airgo.airgoApp.core.network.ApiEndpoints
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatApi {
    @GET(ApiEndpoints.CONVERSATIONS)
    suspend fun getConversations(): List<Conversation>

    @GET(ApiEndpoints.CONVERSATION_MESSAGES)
    suspend fun getMessages(
        @Path("id") conversationId: String,
        @Query("before") before: Int? = null,
        @Query("limit") limit: Int? = null,
    ): List<Message>

    @PATCH(ApiEndpoints.CONVERSATION_READ)
    suspend fun markAsRead(
        @Path("id") conversationId: String,
    ): Response<Unit>

    @DELETE(ApiEndpoints.DELETE_MESSAGE)
    suspend fun deleteMessage(
        @Path("missatge_id") messageId: Int,
    ): Response<Unit>
}
