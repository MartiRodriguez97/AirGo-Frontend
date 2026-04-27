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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airgo.airgoApp.features.onboarding.domain.OnboardingUiState
import com.airgo.airgoApp.features.onboarding.domain.OnboardingViewModel

private data class OnboardingFieldsState(
    val username: String,
    val pes: String,
    val altura: String,
    val teBici: Boolean,
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

    when (viewModel.uiState) {
        is OnboardingUiState.LoadingProfile,
        is OnboardingUiState.Saving,
        -> onboardingLoadingContent()
        is OnboardingUiState.NeedsOnboarding ->
            onboardingFormContent(
                errorMessage = null,
                onSave = { username, pes, altura, teBici ->
                    viewModel.saveOnboarding(username, pes, altura, teBici)
                },
            )
        is OnboardingUiState.Error ->
            onboardingFormContent(
                errorMessage = (viewModel.uiState as OnboardingUiState.Error).message,
                onSave = { username, pes, altura, teBici ->
                    viewModel.saveOnboarding(username, pes, altura, teBici)
                },
            )
        else -> {}
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
    onSave: (username: String, pes: Int, altura: Int, teBici: Boolean) -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var pes by remember { mutableStateOf("") }
    var altura by remember { mutableStateOf("") }
    var teBici by remember { mutableStateOf(false) }

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
                    onUsernameChange = { username = it },
                    onPesChange = { pes = it },
                    onAlturaChange = { altura = it },
                    onTeBiciChange = { teBici = it },
                ),
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        Button(
            onClick = { onSave(username, pes.toIntOrNull() ?: 0, altura.toIntOrNull() ?: 0, teBici) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Continuar") }
    }
}

@Composable
private fun onboardingHeader() {
    Text(text = "Benvingut a AirGo!", style = MaterialTheme.typography.headlineMedium)
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Completa el teu perfil per continuar",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun onboardingFields(state: OnboardingFieldsState) {
    OutlinedTextField(
        value = state.username,
        onValueChange = state.onUsernameChange,
        label = { Text("Nom d'usuari") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(
        value = state.pes,
        onValueChange = state.onPesChange,
        label = { Text("Pes (kg)") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(
        value = state.altura,
        onValueChange = state.onAlturaChange,
        label = { Text("Altura (cm)") },
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
        Text(text = "Tens bicicleta pròpia?", style = MaterialTheme.typography.bodyLarge)
        Switch(checked = state.teBici, onCheckedChange = state.onTeBiciChange)
    }
}
