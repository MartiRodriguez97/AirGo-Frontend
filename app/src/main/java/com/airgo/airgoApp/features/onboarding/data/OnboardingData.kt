package com.airgo.airgoApp.features.onboarding.data

import com.google.gson.annotations.SerializedName

data class UserProfileResponse(
    val id: Int,
    @SerializedName("profile_pic")
    val profilePic: String,
    @SerializedName("google_id")
    val googleId: String?,
    val username: String,
    val punts: Int,
    @SerializedName("teBici")
    val teBici: Boolean?,
    val pes: Double,
    val altura: Double,
    val ratxa: Int,
    val idioma: String?,
    @SerializedName("limitRutes")
    val limitRutes: Int,
    val titol: String?,
    val insignies: String?,
)

data class OnboardingRequest(
    val username: String,
    val pes: Double,
    val altura: Double,
    @SerializedName("teBici")
    val teBici: Boolean,
)
