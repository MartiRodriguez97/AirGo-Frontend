package com.airgo.airgoApp.features.tracking.domain

import android.content.ContentValues.TAG
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresExtension
import androidx.lifecycle.viewModelScope
import com.airgo.airgoApp.R
import com.airgo.airgoApp.core.AppEvents
import com.airgo.airgoApp.core.network.RetrofitProvider
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.core.utils.UiText
import com.airgo.airgoApp.features.profile.data.UpdateUserRequest
import com.airgo.airgoApp.features.settings.account.titols.data.UserTitol
import com.airgo.airgoApp.features.tracking.data.SaveActivityRequest
import com.airgo.airgoApp.features.tracking.data.UnlockTitleRequest
import com.airgo.airgoApp.features.tracking.data.UpdateUserStatsRequest
import com.airgo.airgoApp.features.tracking.domain.TrackingViewModel.SaveState
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

private const val METERS_PER_KM = 1000.0
private const val INVALID_ACTIVITY_CODE = 422

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
fun TrackingViewModel.saveActivity() {
    viewModelScope.launch {
        saveState = TrackingViewModel.SaveState.Saving
        try {
            val activeExercise = SessionManager.exerciciActiu
            val distanceDoneKm = data.distanceMeters / METERS_PER_KM

            val request =
                SaveActivityRequest(
                    distanceMeters = data.distanceMeters,
                    durationSeconds = data.elapsedSeconds,
                    avgSpeedKmh = data.avgSpeedKmh,
                    completat = true,
                    routePoints = trackPoints.map { listOf(it.latitude, it.longitude) },
                    sensacio = selectedSensation,
                )
            val response = RetrofitProvider.trackingApi.saveActivity(request)
            SessionManager.activities = null
            SessionManager.ratxa = response.currentStreak
            val oldPunts = SessionManager.punts ?: 0
            val newPunts = oldPunts + response.pointsEarned

            if (response.newBadges.isNotEmpty()) {
                try {
                    SessionManager.userBadges = RetrofitProvider.profileApi.getUserBadges()
                } catch (e: HttpException) {
                    Log.e(TAG, "Failed to refresh user badges (HTTP)", e)
                } catch (e: IOException) {
                    Log.e(TAG, "Failed to refresh user badges (IO)", e)
                }
            }

            SessionManager.userId?.let { userId ->
                try {
                    RetrofitProvider.trackingApi.updateUserStats(
                        id = userId,
                        request =
                            UpdateUserStatsRequest(
                                punts = response.pointsEarned,
                                ratxa = response.currentStreak,
                            ),
                    )
                } catch (e: HttpException) {
                    Log.e(TAG, "Failed to update user stats (HTTP)", e)
                } catch (e: IOException) {
                    Log.e(TAG, "Failed to update user stats (IO)", e)
                }
            }

            try {
                val profile = RetrofitProvider.profileApi.getUserProfile()
                SessionManager.punts = profile.punts
            } catch (e: HttpException) {
                Log.e(TAG, "Failed to refresh profile locally (HTTP)", e)
                SessionManager.punts = newPunts
            } catch (e: IOException) {
                Log.e(TAG, "Failed to refresh profile locally (IO)", e)
                SessionManager.punts = newPunts
            }

            if (activeExercise != null) {
                updateExerciseProgress(activeExercise, distanceDoneKm)
            }

            AppEvents.notifyActivitySaved()
            titolsPendents = response.titolsPendents
            SessionManager.titolsPendents = response.titolsPendents
            saveState = SaveState.Success(response)
        } catch (e: IOException) {
            Log.e(TAG, "Network error saving activity: ${e.message}", e)
            saveState = SaveState.Error(UiText.StringResource(R.string.error_saving_activity))
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e(TAG, "HTTP error saving activity: ${e.code()}, body: $errorBody")
            if (e.code() == INVALID_ACTIVITY_CODE) {
                val motiu =
                    try {
                        com.google.gson.JsonParser
                            .parseString(errorBody)
                            .asJsonObject
                            .get("motiu")
                            ?.asString ?: "Activitat no vàlida"
                    } catch (ex: IllegalStateException) {
                        Log.e(TAG, "Error parsing 422 body: ${ex.message}", ex)
                        "Activitat no vàlida"
                    } catch (ex: com.google.gson.JsonParseException) {
                        Log.e(TAG, "Error parsing 422 JSON: ${ex.message}", ex)
                        "Activitat no vàlida"
                    }
                saveState = SaveState.InvalidActivity(motiu)
            } else {
                saveState = SaveState.Error(UiText.StringResource(R.string.error_saving_activity))
            }
        }
    }
}

fun TrackingViewModel.fetchAvailableTitles() {
    viewModelScope.launch {
        try {
            val response = RetrofitProvider.trackingApi.getAvailableTitles()
            availableTitles = response.opcions
            showTitleSelectionDialog = true
        } catch (e: IOException) {
            saveState = SaveState.Error(UiText.StringResource(R.string.error_fetching_titles))
            Log.e(TAG, "IO Error fetching titles", e)
        } catch (e: HttpException) {
            saveState = SaveState.Error(UiText.StringResource(R.string.error_fetching_titles))
            Log.e(TAG, "HTTP Error fetching titles", e)
        }
    }
}

fun TrackingViewModel.unlockTitle(
    titleId: Int,
    setAsMain: Boolean,
) {
    viewModelScope.launch {
        try {
            RetrofitProvider.trackingApi.unlockTitle(UnlockTitleRequest(titleId))

            if (setAsMain) {
                SessionManager.userId?.let { userId ->
                    val selectedTitle = availableTitles.find { it.id == titleId }
                    selectedTitle?.let {
                        RetrofitProvider.profileApi.updateUser(
                            userId,
                            UpdateUserRequest(titol = it.nom),
                        )
                        SessionManager.titol = it.nom
                    }
                }
            }

            availableTitles.find { it.id == titleId }?.let { title ->
                val currentList = SessionManager.availableTitols?.toMutableList() ?: mutableListOf()
                if (currentList.none { it.id == title.id }) {
                    currentList.add(
                        UserTitol(id = title.id, nom = title.nom, descripcio = title.descripcio),
                    )
                    SessionManager.availableTitols = currentList
                }
            }

            showTitleSelectionDialog = false
            val newValue = (SessionManager.titolsPendents - 1).coerceAtLeast(0)
            SessionManager.titolsPendents = newValue
            titolsPendents = newValue
        } catch (e: IOException) {
            saveState = SaveState.Error(UiText.StringResource(R.string.error_unlocking_title))
            Log.e(TAG, "IO Error unlocking title", e)
        } catch (e: HttpException) {
            saveState = SaveState.Error(UiText.StringResource(R.string.error_unlocking_title))
            Log.e(TAG, "HTTP Error unlocking title", e)
        }
    }
}
