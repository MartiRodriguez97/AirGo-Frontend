package com.airgo.airgoApp.features.home.components

import android.app.Activity
import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowInsetsControllerCompat
import com.airgo.airgoApp.R
import com.airgo.airgoApp.features.home.domain.HomeViewModel
import com.airgo.airgoApp.features.map.helpers.NominatimResult

private const val TRIMMED_LENGTH = 5

@Composable
fun pendingBuildingCard(onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Business,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(end = 16.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_building_pending_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.home_building_pending_desc),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            TextButton(onClick = onClick) {
                Text(stringResource(R.string.home_building_btn_verify))
            }
        }
    }
}

@Composable
fun buildingAddressDialog(
    viewModel: HomeViewModel,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val view = LocalView.current

    val hideKeyboard =
        remember {
            {
                (context as? Activity)?.let { activity ->
                    WindowInsetsControllerCompat(activity.window, view)
                        .hide(
                            androidx.core.view.WindowInsetsCompat.Type
                                .ime(),
                        )
                }
                (context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                    ?.hideSoftInputFromWindow(view.windowToken, 0)
            }
        }

    val numeroFocusRequester = remember { FocusRequester() }
    var adreca by remember { mutableStateOf("") }
    var numero by remember { mutableStateOf("") }
    val results by viewModel.addressResults.collectAsState()
    val isSearching by viewModel.isSearchingAddress.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    if (errorMessage != null) {
        errorDialog(onDismiss = { viewModel.clearErrorMessage() }, messageRes = errorMessage!!)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                dialogHeader()

                addressInputField(
                    adreca = adreca,
                    isLoading = isLoading,
                    isSearching = isSearching,
                    onValueChange = {
                        adreca = it
                        viewModel.searchAddresses(it)
                    },
                    onNext = { numeroFocusRequester.requestFocus() },
                )

                if (results.isNotEmpty()) {
                    addressResultsList(
                        results = results,
                        onResultSelected = { result ->
                            val parts = result.displayName.split(",")
                            if (parts.isNotEmpty()) adreca = parts[0].trim()
                            parts.drop(1).forEach { part ->
                                val trimmed = part.trim()
                                if (trimmed.all { it.isDigit() } && trimmed.length < TRIMMED_LENGTH) {
                                    numero = trimmed
                                }
                            }
                            viewModel.searchAddresses("")
                        },
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                numberInputField(
                    numero = numero,
                    isLoading = isLoading,
                    focusRequester = numeroFocusRequester,
                    onValueChange = { if (it.all { char -> char.isDigit() }) numero = it },
                    onDone = {
                        hideKeyboard()
                        focusManager.clearFocus(force = true)
                    },
                )

                Spacer(modifier = Modifier.height(16.dp))

                submitButton(
                    enabled = adreca.isNotBlank() && numero.isNotBlank() && !isLoading,
                    isLoading = isLoading,
                    onClick = {
                        hideKeyboard()
                        focusManager.clearFocus(force = true)
                        viewModel.searchAddresses("")
                        viewModel.checkBuildingAddress("Barcelona", adreca, numero)
                    },
                )
            }
        }
    }
}

@Composable
private fun errorDialog(
    onDismiss: () -> Unit,
    messageRes: Int,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.error_dialog_title)) },
        text = { Text(stringResource(messageRes)) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_continue)) }
        },
    )
}

@Composable
private fun dialogHeader() {
    Text(
        text = stringResource(R.string.home_building_dialog_title),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.home_building_dialog_desc),
        style = MaterialTheme.typography.bodyMedium,
    )
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun addressInputField(
    adreca: String,
    isLoading: Boolean,
    isSearching: Boolean,
    onValueChange: (String) -> Unit,
    onNext: () -> Unit,
) {
    OutlinedTextField(
        value = adreca,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.home_building_address_label)) },
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading,
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { onNext() }),
        trailingIcon = {
            if (isSearching) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Icon(Icons.Default.Search, contentDescription = null)
            }
        },
    )
}

@Composable
private fun addressResultsList(
    results: List<NominatimResult>,
    onResultSelected: (NominatimResult) -> Unit,
) {
    LazyColumn(modifier = Modifier.height(150.dp)) {
        items(results) { result ->
            addressResultItem(result) { onResultSelected(result) }
            HorizontalDivider()
        }
    }
}

@Composable
private fun numberInputField(
    numero: String,
    isLoading: Boolean,
    focusRequester: FocusRequester,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
) {
    OutlinedTextField(
        value = numero,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.home_building_number_label)) },
        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
        enabled = !isLoading,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
    )
}

@Composable
private fun submitButton(
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(stringResource(R.string.home_building_btn_submit))
    }
}

@Composable
private fun addressResultItem(
    result: NominatimResult,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = result.displayName,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
        )
    }
}
