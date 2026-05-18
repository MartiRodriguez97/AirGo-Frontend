package com.airgo.airgoApp.features.social.chat.data

import com.google.gson.annotations.SerializedName

data class Conversation(
    val id: String, // Format per "idUser1-idUser2" on idUser1 < idUser2
    @SerializedName("other_user")
    val otherUser: String,
    @SerializedName("last_message")
    val lastMessage: String?,
    @SerializedName("unread_count")
    val unreadCount: String,
    @SerializedName("creada_at")
    val creadaAt: String,
)

data class Message(
    val id: Int,
    val emissor: Int,
    @SerializedName("emissor_username")
    val emissorUsername: String,
    @SerializedName("contingut")
    val contingut: String,
    @SerializedName("enviat_at")
    val enviatAt: String,
    val llegit: Boolean,
)

data class WebSocketMessage(
    val id: Int? = null,
    @SerializedName("emissor_id")
    val emissor: Int? = null,
    @SerializedName("emissor_username")
    val emissorUsername: String? = null,
    @SerializedName("contingut")
    val contingut: String? = null,
    @SerializedName("enviat_at")
    val enviatAt: String? = null,
)
