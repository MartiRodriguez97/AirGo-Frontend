package com.airgo.airgoApp.features.login.domain

import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.network.HttpException
import com.airgo.airgoApp.R
import com.airgo.airgoApp.core.network.RetrofitProvider
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.core.network.UserPreferencesRepository
import com.airgo.airgoApp.core.network.dataStore
import com.airgo.airgoApp.core.utils.UiText
import com.airgo.airgoApp.features.login.data.auth.GoogleTokenRequest
import com.airgo.airgoApp.features.login.data.auth.signInWithGoogle
import com.airgo.airgoApp.features.login.domain.LoginUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

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

class LoginViewModel : ViewModel() {
    private var _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState

    fun onGoogleSignIn(context: Context) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                val credential = signInWithGoogle(context)
                val idToken = credential?.idToken
                if (idToken.isNullOrBlank()) {
                    _uiState.value = LoginUiState.Error(UiText.StringResource(R.string.error_google))
                    return@launch
                }

                val response =
                    RetrofitProvider.authApi.loginWithGoogle(
                        GoogleTokenRequest(token = idToken),
                    )

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    handleSuccessfulLogin(context, body.access, body.refresh, body.user, body.picture)
                } else {
                    val rawError = response.errorBody()?.string()
                    val backendMessage = extractBackendMessage(rawError)
                    val uiText =
                        if (backendMessage.isNullOrBlank()) {
                            UiText.StringResource(R.string.error_google)
                        } else {
                            UiText.StringResource(R.string.error_google)
                        }
                    _uiState.value = LoginUiState.Error(uiText)
                }
            } catch (e: IOException) {
                Log.e("LoginViewModel", "IO Error", e)
                _uiState.value = LoginUiState.Error(UiText.StringResource(R.string.error_google))
            } catch (_: SecurityException) {
                _uiState.value = LoginUiState.Error(UiText.StringResource(R.string.error_google))
            } catch (_: IllegalStateException) {
                _uiState.value = LoginUiState.Error(UiText.StringResource(R.string.error_google))
            }
        }
    }

    private suspend fun handleSuccessfulLogin(
        context: Context,
        access: String?,
        refresh: String?,
        user: String?,
        picture: String?,
    ) {
        val session = SessionManager
        session.accessToken = access
        session.email = user
        session.refreshToken = refresh

        if (user != null) {
            fetchAndSaveUserDetails(session)
        }

        if (session.username.isNullOrBlank()) {
            session.username = user?.substringBefore('@') ?: "Usuario"
        }

        if (session.profilePictureUrl.isNullOrEmpty() && !picture.isNullOrEmpty()) {
            session.profilePictureUrl = picture
            viewModelScope.launch {
                uploadGooglePictureToBackend(context, picture)
            }
        }

        val prefsRepo = UserPreferencesRepository(context.dataStore)
        prefsRepo.saveSession(session.toUserPreferences())

        _uiState.value =
            LoginUiState.Success(
                email = user,
                picture = picture,
            )
    }

    private suspend fun fetchAndSaveUserDetails(session: SessionManager) {
        try {
            val userResponse = RetrofitProvider.authApi.getCurrentUserProfile()
            if (userResponse.isSuccessful && userResponse.body() != null) {
                val userDetails = userResponse.body()!!

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

                try {
                    session.userBadges = RetrofitProvider.profileApi.getUserBadges()
                } catch (e: retrofit2.HttpException) {
                    _uiState.value = LoginUiState.Error(UiText.StringResource(R.string.error_fetching_badges))
                    Log.e("LoginViewModel", "Error fetching user badges (HTTP)", e)
                } catch (e: IOException) {
                    _uiState.value = LoginUiState.Error(UiText.StringResource(R.string.error_fetching_badges))
                    Log.e("LoginViewModel", "Error fetching user badges (IO)", e)
                }

                try {
                    val titlesResponse = RetrofitProvider.trackingApi.getAvailableTitles()
                    session.titolsPendents = titlesResponse.titolsPendents
                } catch (e: IOException) {
                    _uiState.value = LoginUiState.Error(UiText.StringResource(R.string.error_fetching_titles))
                    Log.e("LoginViewModel", "IO Error fetching pending titles", e)
                } catch (e: retrofit2.HttpException) {
                    Log.e("LoginViewModel", "HTTP Error fetching pending titles", e)
                }

                try {
                    fetchAvailableTitlesList(session)
                } catch (e: IOException) {
                    _uiState.value = LoginUiState.Error(UiText.StringResource(R.string.error_fetching_titles))
                    Log.e("LoginViewModel", "IO Error fetching available titles list", e)
                } catch (e: HttpException) {
                    _uiState.value = LoginUiState.Error(UiText.StringResource(R.string.error_fetching_titles))
                    Log.e("LoginViewModel", "Http Error fetching available titles list", e)
                }

                try {
                    val repo =
                        com.airgo.airgoApp.features.home.data
                            .TrainingPlanRepository()
                    session.trainingPlans = repo.fetchFullTrainingPlans()
                } catch (e: IOException) {
                    _uiState.value = LoginUiState.Error(UiText.StringResource(R.string.error_fetching_training_plan))
                    Log.e("LoginViewModel", "IO Error fetching training plans", e)
                } catch (e: retrofit2.HttpException) {
                    _uiState.value = LoginUiState.Error(UiText.StringResource(R.string.error_fetching_training_plan))
                    Log.e("LoginViewModel", "HTTP Error fetching training plans", e)
                } catch (ignored: java.lang.Exception) {
                    Log.e("LoginViewModel", "Error inesperado en planes de entrenamiento")
                }
            }
        } catch (e: IOException) {
            _uiState.value = LoginUiState.Error(UiText.StringResource(R.string.error_fetching_user_profile))
            Log.e("LoginViewModel", "IO Error fetching user details", e)
        } catch (e: retrofit2.HttpException) {
            _uiState.value = LoginUiState.Error(UiText.StringResource(R.string.error_fetching_user_profile))
            Log.e("LoginViewModel", "HTTP Error fetching user details", e)
        }
    }

    private suspend fun fetchAvailableTitlesList(session: SessionManager) {
        val userId = session.userId ?: return
        val url =
            com.airgo.airgoApp.core.network.ApiEndpoints
                .getAvailableTitolsEndpoint(userId)
        val response = RetrofitProvider.titolApi.getAvailableTitols(url)
        if (response.isSuccessful && response.body() != null) {
            session.availableTitols = response.body()!!.map { it.titol }
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
            Log.e(TAG, "JSON error: ${e.message}", e)
            errorText
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }

    private suspend fun uploadGooglePictureToBackend(
        context: Context,
        googlePictureUrl: String,
    ) {
        withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url(googlePictureUrl).build()
                val response = client.newCall(request).execute()

                if (response.isSuccessful && response.body != null) {
                    val tempFile = File(context.cacheDir, "google_temp_profile.jpg")
                    val fos = FileOutputStream(tempFile)
                    fos.write(response.body!!.bytes())
                    fos.close()

                    val requestFile = tempFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    val imagePart =
                        MultipartBody.Part.createFormData(
                            "profile_pic",
                            tempFile.name,
                            requestFile,
                        )

                    val uploadResponse = RetrofitProvider.profileApi.updateProfilePicture(imagePart)
                    if (uploadResponse.isSuccessful) {
                        val newPicUrl = uploadResponse.body()?.newPictureUrl
                        if (!newPicUrl.isNullOrEmpty()) {
                            SessionManager.profilePictureUrl = newPicUrl
                            val prefsRepo = UserPreferencesRepository(context.dataStore)
                            prefsRepo.saveSession(SessionManager.toUserPreferences())
                        }
                    }

                    tempFile.delete()
                }
            } catch (e: IOException) {
                Log.e("LoginViewModel", "IO Error", e)
                _uiState.value = LoginUiState.Error(UiText.StringResource(R.string.error_updating_picture))
            } catch (ignored: IllegalArgumentException) {
                _uiState.value = LoginUiState.Error(UiText.StringResource(R.string.error_updating_picture))
            }
        }
    }
}
