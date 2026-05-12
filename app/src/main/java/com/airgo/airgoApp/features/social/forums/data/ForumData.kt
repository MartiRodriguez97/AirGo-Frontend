package com.airgo.airgoApp.features.social.forums.data

import com.google.gson.annotations.SerializedName

data class Forum(
    val id: Int,
    val nom: String,
    @SerializedName("descripcio")
    val descripcio: String,
    @SerializedName("creat_per")
    val creatPer: Int,
    @SerializedName("es_favorit")
    val esFavorit: Boolean = false,
)

data class CreateForumRequest(
    val nom: String,
    @SerializedName("descripcio")
    val descripcio: String,
)