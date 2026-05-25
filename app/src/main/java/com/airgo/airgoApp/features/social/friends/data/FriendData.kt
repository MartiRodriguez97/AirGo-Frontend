package com.airgo.airgoApp.features.social.friends.data
import com.google.gson.annotations.SerializedName

data class FriendResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("estat") val estat: String,
    @SerializedName("creat_at") val creatAt: String,
    @SerializedName("amic") val amic: AmicInfo,
)

data class AmicInfo(
    @SerializedName("id") val id: Int,
    @SerializedName("username") val username: String,
    @SerializedName("profile_pic") val profilePic: String?,
)

data class FriendRequestBody(
    @SerializedName("receptor_id") val receptorId: Int,
)

data class FriendRequestResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("solicitant_id") val solicitantId: Int,
    @SerializedName("solicitant_username") val solicitantUsername: String,
    @SerializedName("solicitant_profile_pic") val solicitantProfilePic: String?,
)

data class RespondFriendRequestBody(
    @SerializedName("accio") val accio: String,
)
