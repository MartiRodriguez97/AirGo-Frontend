package com.airgo.airgoApp.features.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airgo.airgoApp.R
import com.airgo.airgoApp.features.map.domain.RouteViewModel
import com.airgo.airgoApp.features.map.helpers.NominatimResult
import com.airgo.airgoApp.features.map.helpers.NominatimService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SEARCH_DEBOUNCE_MS = 400L
private const val MIN_QUERY_LENGTH = 3

/**
 * Mode de selecció de punt per saber quin punt s'està cercant.
 */
enum class AddressSearchMode { START, END, WAYPOINT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun addressSearchScreen(
    mode: AddressSearchMode,
    waypointIndex: Int = -1,
    onBack: () -> Unit,
    routeViewModel: RouteViewModel = viewModel(),
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<NominatimResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var searchJob by remember { mutableStateOf<Job?>(null) }

    val title =
        when (mode) {
            AddressSearchMode.START -> stringResource(R.string.start_point)
            AddressSearchMode.END -> stringResource(R.string.end_point)
            AddressSearchMode.WAYPOINT -> stringResource(R.string.waypoint, waypointIndex + 2)
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { newQuery ->
                    query = newQuery
                    searchJob?.cancel()
                    if (newQuery.length >= MIN_QUERY_LENGTH) {
                        searchJob =
                            scope.launch {
                                delay(SEARCH_DEBOUNCE_MS)
                                isSearching = true
                                results = NominatimService.searchAddress(newQuery)
                                isSearching = false
                            }
                    } else {
                        results = emptyList()
                    }
                },
                placeholder = { Text(stringResource(R.string.write_address)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    if (isSearching) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null)
                    }
                },
            )
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(results, key = { it.placeId }) { result ->
                    addressResultItem(
                        result = result,
                        onClick = {
                            when (mode) {
                                AddressSearchMode.START -> routeViewModel.selectStartAddress(result)
                                AddressSearchMode.END -> routeViewModel.selectEndAddress(result)
                                AddressSearchMode.WAYPOINT ->
                                    routeViewModel.selectWaypointAddress(result, waypointIndex)
                                AddressSearchMode.WAYPOINT ->
                                    {
                                        routeViewModel.selectWaypointAddress(result, waypointIndex)
                                    }
                            }
                            onBack()
                        },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun addressResultItem(
    result: NominatimResult,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = result.displayName,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}
