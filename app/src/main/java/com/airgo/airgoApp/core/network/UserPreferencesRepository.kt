package com.airgo.airgoApp.core.network

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
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
        val USERNAME = stringPreferencesKey("username")
        val EMAIL = stringPreferencesKey("email")
        val USER_ID = intPreferencesKey("user_id")
        val PROFILE_PICTURE_URL = stringPreferencesKey("profile_picture_url")
        val TITOL = stringPreferencesKey("titol")
        val TE_BICI = booleanPreferencesKey("te_bici")
        val PES = doublePreferencesKey("pes")
        val ALTURA = doublePreferencesKey("altura")
        val PUNTS = intPreferencesKey("punts")
        val RATXA = intPreferencesKey("ratxa")
        val IDIOMA = stringPreferencesKey("idioma")
        val LIMIT_RUTES = intPreferencesKey("limit_rutes")
        val INSIGNIES = stringPreferencesKey("insignies")
    }

    val userPreferencesFlow: Flow<UserPreferences> =
        dataStore.data.map { preferences ->
            UserPreferences(
                token = preferences[PreferencesKeys.TOKEN],
                refreshToken = preferences[PreferencesKeys.REFRESH_TOKEN],
                username = preferences[PreferencesKeys.USERNAME],
                email = preferences[PreferencesKeys.EMAIL],
                userId = preferences[PreferencesKeys.USER_ID],
                profilePictureUrl = preferences[PreferencesKeys.PROFILE_PICTURE_URL],
                titol = preferences[PreferencesKeys.TITOL],
                teBici = preferences[PreferencesKeys.TE_BICI] ?: false,
                pes = preferences[PreferencesKeys.PES],
                altura = preferences[PreferencesKeys.ALTURA],
                punts = preferences[PreferencesKeys.PUNTS],
                ratxa = preferences[PreferencesKeys.RATXA],
                idioma = preferences[PreferencesKeys.IDIOMA],
                limitRutes = preferences[PreferencesKeys.LIMIT_RUTES],
                insignies = preferences[PreferencesKeys.INSIGNIES],
            )
        }

    suspend fun saveSession(
        token: String,
        refreshToken: String?,
        username: String,
        email: String?,
        userId: Int?,
        profilePictureUrl: String?,
        titol: String?,
        teBici: Boolean,
        pes: Double?,
        altura: Double?,
        punts: Int?,
        ratxa: Int?,
        idioma: String?,
        limitRutes: Int?,
        insignies: String?,
    ) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.TOKEN] = token
            if (refreshToken !=
                null
            ) {
                preferences[PreferencesKeys.REFRESH_TOKEN] = refreshToken
            } else {
                preferences.remove(PreferencesKeys.REFRESH_TOKEN)
            }
            preferences[PreferencesKeys.USERNAME] = username
            if (email != null) preferences[PreferencesKeys.EMAIL] = email else preferences.remove(PreferencesKeys.EMAIL)
            if (userId != null) preferences[PreferencesKeys.USER_ID] = userId else preferences.remove(PreferencesKeys.USER_ID)
            if (profilePictureUrl !=
                null
            ) {
                preferences[PreferencesKeys.PROFILE_PICTURE_URL] = profilePictureUrl
            } else {
                preferences.remove(PreferencesKeys.PROFILE_PICTURE_URL)
            }
            if (titol != null) preferences[PreferencesKeys.TITOL] = titol
            else preferences.remove(PreferencesKeys.TITOL)

            preferences[PreferencesKeys.TE_BICI] = teBici

            if (pes != null) preferences[PreferencesKeys.PES] = pes
            else preferences.remove(PreferencesKeys.PES)
            if (altura != null) preferences[PreferencesKeys.ALTURA] = altura
            else preferences.remove(PreferencesKeys.ALTURA)
            if (punts != null) preferences[PreferencesKeys.PUNTS] = punts
            else preferences.remove(PreferencesKeys.PUNTS)
            if (ratxa != null) preferences[PreferencesKeys.RATXA] = ratxa
            else preferences.remove(PreferencesKeys.RATXA)
            if (idioma != null) preferences[PreferencesKeys.IDIOMA] = idioma
            else preferences.remove(PreferencesKeys.IDIOMA)
            if (limitRutes !=
                null
            ) {
                preferences[PreferencesKeys.LIMIT_RUTES] = limitRutes
            } else {
                preferences.remove(PreferencesKeys.LIMIT_RUTES)
            }
            if (insignies != null) preferences[PreferencesKeys.INSIGNIES] = insignies
            else preferences.remove(PreferencesKeys.INSIGNIES)
        }
    }

    suspend fun updateBikeAvailability(teBici: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.TE_BICI] = teBici
        }
    }

    suspend fun clearSession() {
        dataStore.edit { it.clear() }
    }
}

data class UserPreferences(
    val token: String?,
    val refreshToken: String?,
    val username: String?,
    val email: String?,
    val userId: Int?,
    val profilePictureUrl: String?,
    val titol: String?,
    val teBici: Boolean,
    val pes: Double?,
    val altura: Double?,
    val punts: Int?,
    val ratxa: Int?,
    val idioma: String?,
    val limitRutes: Int?,
    val insignies: String?,
)
