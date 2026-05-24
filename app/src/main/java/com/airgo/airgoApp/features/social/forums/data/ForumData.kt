package com.airgo.airgoApp.features.social.forums.data

import com.google.gson.annotations.SerializedName

data class Forum(
    val id: Int,
    val nom: String?,
    @SerializedName("descripcio")
    val descripcio: String?,
    @SerializedName("creat_per")
    val creatPer: Int,
    @SerializedName("creat_at")
    val creatAt: String = "",
)

data class CreateForumRequest(
    val nom: String,
    @SerializedName("descripcio")
    val descripcio: String,
)

data class AddFavoritForumRequest(
    @SerializedName("forum_id")
    val forumId: Int,
)

data class FavoritForumResponse(
    val id: Int,
    val forum: Forum,
    @SerializedName("afegit_at")
    val afegitAt: String,
)
