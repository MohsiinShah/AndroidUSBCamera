package com.jiangdg.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jiangdg.api.AdRepository
import com.jiangdg.db.AppDatabase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@HiltWorker
class DailyAdSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val appDatabase: AppDatabase,
    private val adRepository: AdRepository
) : CoroutineWorker(context, workerParams) {

//    override suspend fun doWork(): Result {
//        return try {
//            adRepository.pullDailyAds()
//            Result.success()
//        } catch (e: Exception) {
//            e.printStackTrace()
//            Result.retry()
//        }
//    }

    override suspend fun doWork(): Result {
        return try {
            val hasTodayAds = hasAdsForToday()

            if (!hasTodayAds) {
                adRepository.pullDailyAds()
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }


    suspend fun hasAdsForToday(): Boolean {
        val today = LocalDate.now()
        val ads = appDatabase.adDao().getAllAds()
        return ads.any { ad ->
            try {
                val zdt = ZonedDateTime.parse(ad.slotTime) // parses "2025-09-16T10:40+05:00[Asia/Karachi]"
                zdt.toLocalDate() == today
            } catch (e: Exception) {
                false
            }
        }
    }


}
