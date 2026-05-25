package com.airgo.airgoApp.features.social.forums.data

import com.google.gson.annotations.SerializedName

data class ForumMessage(
    val id: Int,
    val emissor: Int,
    @SerializedName("emissor_username")
    val emissorUsername: String,
    @SerializedName("contingut")
    val contingut: String,
    @SerializedName("enviat_at")
    val enviatAt: String,
)

data class ForumWebSocketMessage(
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
