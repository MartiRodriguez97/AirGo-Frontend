package com.airgo.airgoApp.features.settings.account.tebici.domain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airgo.airgoApp.R
import com.airgo.airgoApp.core.network.ApiEndpoints
import com.airgo.airgoApp.core.network.RetrofitProvider
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.core.utils.UiText
import com.airgo.airgoApp.features.settings.account.tebici.data.UpdateBikeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException

sealed class TeBiciUiState {
    object Idle : TeBiciUiState()

    object Loading : TeBiciUiState()

    object Success : TeBiciUiState()

    data class Error(
        val message: UiText,
    ) : TeBiciUiState()
}

class TeBiciViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<TeBiciUiState>(TeBiciUiState.Idle)
    val uiState: StateFlow<TeBiciUiState> = _uiState

    // Función para actualizar la disponibilidad de bicicleta del usuario
    fun updateBikeAvailability(hasBike: Boolean) {
        viewModelScope.launch {
            _uiState.value = TeBiciUiState.Loading
            val userId = SessionManager.userId
            if (userId != null) {
                val url = ApiEndpoints.getChangeInfoEndpoint(userId)

                try {
                    val request = UpdateBikeRequest(teBici = hasBike)
                    val response = RetrofitProvider.teBiciApi.updateHasBike(url, request)

                    if (response.isSuccessful) {
                        SessionManager.teBici = hasBike
                        _uiState.value = TeBiciUiState.Success
                    } else {
                        val errBody = response.errorBody()?.string()
                        android.util.Log.e("TeBiciViewModel", "HTTP ${response.code()}: $errBody")
                        _uiState.value =
                            TeBiciUiState.Error(
                                UiText.StringResource(
                                    R.string.tebici_error_update_http,
                                    response.code(),
                                ),
                            )
                    }
                } catch (e: IOException) {
                    _uiState.value =
                        TeBiciUiState.Error(
                            UiText.StringResource(R.string.error_connection, e.message ?: ""),
                        )
                } catch (e: retrofit2.HttpException) {
                    UiText.StringResource(R.string.error_http_generic, e.message ?: "Unknown")
                }
            } else {
                UiText.StringResource(R.string.error_user_not_identified)
            }
        }
    }

    fun resetUiState() {
        _uiState.value = TeBiciUiState.Idle
    }
}
