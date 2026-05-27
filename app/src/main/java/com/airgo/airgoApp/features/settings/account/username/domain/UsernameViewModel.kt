package com.airgo.airgoApp.features.settings.account.username.domain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airgo.airgoApp.R
import com.airgo.airgoApp.core.network.ApiEndpoints
import com.airgo.airgoApp.core.network.RetrofitProvider
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.core.utils.UiText
import com.airgo.airgoApp.features.settings.account.username.data.ChangeUsernameRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException

private const val HTTP_400 = 400

sealed class UsernameUiState {
    object Idle : UsernameUiState()

    object Loading : UsernameUiState()

    object Success : UsernameUiState()

    data class Error(
        val message: UiText,
    ) : UsernameUiState()
}

// ViewModel para manejar la lógica de cambio de nombre de usuario
class UsernameViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<UsernameUiState>(UsernameUiState.Idle)
    val uiState: StateFlow<UsernameUiState> = _uiState

    // Función para cambiar el nombre de usuario, validando la entrada y manejando la respuesta del servidor
    fun changeUsername(newUsername: String) {
        if (newUsername.isBlank()) {
            _uiState.value = UsernameUiState.Error(UiText.StringResource(R.string.error_username_empty))
            return
        }

        viewModelScope.launch {
            _uiState.value = UsernameUiState.Loading

            try {
                val userId = SessionManager.userId
                if (userId == null) {
                    _uiState.value = UsernameUiState.Error(UiText.StringResource(R.string.error_user_not_identified))
                    return@launch
                }

                val url = ApiEndpoints.getChangeInfoEndpoint(userId)
                val response =
                    RetrofitProvider.usernameApi.changeUsername(
                        url,
                        ChangeUsernameRequest(newUsername),
                    )

                if (response.isSuccessful) {
                    SessionManager.username = newUsername
                    _uiState.value = UsernameUiState.Success
                } else {
                    _uiState.value =
                        UsernameUiState.Error(
                            UiText.StringResource(R.string.error_update_username),
                        )
                }
            } catch (e: IOException) {
                android.util.Log.e("UsernameViewModel", "Error de conexión", e)
                _uiState.value = UsernameUiState.Error(UiText.StringResource(R.string.error_connection_generic))
            } catch (e: retrofit2.HttpException) {
                if (e.code() == HTTP_400) {
                    _uiState.value = UsernameUiState.Error(UiText.StringResource(R.string.conditions_username_taken))
                } else {
                    android.util.Log.e("UsernameViewModel", "Error HTTP", e)
                    _uiState.value =
                        UsernameUiState.Error(
                            UiText.StringResource(
                                R.string.error_http_code_username,
                                e.code(),
                                e.message(),
                            ),
                        )
                }
            }
        }
    }

    fun resetUiState() {
        _uiState.value = UsernameUiState.Idle
    }
}
