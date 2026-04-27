package com.airgo.airgoApp.features.onboarding.domain

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airgo.airgoApp.core.network.RetrofitProvider
import com.airgo.airgoApp.features.onboarding.data.OnboardingRequest
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

private const val LOG_TAG = "OnboardingViewModel"

sealed class OnboardingUiState {
    data object Idle : OnboardingUiState()

    data object LoadingProfile : OnboardingUiState()

    data object NeedsOnboarding : OnboardingUiState()

    data object Saving : OnboardingUiState()

    data object Success : OnboardingUiState()

    data class Error(
        val message: String,
    ) : OnboardingUiState()
}

class OnboardingViewModel : ViewModel() {
    var uiState by mutableStateOf<OnboardingUiState>(OnboardingUiState.Idle)
        private set

    private var userId: Int = -1

    /**
     * Comprova si l'usuari ja ha completat l'onboarding.
     * Si pes == 0 o altura == 0, cal fer l'onboarding.
     */
    fun checkOnboardingNeeded() {
        viewModelScope.launch {
            uiState = OnboardingUiState.LoadingProfile
            try {
                val profile = RetrofitProvider.onboardingApi.getUserProfile()
                userId = profile.id
                uiState =
                    if (profile.pes == 0 || profile.altura == 0) {
                        OnboardingUiState.NeedsOnboarding
                    } else {
                        OnboardingUiState.Success
                    }
            } catch (e: IOException) {
                uiState = OnboardingUiState.Error("Error de connexió")
                Log.e(LOG_TAG, "Network failure: ${e.message}", e)
            } catch (e: HttpException) {
                uiState = OnboardingUiState.Error("Error del servidor: ${e.code()}")
                Log.e(LOG_TAG, "HTTP failure: ${e.message()}", e)
            }
        }
    }

    /**
     * Guarda les dades d'onboarding de l'usuari.
     */
    fun saveOnboarding(
        username: String,
        pes: Int,
        altura: Int,
        teBici: Boolean,
    ) {
        if (username.isBlank() || pes <= 0 || altura <= 0) {
            uiState = OnboardingUiState.Error("Omple tots els camps correctament")
            return
        }
        viewModelScope.launch {
            uiState = OnboardingUiState.Saving
            try {
                RetrofitProvider.onboardingApi.updateUserProfile(
                    id = userId,
                    request =
                        OnboardingRequest(
                            username = username,
                            pes = pes,
                            altura = altura,
                            teBici = teBici,
                        ),
                )
                uiState = OnboardingUiState.Success
                Log.d(LOG_TAG, "Onboarding completat per usuari $userId")
            } catch (e: IOException) {
                uiState = OnboardingUiState.Error("Error de connexió")
                Log.e(LOG_TAG, "Network failure: ${e.message}", e)
            } catch (e: HttpException) {
                uiState = OnboardingUiState.Error("Error del servidor: ${e.code()}")
                Log.e(LOG_TAG, "HTTP failure: ${e.message()}", e)
            }
        }
    }
}
