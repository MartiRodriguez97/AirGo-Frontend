package com.airgo.airgoApp.features.social

import com.google.gson.annotations.SerializedName

data class UserSearchResult(
    @SerializedName("id") val id: Int,
    @SerializedName("username") val username: String,
    @SerializedName("profile_pic") val profilePic: String?,
)
