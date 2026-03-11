package com.airgo.airgoApp.ui.login.domain

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airgo.airgoApp.ui.login.data.auth.RetrofitProvider
import com.airgo.airgoApp.ui.login.data.auth.signInWithGoogle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException

// Define los estados de la UI para el proceso de inicio de sesión
sealed class LoginUiState {
    object Idle : LoginUiState()

    object Loading : LoginUiState()

    data class Success(
        val email: String?,
        val displayName: String?,
    ) : LoginUiState()

    data class Error(
        val message: String,
    ) : LoginUiState()
}

// ViewModel para manejar la lógica de inicio de sesión con Google
class LoginViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState

    // Función para iniciar sesión con Google y actualizar el estado de la UI
    fun onGoogleSignIn(context: Context) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                val credential = signInWithGoogle(context)
                val idToken = credential?.idToken

                if (idToken.isNullOrBlank()) {
                    _uiState.value = LoginUiState.Error("No se obtuvo idToken de Google")
                    return@launch
                }

                val response =
                    RetrofitProvider.authApi.loginWithGoogle(
                        idToken,
                    )

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    _uiState.value =
                        LoginUiState.Success(
                            email = body.email,
                            displayName = body.displayName,
                        )
                } else {
                    _uiState.value = LoginUiState.Error("Backend rechazó el token")
                }
            } catch (e: IOException) {
                _uiState.value = LoginUiState.Error(e.message ?: "Error de red")
            }
        }
    }
}
