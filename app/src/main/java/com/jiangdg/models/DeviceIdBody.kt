package com.jiangdg.models

import com.google.gson.annotations.SerializedName

data class DeviceIdBody(
    @SerializedName("p_device_id")
    val pDeviceId: Int? = null
)