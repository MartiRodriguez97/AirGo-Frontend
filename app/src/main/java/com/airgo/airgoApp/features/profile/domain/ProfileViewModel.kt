package com.airgo.airgoApp.features.profile.domain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airgo.airgoApp.core.network.NetworkErrorMapper
import com.airgo.airgoApp.features.login.data.auth.RetrofitProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException

private const val MOCK_DELAY_TIME = 2000L

sealed class ProfileUiState {
    object Idle : ProfileUiState() // Estado normal, esperando

    object Loading : ProfileUiState() // Mostrando un círculo de carga

    object DeleteSuccess : ProfileUiState() // ¡Cuenta eliminada con éxito!

    object PictureUpdateLoading : ProfileUiState()

    object PictureUpdateSuccess : ProfileUiState()

    data class Error(
        val message: String,
    ) : ProfileUiState() // Ocurrió un problema
}

class ProfileViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Idle)
    val uiState: StateFlow<ProfileUiState> = _uiState

    fun deleteAccount() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading

            try {
                // Fem la petició al backend
                val response = RetrofitProvider.authApi.deleteAccount()

                // Revisem la resposta del servidor
                if (response.isSuccessful) {
                    _uiState.value = ProfileUiState.DeleteSuccess
                    println("Cuenta eliminada con éxito en el backend")
                } else {
                    // Si el servidor rechaza la petición (ej. el token expiró o hay un error interno)
                    _uiState.value =
                        ProfileUiState.Error("Error al eliminar la cuenta (HTTP ${response.code()})")
                }
            } catch (e: IOException) {
                // Error de red (ej. sin internet)
                _uiState.value = ProfileUiState.Error(NetworkErrorMapper.fromIOException(e))
            }
        }
    }

    fun updateProfilePicture(imageUri: String) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.PictureUpdateLoading

            try {
                // MOCK UP
                // TODO : crida api per actualitzar la foto de perfil al backend
                delay(MOCK_DELAY_TIME)
                _uiState.value = ProfileUiState.PictureUpdateSuccess
                println("MOCK: Foto de perfil actualitzada amb èxit! URI: $imageUri")
                delay(MOCK_DELAY_TIME)
                _uiState.value = ProfileUiState.Idle
            } catch (e: IOException) {
                _uiState.value = ProfileUiState.Error("Error: ${e.message}")
            }
        }
    }

    fun resetUiState() {
        _uiState.value = ProfileUiState.Idle
    }
}
