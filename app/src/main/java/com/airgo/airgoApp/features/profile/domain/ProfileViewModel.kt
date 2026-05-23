package com.airgo.airgoApp.features.profile.domain

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airgo.airgoApp.R
import com.airgo.airgoApp.core.network.NetworkErrorMapper
import com.airgo.airgoApp.core.network.RetrofitProvider
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.core.utils.FileUtils
import com.airgo.airgoApp.core.utils.UiText
import com.airgo.airgoApp.features.profile.data.UserBadgeResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.IOException

sealed class ProfileUiState {
    object Idle : ProfileUiState() // Estado normal, esperando

    object Loading : ProfileUiState() // Mostrando un círculo de carga

    object DeleteSuccess : ProfileUiState() // ¡Cuenta eliminada con éxito!

    object PictureUpdateLoading : ProfileUiState()

    object PictureUpdateSuccess : ProfileUiState()

    data class Error(
        val message: UiText,
    ) : ProfileUiState() // Ocurrió un problema
}

class ProfileViewModel : ViewModel() {
    companion object {
        private const val TAG = "ProfileViewModel"
    }

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Idle)
    val uiState: StateFlow<ProfileUiState> = _uiState

    private val _badges = MutableStateFlow<List<UserBadgeResponse>>(emptyList())
    val badges: StateFlow<List<UserBadgeResponse>> = _badges

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            try {
                val profile = RetrofitProvider.profileApi.getUserProfile()
                SessionManager.pes = profile.pes
                SessionManager.altura = profile.altura
                SessionManager.username = profile.username
            } catch (e: IOException) {
                Log.e(TAG, "loadProfile network error", e)
                _uiState.value = ProfileUiState.Error(UiText.StringResource(R.string.error_network_generic))
            } catch (e: retrofit2.HttpException) {
                Log.e(TAG, "loadProfile HTTP ${e.code()}", e)
                _uiState.value = ProfileUiState.Error(UiText.StringResource(R.string.error_server_generic, e.code()))
            }
        }
        loadBadges()

        viewModelScope.launch {
            com.airgo.airgoApp.core.AppEvents.activitySaved.collect {
                loadBadges()
            }
        }
    }

    fun loadBadges() {
        _badges.value = SessionManager.userBadges ?: emptyList()
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading

            try {
                val response = RetrofitProvider.profileApi.deleteAccount()

                if (response.isSuccessful) {
                    SessionManager.clear()
                    _uiState.value = ProfileUiState.DeleteSuccess
                } else {
                    _uiState.value =
                        ProfileUiState.Error(UiText.StringResource(R.string.error_delete_account_http, response.code()))
                }
            } catch (e: IOException) {
                _uiState.value = ProfileUiState.Error(NetworkErrorMapper.fromIOException(e))
            }
        }
    }

    fun updateProfilePicture(
        context: Context,
        imageUriString: String,
    ) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.PictureUpdateLoading
            try {
                val uri = imageUriString.toUri()

                val imageFile = FileUtils.getFileFromUri(context, uri)

                if (imageFile == null) {
                    _uiState.value = ProfileUiState.Error(UiText.StringResource(R.string.error_process_image))
                    return@launch
                }

                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                val requestFile = imageFile.asRequestBody(mimeType.toMediaTypeOrNull())
                val imagePart =
                    MultipartBody.Part.createFormData(
                        "profile_pic",
                        imageFile.name,
                        requestFile,
                    )

                val response = RetrofitProvider.profileApi.updateProfilePicture(imagePart)

                if (response.isSuccessful) {
                    _uiState.value = ProfileUiState.PictureUpdateSuccess

                    val newImageUrl = response.body()?.newPictureUrl

                    if (newImageUrl != null) {
                        runCatching { newImageUrl.toUri() }.getOrNull()

                        SessionManager.profilePictureUrl = newImageUrl

                        _uiState.value = ProfileUiState.PictureUpdateSuccess
                    } else {
                        _uiState.value = ProfileUiState.Error(UiText.StringResource(R.string.error_upload_no_url))
                    }
                    imageFile.delete()
                } else {
                    val errorBody = response.errorBody()?.string()
                    _uiState.value =
                        ProfileUiState.Error(
                            UiText.StringResource(
                                R.string.error_server_generic,
                                response.code(),
                            ),
                        )
                }
            } catch (e: IOException) {
                Log.e(TAG, "updateProfilePicture: error de conexión", e)
                _uiState.value = ProfileUiState.Error(UiText.StringResource(R.string.error_connection_upload))
            }
        }
    }

    fun resetUiState() {
        _uiState.value = ProfileUiState.Idle
    }
}
