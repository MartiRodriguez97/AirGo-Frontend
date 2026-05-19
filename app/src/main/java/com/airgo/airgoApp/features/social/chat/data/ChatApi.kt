package com.airgo.airgoApp.features.social.chat.data

import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatApi {
    @GET("api/conversations/")
    suspend fun getConversations(): List<Conversation>

    @GET("api/conversations/{id}/messages/")
    suspend fun getMessages(
        @Path("id") conversationId: String,
        @Query("before") before: Int? = null,
        @Query("limit") limit: Int? = null,
    ): List<Message>

    @PATCH("api/conversations/{id}/read/")
    suspend fun markAsRead(
        @Path("id") conversationId: String,
    ): Response<Unit>

    @DELETE("api/conversations/messages/{missatge_id}/")
    suspend fun deleteMessage(
        @Path("missatge_id") messageId: Int,
    ): Response<Unit>
}
