package com.airgo.airgoApp.features.login.domain

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airgo.airgoApp.core.network.NetworkErrorMapper
import com.airgo.airgoApp.features.login.data.auth.GoogleTokenRequest
import com.airgo.airgoApp.features.login.data.auth.RetrofitProvider
import com.airgo.airgoApp.features.login.data.auth.signInWithGoogle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

// Define los estados de la UI para el proceso de inicio de sesión
sealed class LoginUiState {
    object Idle : LoginUiState()

    object Loading : LoginUiState()

    data class Success(
        val email: String?,
        val picture: String?,
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

                // Log token para depuración
                println("Google ID Token: $idToken")

                if (idToken.isNullOrBlank()) {
                    _uiState.value = LoginUiState.Error("No se obtuvo idToken de Google")
                    return@launch
                }

                val response =
                    RetrofitProvider.authApi.loginWithGoogle(
                        GoogleTokenRequest(token = idToken),
                    )

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    _uiState.value =
                        LoginUiState.Success(
                            email = body.user,
                            picture = body.picture,
                        )
                    // Log para depuración
                    println("Login exitoso: ${body.user}, ${body.picture}")
                } else {
                    val rawError = response.errorBody()?.string()
                    val backendMessage = extractBackendMessage(rawError)
                    val message =
                        if (backendMessage.isNullOrBlank()) {
                            "Backend rechazó el token (HTTP ${response.code()})"
                        } else {
                            "Backend rechazó el token (HTTP ${response.code()}): $backendMessage"
                        }
                    _uiState.value = LoginUiState.Error(message)
                }
            } catch (e: IOException) {
                _uiState.value = LoginUiState.Error(NetworkErrorMapper.fromIOException(e))
            } catch (_: SecurityException) {
                _uiState.value = LoginUiState.Error("Permisos de red no disponibles")
            } catch (_: IllegalStateException) {
                _uiState.value = LoginUiState.Error("Respuesta invalida del servidor")
            }
        }
    }

    private fun extractBackendMessage(rawError: String?): String? {
        val errorText = rawError?.trim()
        if (errorText.isNullOrBlank()) return null
        return try {
            val obj = JSONObject(errorText)
            obj
                .optString("detail")
                .ifBlank { obj.optString("message") }
                .ifBlank { obj.optString("error") }
                .ifBlank { errorText }
        } catch (e: JSONException) {
            Log.w("LoginViewModel", "No se pudo parsear el error del backend: $errorText", e)
            errorText
        }
    }
}
