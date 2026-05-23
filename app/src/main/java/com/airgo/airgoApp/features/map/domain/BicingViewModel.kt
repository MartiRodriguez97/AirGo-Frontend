package com.airgo.airgoApp.features.map.domain

import android.content.ContentValues.TAG
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airgo.airgoApp.R
import com.airgo.airgoApp.core.network.RetrofitProvider
import com.airgo.airgoApp.core.utils.UiText
import com.airgo.airgoApp.features.map.data.BicingStation
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class BicingViewModel : ViewModel() {
    var stations by mutableStateOf<List<BicingStation>>(emptyList())
        private set

    var errorMessage by mutableStateOf<UiText?>(null)
        private set

    fun fetchStations(
        lat: Double,
        lon: Double,
        radio: Int = 5,
    ) {
        viewModelScope.launch {
            try {
                errorMessage = null
                stations = RetrofitProvider.bicingApi.getBicingStations(lat, lon, radio)
                if (stations.isEmpty()) {
                    errorMessage = UiText.StringResource(R.string.error_fetching_bicing)
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network error fetching routes: ${e.message}", e)
                errorMessage = UiText.StringResource(R.string.error_connection_generic)
            } catch (e: HttpException) {
                Log.e(TAG, "HTTP error fetching routes: ${e.code()}", e)
                errorMessage = UiText.StringResource(R.string.error_connection_generic, e.code())
            }
        }
    }

    fun clearError() {
        errorMessage = null
    }
}
