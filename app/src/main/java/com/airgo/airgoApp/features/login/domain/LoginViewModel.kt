package com.airgo.airgoApp.features.login.domain

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airgo.airgoApp.R
import com.airgo.airgoApp.core.network.NetworkErrorMapper
import com.airgo.airgoApp.core.network.RetrofitProvider
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.core.utils.UiText
import com.airgo.airgoApp.features.login.data.auth.GoogleTokenRequest
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
        val message: UiText,
    ) : LoginUiState()
}

// ViewModel para manejar la lógica de inicio de sesión con Google
class LoginViewModel : ViewModel() {
    private var _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
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
                    _uiState.value = LoginUiState.Error(UiText.StringResource(R.string.error_google_no_token))
                    return@launch
                }

                val response =
                    RetrofitProvider.authApi.loginWithGoogle(
                        GoogleTokenRequest(token = idToken),
                    )

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val session = SessionManager
                    session.accessToken = body.access
                    session.email = body.user
                    session.refreshToken = body.refresh

                    if (body.user != null) {
                        fetchAndSaveUserDetails(session)
                    }

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
                    val uiText =
                        if (backendMessage.isNullOrBlank()) {
                            UiText.StringResource(R.string.error_backend_token_rejected, response.code())
                        } else {
                            UiText.StringResource(
                                R.string.error_backend_token_rejected_details,
                                response.code(),
                                backendMessage,
                            )
                        }
                    _uiState.value = LoginUiState.Error(uiText)
                }
            } catch (e: IOException) {
                _uiState.value = LoginUiState.Error(NetworkErrorMapper.fromIOException(e))
            } catch (_: SecurityException) {
                _uiState.value = LoginUiState.Error(UiText.StringResource(R.string.error_security_permissions))
            } catch (_: IllegalStateException) {
                _uiState.value = LoginUiState.Error(UiText.StringResource(R.string.error_invalid_server_response))
            }
        }
    }

    private suspend fun fetchAndSaveUserDetails(session: SessionManager) {
        try {
            val userResponse = RetrofitProvider.authApi.getCurrentUserProfile()
            if (userResponse.isSuccessful && userResponse.body() != null) {
                val userDetails = userResponse.body()!!
                Log.d("LoginViewModel", "Usuario extraido: $userDetails")

                session.userId = userDetails.id
                session.username = userDetails.username
                session.profilePictureUrl = userDetails.profilePic
                session.punts = userDetails.punts
                session.teBici = userDetails.teBici
                session.pes = userDetails.pes
                session.altura = userDetails.altura
                session.ratxa = userDetails.ratxa
                session.idioma = userDetails.idioma
                session.limitRutes = userDetails.limitRutes
                session.titol = userDetails.titol
                session.insignies = userDetails.insignies
            } else {
                val errBody = userResponse.errorBody()?.string()
                Log.e(
                    "LoginViewModel",
                    "Error (HTTP ${userResponse.code()}): $errBody",
                )
            }
        } catch (e: IOException) {
            Log.e("LoginViewModel", "IO Error fetching user details", e)
        } catch (e: retrofit2.HttpException) {
            Log.e("LoginViewModel", "HTTP Error fetching user details", e)
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

    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }
}
