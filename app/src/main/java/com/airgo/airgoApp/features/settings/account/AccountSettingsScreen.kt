package com.airgo.airgoApp.features.settings.account

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.features.settings.account.tebici.domain.TeBiciUiState
import com.airgo.airgoApp.features.settings.account.tebici.domain.TeBiciViewModel
import com.airgo.airgoApp.features.settings.account.titols.editTitolScreen
import com.airgo.airgoApp.features.settings.account.username.editUsernameScreen

// Pantalla principal de configuración de cuenta,
// que muestra el nombre de usuario, títulos y estado de bicicleta disponible,
// y permite navegar a las pantallas de edición correspondientes
@Composable
fun accountSettingsScreen(
    displayName: String,
    onBackClick: () -> Unit,
    teBiciViewModel: TeBiciViewModel = viewModel(),
) {
    var screenState by remember { mutableStateOf("MAIN") }
    val session = SessionManager
    val teBiciUiState by teBiciViewModel.uiState.collectAsState()
    var currentTitol by remember { mutableStateOf(session.titol ?: "") }
    // Update local state, initialize with parameters or SessionManager
    var currentDisplayName by remember { mutableStateOf(session.username ?: displayName) }
    var isBikeAvailable by remember {
        mutableStateOf(session.teBici ?: false) // Ajusta segn tu lgica
    }

    handleTeBiciSideEffects(
        uiState = teBiciUiState,
        viewModel = teBiciViewModel,
        onResetBikeAvailability = { isBikeAvailable = session.teBici ?: false },
    )
    BackHandler {
        if (screenState != "MAIN") {
            screenState = "MAIN"
        } else {
            onBackClick()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = screenState,
            transitionSpec = {
                if (targetState != "MAIN") {
                    // Profile -> Settings
                    (slideInHorizontally { fullWidth -> fullWidth } + fadeIn()).togetherWith(
                        slideOutHorizontally { fullWidth -> -fullWidth } + fadeOut(),
                    )
                } else {
                    // Settings -> Profile
                    (slideInHorizontally { fullWidth -> -fullWidth } + fadeIn()).togetherWith(
                        slideOutHorizontally { fullWidth -> fullWidth } + fadeOut(),
                    )
                }
            },
            label = "settings-transition",
        ) { state ->
            when (state) {
                "MAIN" -> {
                    accountSettingsContent(
                        state =
                            AccountSettingsState(
                                displayName = currentDisplayName,
                                titol = currentTitol,
                                isEnabled = isBikeAvailable,
                                isLoadingBike = teBiciUiState is TeBiciUiState.Loading,
                            ),
                        onBicycleToggle = { newValue ->
                            isBikeAvailable = newValue
                            teBiciViewModel.updateBikeAvailability(newValue)
                        },
                        onEditUsername = { screenState = "EDIT_USERNAME" },
                        onEditTitol = { screenState = "EDIT_TITOL" },
                        onBackClick = onBackClick,
                    )
                }
                "EDIT_USERNAME" -> {
                    editUsernameScreen(
                        displayName = currentDisplayName,
                        onBackClick = { screenState = "MAIN" },
                        onSave = { newName ->
                            currentDisplayName = newName
                        },
                    )
                }
                "EDIT_TITOL" -> {
                    editTitolScreen(
                        currentTitol = currentTitol,
                        onBackClick = { screenState = "MAIN" },
                        onSave = { newTitol ->
                            currentTitol = newTitol
                        },
                    )
                }
            }
        }
    }
}

data class AccountSettingsState(
    val displayName: String,
    val titol: String,
    val isEnabled: Boolean,
    val isLoadingBike: Boolean,
)

// Maneja los efectos secundarios relacionados con la actualización de la disponibilidad de bicicleta,
// mostrando mensajes de éxito o error según corresponda
@Composable
private fun handleTeBiciSideEffects(
    uiState: TeBiciUiState,
    viewModel: TeBiciViewModel,
    onResetBikeAvailability: () -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(uiState) {
        when (uiState) {
            is TeBiciUiState.Success -> {
                Toast.makeText(context, "Estado de la bicicleta actualizado", Toast.LENGTH_SHORT).show()
                viewModel.resetUiState()
            }
            is TeBiciUiState.Error -> {
                Toast.makeText(context, uiState.message, Toast.LENGTH_LONG).show()
                onResetBikeAvailability()
                viewModel.resetUiState()
            }
            else -> {}
        }
    }
}

// Composable que muestra el contenido principal de la pantalla de configuración de cuenta,
// incluyendo el nombre de usuario, títulos y estado de bicicleta disponible,
// con opciones para editar cada uno y un botón de retroceso en el encabezado
@Composable
private fun accountSettingsContent(
    state: AccountSettingsState, // Agrupamos los 4 parámetros de interfaz aquí
    onBicycleToggle: (Boolean) -> Unit,
    onEditUsername: () -> Unit,
    onEditTitol: () -> Unit,
    onBackClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
    ) {
        accountSettingsHeader(onBackClick = onBackClick)
        HorizontalDivider(thickness = DividerDefaults.Thickness, color = DividerDefaults.color)
        usernameRow(
            displayName = state.displayName,
            onEditUsername = onEditUsername,
        )
        HorizontalDivider(thickness = DividerDefaults.Thickness, color = DividerDefaults.color)
        titolRow(
            titol = state.titol,
            onEditTitol = onEditTitol,
        )
        HorizontalDivider(thickness = DividerDefaults.Thickness, color = DividerDefaults.color)
        bicycleAvailableRow(
            isEnabled = state.isEnabled,
            isLoading = state.isLoadingBike,
            onBicycleToggle = onBicycleToggle,
        )
        HorizontalDivider(thickness = DividerDefaults.Thickness, color = DividerDefaults.color)
    }
}

// Composable para el encabezado de la pantalla de configuración de cuenta,
// con un botón de retroceso y el título de la sección centrado
@Composable
private fun accountSettingsHeader(onBackClick: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "Tu cuenta",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

// Composable para mostrar una fila con el nombre de usuario,
// que es clickeable para navegar a la pantalla de edición de nombre de usuario,
// y que muestra un ícono de flecha para indicar que es una opción navegable
@Composable
private fun usernameRow(
    displayName: String,
    onEditUsername: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onEditUsername() }
                .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Nombre de usuario",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = displayName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
            contentDescription = "Editar",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
    }
}

// Composable para mostrar una fila con el título del usuario,
// que es clickeable para navegar a la pantalla de edición de títulos,
// y que muestra un ícono de flecha para indicar que es una opción navegable.
// El título se muestra en un estilo de texto más tenue para diferenciarlo del nombre de usuario
@Composable
private fun titolRow(
    titol: String,
    onEditTitol: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onEditTitol() }
                .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Títulos",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = titol,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
            contentDescription = "Editar",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
    }
}

// Composable para mostrar una fila con un switch para activar o desactivar
// la disponibilidad de bicicleta, que muestra el estado actual y que se deshabilita
// mientras se está cargando la actualización del estado de la bicicleta, para
// evitar cambios múltiples mientras se procesa la solicitud
@Composable
private fun bicycleAvailableRow(
    isEnabled: Boolean,
    onBicycleToggle: (Boolean) -> Unit,
    isLoading: Boolean,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Bicicleta disponible",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = isEnabled,
            onCheckedChange = onBicycleToggle,
            enabled = !isLoading, // Deshabilitar el switch si está cargando
        )
    }
}
