package com.jiangdg.models


import com.google.gson.annotations.SerializedName

data class GetAdsRequest(
    @SerializedName("p_device_id")
    val pDeviceId: Int? = null,
    @SerializedName("p_ts")
    val pTs: String? = null,
    @SerializedName("p_user")
    val pUser: String? = null
)