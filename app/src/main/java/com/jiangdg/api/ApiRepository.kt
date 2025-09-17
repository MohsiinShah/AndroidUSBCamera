package com.jiangdg.api

import com.jiangdg.models.ApiResponse
import com.jiangdg.models.DeviceIdBody
import com.jiangdg.models.GetAdsRequest
import javax.inject.Inject

class ApiRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getData(request: GetAdsRequest): ApiResponse = apiService.getData(request)
}
