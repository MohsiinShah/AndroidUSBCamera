package com.jiangdg.api

import com.jiangdg.demo.BuildConfig
import com.jiangdg.models.ApiResponse
import com.jiangdg.models.DeviceIdBody
import com.jiangdg.models.GetAdsRequest
import com.jiangdg.models.ReportRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST

interface ApiService {
    @POST("rest/v1/rpc/get_ads_for_user_device")  // Replace with your endpoint
    @Headers(
        "Authorization: Bearer ${BuildConfig.API_KEY}",
        "apikey: ${BuildConfig.API_KEY}",
        "Content-Type: application/json"
    )
    suspend fun getData(@Body body: GetAdsRequest): ApiResponse


    @POST("rest/v1/rpc/get_user_by_device")  // Replace with your endpoint
    @Headers(
        "Authorization: Bearer ${BuildConfig.API_KEY}",
        "apikey: ${BuildConfig.API_KEY}",
        "Content-Type: application/json"
    )
    suspend fun getDeviceID(@Body body: DeviceIdBody): Response<ResponseBody>

    @POST("rest/v1/rpc/apply_seen_and_upsert_daily_report")  // Replace with your endpoint
    @Headers(
        "Authorization: Bearer ${BuildConfig.API_KEY}",
        "apikey: ${BuildConfig.API_KEY}",
        "Content-Type: application/json"
    )
    suspend fun sendDailyReport(@Body body: ReportRequest): Response<ResponseBody>
}
