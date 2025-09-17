package com.jiangdg.models


import com.google.gson.annotations.SerializedName

data class ReportRequest(
    @SerializedName("p_ads")
    val pAds: List<String?>? = null,
    @SerializedName("p_user")
    val pUser: String? = null
)