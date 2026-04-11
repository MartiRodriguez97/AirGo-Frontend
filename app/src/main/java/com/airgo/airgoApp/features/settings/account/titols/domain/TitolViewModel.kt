package com.airgo.airgoApp.features.settings.account.titols.domain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airgo.airgoApp.core.network.ApiEndpoints
import com.airgo.airgoApp.core.network.RetrofitProvider
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.features.settings.account.titols.data.ChangeTitolRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException

sealed class TitolUiState {
    object Idle : TitolUiState()

    object Loading : TitolUiState()

    object Success : TitolUiState()

    data class Error(
        val message: String,
    ) : TitolUiState()
}

class TitolViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<TitolUiState>(TitolUiState.Idle)
    val uiState: StateFlow<TitolUiState> = _uiState

    fun changeTitol(newTitol: String) {
        if (newTitol.isBlank()) {
            _uiState.value = TitolUiState.Error("El título no puede estar vacío.")
            return
        }

        viewModelScope.launch {
            _uiState.value = TitolUiState.Loading

            try {
                val userId = SessionManager.userId
                if (userId == null) {
                    _uiState.value = TitolUiState.Error("Error: Usuario no identificado.")
                    return@launch
                }

                val url = ApiEndpoints.getChangeTitolEndpoint(userId)
                val response =
                    RetrofitProvider.titolApi.changeTitol(
                        url,
                        ChangeTitolRequest(newTitol),
                    )

                if (response.isSuccessful) {
                    SessionManager.titol = newTitol
                    _uiState.value = TitolUiState.Success
                } else {
                    val errorBodyString = response.errorBody()?.string()
                    _uiState.value = TitolUiState.Error("Error HTTP ${response.code()}: $errorBodyString")
                }
            } catch (e: IOException) {
                _uiState.value = TitolUiState.Error("Error de conexión: ${e.message}")
            } catch (e: retrofit2.HttpException) {
                _uiState.value = TitolUiState.Error("Error HTTP: ${e.message}")
            }
        }
    }

    fun resetUiState() {
        _uiState.value = TitolUiState.Idle
    }
}
