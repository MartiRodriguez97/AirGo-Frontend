package com.airgo.airgoApp.features.settings.account.titols

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import com.airgo.airgoApp.features.settings.account.titols.data.UserTitol
import com.airgo.airgoApp.features.settings.account.titols.domain.TitolUiState
import com.airgo.airgoApp.features.settings.account.titols.domain.TitolViewModel

@Composable
fun editTitolScreen(
    currentTitol: String,
    onBackClick: () -> Unit,
    onSave: (String) -> Unit,
    viewModel: TitolViewModel = viewModel(),
) {
    var titol by remember { mutableStateOf(currentTitol) }
    val uiState by viewModel.uiState.collectAsState()
    val availableTitols by viewModel.availableTitols.collectAsState()
    val loadError by viewModel.loadError.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is TitolUiState.Success -> {
                Toast.makeText(context, "Título actualizado correctamente", Toast.LENGTH_SHORT).show()
                onSave(titol)
                viewModel.resetUiState()
                onBackClick()
            }
            is TitolUiState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                viewModel.resetUiState()
            }
            else -> {}
        }
    }

    BackHandler {
        onBackClick()
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        editTitolHeader(onBackClick)

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 16.dp),
            thickness = DividerDefaults.Thickness,
            color = DividerDefaults.color,
        )

        editTitolForm(
            titol = titol,
            availableTitols = availableTitols,
            loadError = loadError,
            onTitolChange = { titol = it },
            currentTitol = currentTitol,
            isLoading = uiState is TitolUiState.Loading,
            onSave = {
                viewModel.changeTitol(titol)
            },
        )
    }
}

@Composable
private fun editTitolHeader(onBackClick: () -> Unit) {
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
            text = "Títulos",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun editTitolForm(
    titol: String,
    availableTitols: List<UserTitol>,
    loadError: String?,
    onTitolChange: (String) -> Unit,
    currentTitol: String,
    isLoading: Boolean,
    onSave: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
    ) {
        Text(
            text = "Títols desbloquejats",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (loadError != null) {
            Text(
                text = loadError,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
            )
        } else if (availableTitols.isEmpty()) {
            Text(
                text = "No hi ha títols disponibles",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            availableTitols.forEach { availableTitol ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { onTitolChange(availableTitol.nom) }
                            .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = titol == availableTitol.nom,
                        onClick = { onTitolChange(availableTitol.nom) },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = availableTitol.nom,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = availableTitol.descripcio,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            enabled = titol.isNotBlank() && titol != currentTitol && !isLoading,
        ) {
            Text(text = if (isLoading) "Guardando..." else "Guardar")
        }
    }
}
