package com.jiangdg.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.jiangdg.models.Ad
import java.time.ZonedDateTime

@Entity(tableName = "ads")
data class AdEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0, // local PK
    val id: String?,               // API ad id
    val campaignRef: Int?,
    val createdAt: String?,
    val createdBy: String?,
    val fullscreenSeconds: Int?,
    val lbannerSeconds: Int?,
    val slotTime: String,
    val urlLeft: String?,
    val urlBottom: String?,
    val urlFullscreen: String?,
    val qrLink: String?,
    val userId: String?,
    var isDisplayed: Boolean = false
)

// Mapping functions
fun Ad.toEntity(): AdEntity {
    return AdEntity(
        id = id,
        campaignRef = campaignRef,
        createdAt = createdAt,
        createdBy = createdBy,
        fullscreenSeconds = fullscreenSeconds,
        lbannerSeconds = lbannerSeconds,
        slotTime = slotTime ?: "",
        urlLeft = urlLeft,
        urlBottom = urlBottom,
        urlFullscreen = urlFullscreen,
        qrLink = qrLink,
        userId = userId,
        isDisplayed = false
    )
}

fun AdEntity.toModel(): Ad = Ad(
    id = id,
    campaignRef = campaignRef,
    createdAt = createdAt,
    createdBy = createdBy,
    fullscreenSeconds = fullscreenSeconds,
    lbannerSeconds = lbannerSeconds,
    slotTime = slotTime,
    urlLeft = urlLeft,
    urlBottom = urlBottom,
    urlFullscreen = urlFullscreen,
    qrLink = qrLink,
    userId = userId
)

fun AdEntity.slotDateTime(): ZonedDateTime = ZonedDateTime.parse(slotTime)
