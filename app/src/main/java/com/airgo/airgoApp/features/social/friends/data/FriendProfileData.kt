package com.airgo.airgoApp.features.social.friends.data

import com.google.gson.annotations.SerializedName

data class FriendProfileData(
    @SerializedName("id") val id: Int,
    @SerializedName("username") val username: String,
    @SerializedName("profile_pic") val profilePic: String?,
    @SerializedName("punts") val punts: Int,
    @SerializedName("ratxa") val ratxa: Int,
    @SerializedName("titol") val titol: String?,
    @SerializedName("teBici") val teBici: Boolean?,
)
