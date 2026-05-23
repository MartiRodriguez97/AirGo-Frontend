package com.airgo.airgoApp.features.social.friends.domain
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airgo.airgoApp.R
import com.airgo.airgoApp.core.network.RetrofitProvider
import com.airgo.airgoApp.core.utils.UiText
import com.airgo.airgoApp.features.social.chat.domain.ChatUiState
import com.airgo.airgoApp.features.social.friends.data.FriendProfileData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

sealed class FriendProfileUiState {
    object Loading : FriendProfileUiState()

    data class Success(
        val profile: FriendProfileData,
    ) : FriendProfileUiState()

    data class Error(
        val message: UiText.StringResource,
    ) : FriendProfileUiState()
}

class FriendProfileViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<FriendProfileUiState>(FriendProfileUiState.Loading)
    val uiState: StateFlow<FriendProfileUiState> = _uiState

    fun loadProfile(userId: Int) {
        viewModelScope.launch {
            _uiState.value = FriendProfileUiState.Loading
            try {
                val profile = RetrofitProvider.socialApi.getPublicProfile(userId)
                _uiState.value = FriendProfileUiState.Success(profile)
            } catch (e: Exception) {
                _uiState.value = FriendProfileUiState.Error(UiText.StringResource(R.string.error_loading_profile))
            } catch (e: IOException) {
                _uiState.value = FriendProfileUiState.Error(UiText.StringResource(R.string.error_network_generic))
            } catch (e: HttpException) {
                _uiState.value = FriendProfileUiState.Error(UiText.StringResource(R.string.error_server_generic, e.code()))
            }
        }
    }
}
