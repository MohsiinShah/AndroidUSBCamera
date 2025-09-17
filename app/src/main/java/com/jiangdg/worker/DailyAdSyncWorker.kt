package com.jiangdg.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jiangdg.api.AdRepository
import com.jiangdg.api.ApiService
import com.jiangdg.db.AppDatabase
import com.jiangdg.db.displayedAdIds
import com.jiangdg.db.toEntity
import com.jiangdg.models.DeviceIdBody
import com.jiangdg.models.GetAdsRequest
import com.jiangdg.models.ReportRequest
import com.jiangdg.utils.Constants
import com.jiangdg.utils.DatastoreManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.collections.map

@HiltWorker
class DailyAdSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val appDatabase: AppDatabase,
    private val adRepository: AdRepository,
    private val datastoreManager: DatastoreManager,
    private val apiService: ApiService
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val hasTodayAds = hasAdsForToday()

            if (!hasTodayAds) {
                val deviceID = datastoreManager.getData(Constants.USER_DEVICE_ID, 0).first()
                var userIdCloud = datastoreManager.getData(Constants.USER_ID_CLOUD, "").first()

                if(userIdCloud == "") {
                    try {
                        val response = apiService.getDeviceID(DeviceIdBody(deviceID))
                        if(response.isSuccessful){
                            val rawString = response.body()?.string()?.trim('"') ?: ""
                            if (rawString.isBlank()) {
                                println("Empty response (no user ID)")
                            } else {
                                userIdCloud = rawString
                                datastoreManager.saveData(Constants.USER_ID_CLOUD, userIdCloud)
                            }
                        }
                    }catch (e: Exception){
                        e.printStackTrace()
                    }
                }

                val ads = appDatabase.adDao().getAllAds()

                if(ads.isNotEmpty()) {
                    val displayedAdsIds = appDatabase.adDao().getAllAds().displayedAdIds()

                    if(displayedAdsIds.isNotEmpty()){
                        try {
                            val reportRequest =
                                ReportRequest(pAds = displayedAdsIds, pUser = userIdCloud)
                            apiService.sendDailyReport(body = reportRequest)
                        }catch (e: Exception){
                            e.printStackTrace()
                        }
                    }
                }
                val getAdsRequest = GetAdsRequest(pDeviceId = deviceID, pUser = userIdCloud, pTs = getCurrentTime())
                adRepository.pullDailyAds(getAdsRequest)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    suspend fun getCurrentTime(): String{
        val nowUtc = Instant.now()

        // Format as ISO-8601 with "Z"
        val formatter = DateTimeFormatter.ISO_INSTANT
        return formatter.format(nowUtc)
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
