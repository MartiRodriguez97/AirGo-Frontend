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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airgo.airgoApp.R
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.features.profile.components.deleteAccountSection
import com.airgo.airgoApp.features.profile.domain.ProfileUiState
import com.airgo.airgoApp.features.settings.account.tebici.domain.TeBiciUiState
import com.airgo.airgoApp.features.settings.account.tebici.domain.TeBiciViewModel
import com.airgo.airgoApp.features.settings.account.titols.editTitolScreen
import com.airgo.airgoApp.features.settings.account.username.editUsernameScreen

@Composable
fun accountSettingsScreen(
    displayName: String,
    uiState: ProfileUiState,
    onBackClick: () -> Unit,
    onShowDeleteDialog: () -> Unit,
    onLogout: () -> Unit,
    teBiciViewModel: TeBiciViewModel = viewModel(),
) {
    var screenState by remember { mutableStateOf("MAIN") }
    val session = SessionManager
    val teBiciUiState by teBiciViewModel.uiState.collectAsState()
    var currentTitol by remember { mutableStateOf(session.titol ?: "") }
    var currentDisplayName by remember { mutableStateOf(session.username ?: displayName) }
    var isBikeAvailable by remember { mutableStateOf(session.teBici ?: false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    handleTeBiciSideEffects(
        uiState = teBiciUiState,
        viewModel = teBiciViewModel,
        onResetBikeAvailability = { isBikeAvailable = session.teBici ?: false },
    )

    BackHandler { if (screenState != "MAIN") screenState = "MAIN" else onBackClick() }

    Box(modifier = Modifier.fillMaxSize()) {
        accountSettingsNavigation(
            screenState = screenState,
            state =
                AccountSettingsState(
                    displayName = currentDisplayName,
                    titol = currentTitol,
                    isEnabled = isBikeAvailable,
                    isLoadingBike = teBiciUiState is TeBiciUiState.Loading,
                ),
            uiState = uiState,
            actions =
                AccountSettingsActions(
                    onBicycleToggle = { newValue ->
                        isBikeAvailable = newValue
                        teBiciViewModel.updateBikeAvailability(newValue)
                    },
                    onEditUsername = { screenState = "EDIT_USERNAME" },
                    onEditTitol = { screenState = "EDIT_TITOL" },
                    onBackClick = onBackClick,
                    onShowDeleteDialog = onShowDeleteDialog,
                    onLogoutClick = { showLogoutDialog = true },
                    onScreenStateChange = { screenState = it },
                    onDisplayNameChange = { currentDisplayName = it },
                    onTitolChange = { currentTitol = it },
                ),
        )
    }

    if (showLogoutDialog) {
        logoutDialog(
            onDismiss = { showLogoutDialog = false },
            onConfirm = {
                showLogoutDialog = false
                onLogout()
            },
        )
    }
}

@Composable
private fun logoutDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_logout_dialog_title)) },
        text = { Text(stringResource(R.string.settings_logout_dialog_text)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
            ) {
                Text(stringResource(R.string.settings_logout_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_logout_cancel))
            }
        },
    )
}

@Composable
private fun accountSettingsNavigation(
    screenState: String,
    state: AccountSettingsState,
    uiState: ProfileUiState,
    actions: AccountSettingsActions,
) {
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
    ) { currentScreen ->
        when (currentScreen) {
            "MAIN" -> {
                accountSettingsContent(
                    state = state,
                    uiState = uiState,
                    actions = actions,
                )
            }
            "EDIT_USERNAME" -> {
                editUsernameScreen(
                    displayName = state.displayName,
                    onBackClick = { actions.onScreenStateChange?.invoke("MAIN") },
                    onSave = { actions.onDisplayNameChange?.invoke(it) },
                )
            }
            "EDIT_TITOL" -> {
                editTitolScreen(
                    currentTitol = state.titol,
                    onBackClick = { actions.onScreenStateChange?.invoke("MAIN") },
                    onSave = { actions.onTitolChange?.invoke(it) },
                )
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

data class AccountSettingsActions(
    val onBicycleToggle: (Boolean) -> Unit,
    val onEditUsername: () -> Unit,
    val onEditTitol: () -> Unit,
    val onBackClick: () -> Unit,
    val onShowDeleteDialog: () -> Unit,
    val onLogoutClick: () -> Unit,
    val onScreenStateChange: ((String) -> Unit)? = null,
    val onDisplayNameChange: ((String) -> Unit)? = null,
    val onTitolChange: ((String) -> Unit)? = null,
)

// Maneja los efectos secundarios relacionados con la actualizacin de la disponibilidad de bicicleta,
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
                viewModel.resetUiState()
            }
            is TeBiciUiState.Error -> {
                Toast.makeText(context, uiState.message.asString(context), Toast.LENGTH_LONG).show()
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
    state: AccountSettingsState,
    uiState: ProfileUiState,
    actions: AccountSettingsActions,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        accountSettingsHeader(onBackClick = actions.onBackClick)
        HorizontalDivider(thickness = DividerDefaults.Thickness, color = DividerDefaults.color)
        usernameRow(displayName = state.displayName, onEditUsername = actions.onEditUsername)
        HorizontalDivider(thickness = DividerDefaults.Thickness, color = DividerDefaults.color)
        titolRow(titol = state.titol, onEditTitol = actions.onEditTitol)
        HorizontalDivider(thickness = DividerDefaults.Thickness, color = DividerDefaults.color)
        bicycleAvailableRow(
            isEnabled = state.isEnabled,
            isLoading = state.isLoadingBike,
            onBicycleToggle = actions.onBicycleToggle,
        )
        HorizontalDivider(thickness = DividerDefaults.Thickness, color = DividerDefaults.color)

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            onClick = actions.onLogoutClick,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors =
                ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
        ) {
            Text(stringResource(R.string.settings_logout))
        }

        deleteAccountSection(
            uiState = uiState,
            onDeleteClicked = actions.onShowDeleteDialog,
        )
    }
}

@Composable
private fun accountSettingsHeader(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.acc_back_button),
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(id = R.string.account_settings_header),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun usernameRow(displayName: String, onEditUsername: () -> Unit) {
    androidx.compose.material3.Surface(
        onClick = onEditUsername,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.account_settings_username_label),
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
                contentDescription = stringResource(R.string.acc_edit_icon),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
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
    androidx.compose.material3.Surface(
        onClick = onEditTitol,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
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
                text = stringResource(R.string.account_settings_titles_label),
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
                contentDescription = stringResource(R.string.acc_edit_icon),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
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
            text = stringResource(R.string.account_settings_bike_available),
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
