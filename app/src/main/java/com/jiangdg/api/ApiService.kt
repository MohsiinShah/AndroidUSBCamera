package com.jiangdg.api

import com.jiangdg.demo.BuildConfig
import com.jiangdg.models.ApiResponse
import retrofit2.http.GET
import retrofit2.http.Headers

interface ApiService {
    @GET("rest/v1/ads")  // Replace with your endpoint
    @Headers(
        "Authorization: Bearer ${BuildConfig.API_KEY}",
        "apikey: ${BuildConfig.API_KEY}",
        "Content-Type: application/json"
    )
    suspend fun getData(): ApiResponse
}
