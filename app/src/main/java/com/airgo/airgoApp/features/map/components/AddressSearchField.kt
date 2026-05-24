package com.airgo.airgoApp.features.map.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.airgo.airgoApp.features.map.helpers.NominatimResult

private const val MAX_SUGGESTION_LENGTH = 60

/**
 * Camp de text per cercar una adreça amb suggeriments de Nominatim.
 * Inclou botó per activar la selecció al mapa.
 */
@Composable
internal fun addressSearchField(
    label: String,
    query: String,
    suggestions: List<NominatimResult>,
    isSearching: Boolean,
    isSelected: Boolean,
    onQueryChange: (String) -> Unit,
    onSuggestionSelected: (NominatimResult) -> Unit,
    onActivateMapSelection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember(suggestions) { mutableStateOf(suggestions.isNotEmpty()) }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                onQueryChange(it)
                expanded = true
            },
            label = { Text(label, style = MaterialTheme.typography.bodySmall) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            trailingIcon = {
                if (isSearching) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Search, contentDescription = null)
                }
            },
            textStyle = MaterialTheme.typography.bodySmall,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline,
            ),
        )
        IconButton(onClick = onActivateMapSelection) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Seleccionar al mapa",
                tint = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }

        DropdownMenu(
            expanded = expanded && suggestions.isNotEmpty(),
            onDismissRequest = { expanded = false },
        ) {
            suggestions.forEach { result ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = result.displayName.take(MAX_SUGGESTION_LENGTH) +
                                    if (result.displayName.length > MAX_SUGGESTION_LENGTH) "…" else "",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    onClick = {
                        onSuggestionSelected(result)
                        expanded = false
                    },
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}