package com.airgo.airgoApp.core.network

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class UserPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) {
    private object PreferencesKeys {
        val TOKEN = stringPreferencesKey("token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val USER_ID = intPreferencesKey("user_id")
    }

    val userPreferencesFlow: Flow<UserPreferences> =
        dataStore.data.map { preferences ->
            UserPreferences(
                token = preferences[PreferencesKeys.TOKEN],
                refreshToken = preferences[PreferencesKeys.REFRESH_TOKEN],
                userId = preferences[PreferencesKeys.USER_ID],
            )
        }

    private fun <T> androidx.datastore.preferences.core.MutablePreferences.setOrRemove(
        key: androidx.datastore.preferences.core.Preferences.Key<T>,
        value: T?,
    ) {
        if (value != null) {
            this[key] = value
        } else {
            this.remove(key)
        }
    }

    suspend fun saveSession(userPrefs: UserPreferences) {
        dataStore.edit { preferences ->
            preferences.setOrRemove(PreferencesKeys.TOKEN, userPrefs.token)
            preferences.setOrRemove(PreferencesKeys.REFRESH_TOKEN, userPrefs.refreshToken)
            preferences.setOrRemove(PreferencesKeys.USER_ID, userPrefs.userId)
        }
    }

    suspend fun clearSession() {
        dataStore.edit { it.clear() }
    }
}

data class UserPreferences(
    val token: String?,
    val refreshToken: String?,
    val userId: Int?,
)
