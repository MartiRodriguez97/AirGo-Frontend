package com.airgo.airgoApp.features.activity.domain
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airgo.airgoApp.R
import com.airgo.airgoApp.core.network.RetrofitProvider
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.features.activity.data.ActivityResponse
import com.airgo.airgoApp.features.activity.data.UpdateCommentRequest
import com.airgo.airgoApp.features.activity.data.UpdateSensationRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException

sealed class ActivityUiState {
    object Loading : ActivityUiState()

    object Empty : ActivityUiState()

    data class Success(
        val activities: List<ActivityResponse>,
    ) : ActivityUiState()

    data class Error(
        val message: Int,
    ) : ActivityUiState()
}

class ActivityViewModel : ViewModel() {
    companion object {
        private const val TAG = "ActivityViewModel"
    }

    private val _uiState = MutableStateFlow<ActivityUiState>(ActivityUiState.Loading)
    val uiState: StateFlow<ActivityUiState> = _uiState

    init {
        loadActivities()
    }

    fun loadActivities(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val cachedActivities = SessionManager.activities
            if (!forceRefresh && cachedActivities != null) {
                _uiState.value =
                    if (cachedActivities.isEmpty()) {
                        ActivityUiState.Empty
                    } else {
                        ActivityUiState.Success(cachedActivities)
                    }
                return@launch
            }

            _uiState.value = ActivityUiState.Loading
            try {
                val activities = RetrofitProvider.activityApi.getActivities()
                val sortedActivities = activities.sortedByDescending { it.id }

                SessionManager.activities = sortedActivities

                _uiState.value =
                    if (sortedActivities.isEmpty()) {
                        ActivityUiState.Empty
                    } else {
                        ActivityUiState.Success(sortedActivities)
                    }
            } catch (e: IOException) {
                Log.e(TAG, "Network error loading activities", e)
                _uiState.value = ActivityUiState.Error(R.string.error_loading_activities)
            } catch (e: retrofit2.HttpException) {
                Log.e(TAG, "HTTP ${e.code()} loading activities", e)
                _uiState.value = ActivityUiState.Error(R.string.error_loading_activities)
            }
        }
    }

    fun deleteActivity(id: Int) {
        viewModelScope.launch {
            try {
                val response = RetrofitProvider.activityApi.deleteActivity(id)
                if (response.isSuccessful) {
                    val currentState = _uiState.value
                    if (currentState is ActivityUiState.Success) {
                        val updatedActivities = currentState.activities.filter { it.id != id }

                        SessionManager.activities = updatedActivities

                        _uiState.value =
                            if (updatedActivities.isEmpty()) {
                                ActivityUiState.Empty
                            } else {
                                ActivityUiState.Success(updatedActivities)
                            }
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network error deleting activity", e)
                _uiState.value = ActivityUiState.Error(R.string.error_deleting_activity)
            } catch (e: retrofit2.HttpException) {
                Log.e(TAG, "HTTP ${e.code()} deleting activity", e)
                _uiState.value = ActivityUiState.Error(R.string.error_deleting_activity)
            }
        }
    }

    fun updateActivitySensation(
        activityId: Int,
        newSensationValue: Int,
    ) {
        viewModelScope.launch {
            try {
                val request = UpdateSensationRequest(sensacio = newSensationValue)

                val response = RetrofitProvider.activityApi.updateActivitySensation(activityId, request)

                if (response.isSuccessful) {
                    val currentState = _uiState.value
                    if (currentState is ActivityUiState.Success) {
                        val updatedActivities =
                            currentState.activities.map { activity ->
                                if (activity.id == activityId) {
                                    activity.copy(sensacio = newSensationValue)
                                } else {
                                    activity
                                }
                            }
                        _uiState.value = ActivityUiState.Success(updatedActivities)
                    }
                } else {
                    _uiState.value = ActivityUiState.Error(R.string.error_updating_activity_sensation)
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network error updating sensation", e)
                _uiState.value = ActivityUiState.Error(R.string.error_updating_activity_sensation)
            } catch (e: retrofit2.HttpException) {
                Log.e(TAG, "HTTP ${e.code()} updating sensation", e)
                _uiState.value = ActivityUiState.Error(R.string.error_updating_activity_sensation)
            }
        }
    }

    fun updateActivityComment(
        activityId: Int,
        newComment: String,
    ) {
        viewModelScope.launch {
            try {
                val request = UpdateCommentRequest(comentari = newComment)
                val response = RetrofitProvider.activityApi.updateActivityComment(activityId, request)

                if (response.isSuccessful) {
                    val currentState = _uiState.value
                    if (currentState is ActivityUiState.Success) {
                        val updatedActivities =
                            currentState.activities.map { activity ->
                                if (activity.id == activityId) {
                                    activity.copy(comentari = newComment)
                                } else {
                                    activity
                                }
                            }
                        _uiState.value = ActivityUiState.Success(updatedActivities)
                    }
                } else {
                    _uiState.value = ActivityUiState.Error(R.string.error_updating_activity_comment)
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network error updating comment", e)
                _uiState.value = ActivityUiState.Error(R.string.error_updating_activity_comment)
            } catch (e: retrofit2.HttpException) {
                Log.e(TAG, "HTTP ${e.code()} updating comment", e)
                _uiState.value = ActivityUiState.Error(R.string.error_updating_activity_comment)
            }
        }
    }
}
