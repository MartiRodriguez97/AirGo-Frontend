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
            } catch (e: retrofit2.HttpException) {
                Log.e(TAG, "loadProfile HTTP ${e.code()}", e)
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
                // Fem la petició al backend
                val response = RetrofitProvider.profileApi.deleteAccount()

                println("Esperando resposta del servidor per eliminar compte...")

                // Revisem la resposta del servidor
                if (response.isSuccessful) {
                    SessionManager.clear()
                    _uiState.value = ProfileUiState.DeleteSuccess
                    println("Compte eliminat amb èxit al backend")
                } else {
                    // Si el servidor rechaza la petición (ej. el token expiró o hay un error interno)
                    _uiState.value =
                        ProfileUiState.Error(UiText.StringResource(R.string.error_delete_account_http, response.code()))
                }
            } catch (e: IOException) {
                // Error de red (ej. sin internet)
                _uiState.value = ProfileUiState.Error(NetworkErrorMapper.fromIOException(e))
                println("Error: " + e.printStackTrace())
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

                // Comprobamos la respuesta
                if (response.isSuccessful) {
                    _uiState.value = ProfileUiState.PictureUpdateSuccess

                    val newImageUrl = response.body()?.newPictureUrl
                    Log.d(TAG, "updateProfilePicture newImageUrl(raw)=$newImageUrl")

                    if (newImageUrl != null) {
                        val parsedUri = runCatching { newImageUrl.toUri() }.getOrNull()
                        Log.d(
                            TAG,
                            "updateProfilePicture parsed host=${parsedUri?.host} port=${parsedUri?.port}",
                        )

                        // 2. ACTUALIZAMOS EL SESSION MANAGER
                        // (Usa la función que tengas en tu SessionManager para guardar la foto o actualizar el usuario)
                        SessionManager.profilePictureUrl = newImageUrl

                        // 3. Actualizamos la interfaz
                        _uiState.value = ProfileUiState.PictureUpdateSuccess
                    } else {
                        Log.w(TAG, "updateProfilePicture: respuesta OK pero newImageUrl es null")
                        // Si la petición dio 200 OK pero el backend se olvidó de enviarnos la URL
                        _uiState.value = ProfileUiState.Error(UiText.StringResource(R.string.error_upload_no_url))
                    }

                    println("¡Foto subida con éxito al servidor!")

                    // Opcional: Borramos el archivo temporal para no ocupar espacio en el móvil
                    imageFile.delete()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.w(TAG, "updateProfilePicture HTTP error code=${response.code()}, errorBody=$errorBody")
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
                e.printStackTrace()
                _uiState.value = ProfileUiState.Error(UiText.StringResource(R.string.error_connection_upload))
            }
        }
    }

    fun resetUiState() {
        _uiState.value = ProfileUiState.Idle
    }
}
