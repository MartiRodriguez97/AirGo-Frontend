package com.airgo.airgoApp.features.profile.domain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airgo.airgoApp.core.network.NetworkErrorMapper
import com.airgo.airgoApp.features.login.data.auth.RetrofitProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException

sealed class ProfileUiState {
    object Idle : ProfileUiState() // Estado normal, esperando

    object Loading : ProfileUiState() // Mostrando un círculo de carga

    object DeleteSuccess : ProfileUiState() // ¡Cuenta eliminada con éxito!

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
}
