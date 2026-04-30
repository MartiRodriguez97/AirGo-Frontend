package com.airgo.airgoApp.features.calendar.domain.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.airgo.airgoApp.R
import com.airgo.airgoApp.core.network.RetrofitProvider
import com.airgo.airgoApp.features.calendar.data.remote.CalendarApiService
import com.airgo.airgoApp.features.calendar.domain.model.PlaEntrenament
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException

class CalendarViewModel(
    private val apiService: CalendarApiService,
) : ViewModel() {
    private val _plaEntrenamentList = MutableStateFlow<List<PlaEntrenament>>(emptyList())
    val plaEntrenamentList: StateFlow<List<PlaEntrenament>> = _plaEntrenamentList

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorResId = MutableStateFlow<Int?>(null)
    val errorResId: StateFlow<Int?> = _errorResId

    fun fetchPlaEntrenament() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorResId.value = null
            try {
                val response = apiService.getPlaEntrenament()
                if (response.isSuccessful) {
                    _plaEntrenamentList.value = response.body() ?: emptyList()
                } else {
                    _errorResId.value = R.string.calendar_error_server
                }
            } catch (e: IOException) {
                _errorResId.value = R.string.calendar_error_network
                e.printStackTrace()
            } catch (e: retrofit2.HttpException) {
                _errorResId.value = R.string.calendar_error_server
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorResId.value = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
                        return CalendarViewModel(RetrofitProvider.calendarApi) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
    }
}
