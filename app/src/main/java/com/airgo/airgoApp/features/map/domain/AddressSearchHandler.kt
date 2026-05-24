package com.airgo.airgoApp.features.map.domain

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.airgo.airgoApp.features.map.helpers.NominatimResult
import com.airgo.airgoApp.features.map.helpers.NominatimService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

private const val MIN_QUERY_LENGTH = 3

class AddressSearchHandler(
    private val scope: CoroutineScope,
) {
    var startAddressQuery by mutableStateOf("")
    var endAddressQuery by mutableStateOf("")
    var startSuggestions by mutableStateOf<List<NominatimResult>>(emptyList())
    var endSuggestions by mutableStateOf<List<NominatimResult>>(emptyList())
    var isSearchingAddress by mutableStateOf(false)
    var waypointAddresses by mutableStateOf<List<String>>(emptyList())

    fun searchStart(query: String) {
        startAddressQuery = query
        startSuggestions = emptyList()
        if (query.length < MIN_QUERY_LENGTH) return
        scope.launch {
            isSearchingAddress = true
            startSuggestions = NominatimService.searchAddress(query)
            isSearchingAddress = false
        }
    }

    fun searchEnd(query: String) {
        endAddressQuery = query
        endSuggestions = emptyList()
        if (query.length < MIN_QUERY_LENGTH) return
        scope.launch {
            isSearchingAddress = true
            endSuggestions = NominatimService.searchAddress(query)
            isSearchingAddress = false
        }
    }

    fun selectStart(result: NominatimResult): GeoPoint {
        startAddressQuery = result.displayName
        startSuggestions = emptyList()
        return GeoPoint(result.lat.toDouble(), result.lon.toDouble())
    }

    fun selectEnd(result: NominatimResult): GeoPoint {
        endAddressQuery = result.displayName
        endSuggestions = emptyList()
        return GeoPoint(result.lat.toDouble(), result.lon.toDouble())
    }

    fun selectWaypoint(
        result: NominatimResult,
        index: Int,
    ): GeoPoint {
        val point = GeoPoint(result.lat.toDouble(), result.lon.toDouble())
        waypointAddresses =
            waypointAddresses.toMutableList().also {
                if (index < it.size) it[index] = result.displayName else it.add(result.displayName)
            }
        return point
    }

    fun updateWaypointAddress(
        index: Int,
        address: String,
    ) {
        waypointAddresses =
            waypointAddresses.toMutableList().also {
                if (index < it.size) it[index] = address else it.add(address)
            }
    }

    fun removeWaypointAddress(index: Int) {
        waypointAddresses =
            waypointAddresses.toMutableList().also {
                if (index < it.size) it.removeAt(index)
            }
    }

    fun clear() {
        startAddressQuery = ""
        endAddressQuery = ""
        waypointAddresses = emptyList()
        startSuggestions = emptyList()
        endSuggestions = emptyList()
    }
}
