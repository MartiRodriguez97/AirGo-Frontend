package com.airgo.airgoApp.features.settings.account.username.domain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airgo.airgoApp.core.network.ApiEndpoints
import com.airgo.airgoApp.core.network.RetrofitProvider
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.features.settings.account.username.data.ChangeUsernameRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException

sealed class UsernameUiState {
    object Idle : UsernameUiState()

    object Loading : UsernameUiState()

    object Success : UsernameUiState()

    data class Error(
        val message: String,
    ) : UsernameUiState()
}

class UsernameViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<UsernameUiState>(UsernameUiState.Idle)
    val uiState: StateFlow<UsernameUiState> = _uiState

    fun changeUsername(newUsername: String) {
        if (newUsername.isBlank()) {
            _uiState.value = UsernameUiState.Error("El nombre de usuario no puede estar vacío.")
            return
        }

        viewModelScope.launch {
            _uiState.value = UsernameUiState.Loading

            try {
                val userId = SessionManager.userId
                if (userId == null) {
                    _uiState.value = UsernameUiState.Error("Error: Usuario no identificado.")
                    return@launch
                }

                val url = ApiEndpoints.getChangeUsernameEndpoint(userId)
                val response =
                    RetrofitProvider.usernameApi.changeUsername(
                        url,
                        ChangeUsernameRequest(newUsername),
                    )

                if (response.isSuccessful) {
                    SessionManager.username = newUsername
                    _uiState.value = UsernameUiState.Success
                } else {
                    val errorBodyString = response.errorBody()?.string()
                    android.util.Log.e("UsernameViewModel", "HTTP ${response.code()}: $errorBodyString")
                    _uiState.value = UsernameUiState.Error("Error HTTP ${response.code()}: $errorBodyString")
                }
            } catch (e: IOException) {
                _uiState.value = UsernameUiState.Error("Error de conexión: ${e.message}")
            } catch (e: retrofit2.HttpException) {
                _uiState.value = UsernameUiState.Error("Error HTTP: ${e.message}")
            }
        }
    }

    fun resetUiState() {
        _uiState.value = UsernameUiState.Idle
    }
}
