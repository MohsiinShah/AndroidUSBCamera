package com.jiangdg.api

import com.jiangdg.db.AdDao
import com.jiangdg.db.AdEntity
import com.jiangdg.db.toEntity
import com.jiangdg.models.Ad
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class AdRepository @Inject constructor(
    private val api: ApiRepository,
    private val dao: AdDao
) {

    suspend fun pullDailyAds() {
        val ads = api.getData() // List<Ad>
        val now = ZonedDateTime.now()

        // Round "now" up to the next 5-minute slot
        val minutesToAdd = 5 - (now.minute % 5)
        val baseSlot = now
            .plusMinutes(minutesToAdd.toLong())
            .withSecond(0)
            .withNano(0)

        val updatedAds = ads.mapIndexed { index, ad ->
            val newSlot = baseSlot.plusMinutes((index * 5).toLong())

            ad.copy(
                slotTime = newSlot.toString(),

                // Remove any trailing digits after .png
                urlLeft = ad.urlLeft?.replace(Regex("(\\.png)\\d*$"), "$1"),
                urlBottom = ad.urlBottom?.replace(Regex("(\\.png)\\d*$"), "$1"),
                urlFullscreen = ad.urlFullscreen?.replace(Regex("(\\.png)\\d*$"), "$1")
            )
        }

        // Clear old ads and insert updated ones
        dao.clearAll()
        dao.insertAll(updatedAds.map { it.toEntity() })
    }


    suspend fun markAdDisplayed(ad: AdEntity) {
        dao.update(ad.copy(isDisplayed = true))
    }

    suspend fun getAdForSlot(slotTime: String): AdEntity? {
        return dao.getAdForSlot(slotTime)
    }
}



fun Ad.withTodaySlot(): Ad {
    val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    val original = OffsetDateTime.parse(this.slotTime, formatter)

    // Replace the date part with today, but keep hour/min/sec
    val todaySlot = LocalDate.now().atTime(
        original.hour,
        original.minute,
        original.second
    ).atOffset(ZoneOffset.UTC)

    return this.copy(slotTime = todaySlot.format(formatter))
}
