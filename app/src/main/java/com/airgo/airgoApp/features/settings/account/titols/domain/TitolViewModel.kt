package com.airgo.airgoApp.features.settings.account.titols.domain

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airgo.airgoApp.core.network.ApiEndpoints
import com.airgo.airgoApp.core.network.RetrofitProvider
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.features.settings.account.titols.data.ChangeTitolRequest
import com.airgo.airgoApp.features.settings.account.titols.data.UserTitol
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException

sealed class TitolUiState {
    object Idle : TitolUiState()

    object Loading : TitolUiState()

    object Success : TitolUiState()

    data class Error(
        val message: String,
    ) : TitolUiState()
}

// ViewModel para manejar la lógica de cambio de título del usuario
class TitolViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<TitolUiState>(TitolUiState.Idle)
    val uiState: StateFlow<TitolUiState> = _uiState

    private val _availableTitols = MutableStateFlow<List<UserTitol>>(emptyList())
    val availableTitols: StateFlow<List<UserTitol>> = _availableTitols

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError

    init {
        loadAvailableTitols()
    }

    // Carga los títulos disponibles para el usuario, utilizando caché si ya se han cargado previamente
    private fun loadAvailableTitols() {
        if (SessionManager.availableTitols != null) {
            _availableTitols.value = SessionManager.availableTitols!!
            return
        }

        viewModelScope.launch {
            try {
                val userId = SessionManager.userId ?: return@launch
                val url = ApiEndpoints.getAvailableTitolsEndpoint(userId)
                val response = RetrofitProvider.titolApi.getAvailableTitols(url)

                if (response.isSuccessful) {
                    val body = response.body()?.map { it.titol } ?: emptyList()
                    SessionManager.availableTitols = body
                    _availableTitols.value = body
                    _loadError.value = null
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("TitolViewModel", "Error fetching titols HTTP ${response.code()}: $errorBody")
                    _loadError.value = "Error al cargar títulos: ${response.code()}"
                }
            } catch (e: IOException) {
                Log.e("TitolViewModel", "Network error fetching titols", e)
                _loadError.value = "No s'ha pogut connectar en aquests moments."
            } catch (e: retrofit2.HttpException) {
                Log.e("TitolViewModel", "HTTP Exception fetching titols", e)
                _loadError.value = "Error HTTP: ${e.message}"
            }
        }
    }

    fun changeTitol(newTitol: String) {
        if (newTitol.isBlank()) {
            _uiState.value = TitolUiState.Error("El título no puede estar vacío.")
            return
        }

        viewModelScope.launch {
            _uiState.value = TitolUiState.Loading

            try {
                val userId = SessionManager.userId
                if (userId == null) {
                    _uiState.value = TitolUiState.Error("Error: Usuario no identificado.")
                    return@launch
                }

                val url = ApiEndpoints.getChangeInfoEndpoint(userId)
                val response =
                    RetrofitProvider.titolApi.changeTitol(
                        url,
                        ChangeTitolRequest(newTitol),
                    )

                if (response.isSuccessful) {
                    SessionManager.titol = newTitol
                    _uiState.value = TitolUiState.Success
                } else {
                    val errorBodyString = response.errorBody()?.string()
                    Log.e("TitolViewModel", "Error changing titol HTTP ${response.code()}: $errorBodyString")
                    _uiState.value = TitolUiState.Error("Error HTTP ${response.code()}: $errorBodyString")
                }
            } catch (e: IOException) {
                Log.e("TitolViewModel", "Network error changing titol", e)
                _uiState.value = TitolUiState.Error("Error de conexión: ${e.message}")
            } catch (e: retrofit2.HttpException) {
                Log.e("TitolViewModel", "HTTP Exception changing titol", e)
                _uiState.value = TitolUiState.Error("Error HTTP: ${e.message}")
            }
        }
    }

    fun resetUiState() {
        _uiState.value = TitolUiState.Idle
    }
}
