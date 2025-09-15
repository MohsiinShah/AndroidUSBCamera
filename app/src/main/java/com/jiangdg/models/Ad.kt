package com.jiangdg.models


import com.google.gson.annotations.SerializedName
import java.time.ZonedDateTime

data class Ad(
    @SerializedName("campaign_ref")
    val campaignRef: Int? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("created_by")
    val createdBy: String? = null,
    @SerializedName("fullscreen_seconds")
    val fullscreenSeconds: Int? = null,
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("lbanner_seconds")
    val lbannerSeconds: Int? = null,
    @SerializedName("qr_link")
    val qrLink: String? = null,
    @SerializedName("seen_by")
    val seenBy: List<String?>? = null,
    @SerializedName("slot_time")
    val slotTime: String? = null,
    @SerializedName("url_bottom")
    val urlBottom: String? = null,
    @SerializedName("url_fullscreen")
    val urlFullscreen: String? = null,
    @SerializedName("url_left")
    val urlLeft: String? = null,
    @SerializedName("user_id")
    val userId: String? = null
)

fun Ad.slotDateTime(): ZonedDateTime = ZonedDateTime.parse(slotTime)
