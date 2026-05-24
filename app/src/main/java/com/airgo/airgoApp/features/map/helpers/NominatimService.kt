package com.airgo.airgoApp.features.map.helpers

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

private const val LOG_TAG = "NominatimService"
private const val NOMINATIM_URL = "https://nominatim.openstreetmap.org/search"
private const val USER_AGENT = "AirGoApp/1.0"
private const val MAX_RESULTS = 5

object NominatimService {
    private val client = OkHttpClient()
    private val gson = Gson()

    suspend fun searchAddress(query: String): List<NominatimResult> =
        withContext(Dispatchers.IO) {
            try {
                val url =
                    "$NOMINATIM_URL?q=${query.trim().replace(" ", "+")}" +
                        "&format=json&limit=$MAX_RESULTS&addressdetails=0" +
                        "&viewbox=2.0694,41.2971,2.2267,41.4695&bounded=1"
                val request =
                    Request
                        .Builder()
                        .url(url)
                        .header("User-Agent", USER_AGENT)
                        .build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: return@withContext emptyList()
                val type = object : TypeToken<List<NominatimResult>>() {}.type
                gson.fromJson(body, type)
            } catch (e: IOException) {
                Log.e(LOG_TAG, "Error searching address: ${e.message}", e)
                emptyList()
            } catch (e: IllegalStateException) {
                Log.e(LOG_TAG, "Error parsing address response: ${e.message}", e)
                emptyList()
            }
        }

    suspend fun reverseGeocode(
        lat: Double,
        lon: Double,
    ): String? =
        withContext(Dispatchers.IO) {
            try {
                val url = "https://nominatim.openstreetmap.org/reverse?lat=$lat&lon=$lon&format=json"
                val request =
                    Request
                        .Builder()
                        .url(url)
                        .header("User-Agent", USER_AGENT)
                        .build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: return@withContext null
                val json =
                    com.google.gson.JsonParser
                        .parseString(body)
                        .asJsonObject
                json.get("display_name")?.asString
            } catch (e: IOException) {
                Log.e(LOG_TAG, "Error reverse geocoding: ${e.message}", e)
                null
            } catch (e: IllegalStateException) {
                Log.e(LOG_TAG, "Error parsing reverse geocoding response: ${e.message}", e)
                null
            }
        }
}
