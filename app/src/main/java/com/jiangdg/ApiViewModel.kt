package com.jiangdg

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jiangdg.api.ApiRepository
import com.jiangdg.models.ApiResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ApiViewModel @Inject constructor(
    private val repository: ApiRepository
) : ViewModel() {

    private val _data = MutableLiveData<ApiResponse>()
    val data: LiveData<ApiResponse> = _data

    fun fetchData() {
        viewModelScope.launch {
            try {
//                _data.value = repository.getData()
            } catch (e: Exception) {
                // handle error
            }
        }
    }
}
