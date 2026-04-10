package com.airgo.airgoApp.features.settings.account.tebici.domain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airgo.airgoApp.core.network.ApiEndpoints
import com.airgo.airgoApp.core.network.RetrofitProvider
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.features.settings.account.tebici.data.TeBiciApi
import com.airgo.airgoApp.features.settings.account.tebici.data.UpdateBikeRequest
import kotlinx.coroutines.launch

class TeBiciViewModel : ViewModel() {

    fun updateBikeAvailability(hasBike: Boolean) {
        viewModelScope.launch {
            val userId = SessionManager.userId
            if(userId != null) {
                val url = ApiEndpoints.getChangeUserInfoEndpoint(userId)

                try {
                    val request = UpdateBikeRequest(teBici = hasBike)
                    val response = RetrofitProvider.teBiciApi.updateHasBike(url, request)

                    if (response.isSuccessful) {
                        SessionManager.teBici = hasBike
                    } else {
                        // Manejar el error del servidor (ej. mostrar un Toast/Snackbar)
                    }
                } catch (e: Exception) {
                    // Manejar error de red o de conexión
                }
            }
        }
    }
}