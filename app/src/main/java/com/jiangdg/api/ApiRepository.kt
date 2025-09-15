package com.jiangdg.api

import com.jiangdg.models.ApiResponse
import javax.inject.Inject

class ApiRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getData(): ApiResponse = apiService.getData()
}
