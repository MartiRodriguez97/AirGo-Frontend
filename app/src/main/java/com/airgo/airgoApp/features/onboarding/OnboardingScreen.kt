package com.airgo.airgoApp.features.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airgo.airgoApp.R
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.features.onboarding.domain.OnboardingUiState
import com.airgo.airgoApp.features.onboarding.domain.OnboardingViewModel

private data class OnboardingFieldsState(
    val username: String,
    val pes: String,
    val altura: String,
    val teBici: Boolean,
    val usernameError: String?,
    val pesError: String?,
    val alturaError: String?,
    val onUsernameChange: (String) -> Unit,
    val onPesChange: (String) -> Unit,
    val onAlturaChange: (String) -> Unit,
    val onTeBiciChange: (Boolean) -> Unit,
)

@Composable
fun onboardingScreen(
    onCompleted: () -> Unit,
    viewModel: OnboardingViewModel = viewModel(),
) {
    LaunchedEffect(viewModel.uiState) {
        if (viewModel.uiState is OnboardingUiState.Success) {
            onCompleted()
        }
    }

    val state = viewModel.uiState
    if (state is OnboardingUiState.LoadingProfile) {
        onboardingLoadingContent()
    } else {
        val isSaving = state is OnboardingUiState.Saving
        val errorMessage = if (state is OnboardingUiState.Error) state.message.asString() else null

        Box(modifier = Modifier.fillMaxSize()) {
            onboardingFormContent(
                errorMessage = errorMessage,
                isSaving = isSaving,
                onSave = { username, pes, altura, teBici ->
                    viewModel.saveOnboarding(username, pes, altura, teBici)
                },
            )
            if (isSaving) {
                // Dim screen slightly while showing progress
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun onboardingLoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun onboardingFormContent(
    errorMessage: String?,
    isSaving: Boolean,
    onSave: (username: String, pes: Double, altura: Double, teBici: Boolean) -> Unit,
) {
    var username by remember { mutableStateOf(SessionManager.username ?: "") }
    var pes by remember { mutableStateOf("") }
    var altura by remember { mutableStateOf("") }
    var teBici by remember { mutableStateOf(false) }

    var usernameError by remember { mutableStateOf<String?>(null) }
    var pesError by remember { mutableStateOf<String?>(null) }
    var alturaError by remember { mutableStateOf<String?>(null) }

    val userAlreadyExistsMsg = stringResource(R.string.error_username_already_exists)

    LaunchedEffect(errorMessage) {
        if (errorMessage == userAlreadyExistsMsg) {
            usernameError = errorMessage
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        onboardingHeader()
        Spacer(modifier = Modifier.height(32.dp))
        onboardingFields(
            state =
                OnboardingFieldsState(
                    username = username,
                    pes = pes,
                    altura = altura,
                    teBici = teBici,
                    usernameError = usernameError,
                    pesError = pesError,
                    alturaError = alturaError,
                    onUsernameChange = {
                        username = it
                        usernameError = null
                    },
                    onPesChange = {
                        if (isValidNumberInput(it)) {
                            pes = it
                            pesError = null
                        }
                    },
                    onAlturaChange = {
                        if (isValidNumberInput(it)) {
                            altura = it
                            alturaError = null
                        }
                    },
                    onTeBiciChange = { teBici = it },
                ),
        )
        Spacer(modifier = Modifier.height(8.dp))
        onboardingErrorText(errorMessage, userAlreadyExistsMsg)
        onboardingSubmitButton(
            isSaving = isSaving,
            username = username,
            pes = pes,
            altura = altura,
            teBici = teBici,
            onValidationError = { u, p, a ->
                usernameError = u
                pesError = p
                alturaError = a
            },
            onSave = onSave,
        )
    }
}

@Composable
private fun onboardingErrorText(
    errorMessage: String?,
    userAlreadyExistsMsg: String,
) {
    if (errorMessage != null && errorMessage != userAlreadyExistsMsg) {
        Text(
            text = errorMessage,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

private fun isValidNumberInput(input: String) = input.isEmpty() || input.matches(Regex("^\\d*$|^\\d+\\.\\d*$"))

@Composable
private fun onboardingSubmitButton(
    isSaving: Boolean,
    username: String,
    pes: String,
    altura: String,
    teBici: Boolean,
    onValidationError: (String?, String?, String?) -> Unit,
    onSave: (String, Double, Double, Boolean) -> Unit,
) {
    val emptyUsernameMsg = stringResource(R.string.error_username_empty)
    val shortUsernameMsg = stringResource(R.string.error_username_too_short)
    val invalidWeightMsg = stringResource(R.string.error_invalid_weight)
    val invalidHeightMsg = stringResource(R.string.error_invalid_height)

    Button(
        onClick = {
            if (isSaving) return@Button
            val p = pes.toDoubleOrNull() ?: 0.0
            val a = altura.toDoubleOrNull() ?: 0.0

            val uError =
                when {
                    username.isBlank() -> emptyUsernameMsg
                    username.length < 4 -> shortUsernameMsg
                    else -> null
                }
            val pError = if (p <= 0) invalidWeightMsg else null
            val aError = if (a <= 0) invalidHeightMsg else null

            if (uError == null && pError == null && aError == null) {
                onSave(username, p, a, teBici)
            } else {
                onValidationError(uError, pError, aError)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = !isSaving,
    ) { Text(stringResource(R.string.continue_on_boarding)) }
}

@Composable
private fun onboardingHeader() {
    Text(text = stringResource(R.string.welcome_on_boarding), style = MaterialTheme.typography.headlineMedium)
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.complete_profile_on_boarding),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun onboardingFields(state: OnboardingFieldsState) {
    OutlinedTextField(
        value = state.username,
        onValueChange = state.onUsernameChange,
        label = { Text(stringResource(R.string.account_settings_username_label)) },
        isError = state.usernameError != null,
        supportingText = {
            if (state.usernameError != null) {
                Text(text = state.usernameError)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(
        value = state.pes,
        onValueChange = state.onPesChange,
        label = { Text(stringResource(R.string.weight_on_boarding)) },
        isError = state.pesError != null,
        supportingText = {
            if (state.pesError != null) {
                Text(text = state.pesError)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(
        value = state.altura,
        onValueChange = state.onAlturaChange,
        label = { Text(stringResource(R.string.height_on_boarding)) },
        isError = state.alturaError != null,
        supportingText = {
            if (state.alturaError != null) {
                Text(text = state.alturaError)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
    Spacer(modifier = Modifier.height(16.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = stringResource(R.string.own_bike_on_boarding), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = state.teBici, onCheckedChange = state.onTeBiciChange)
    }
}
