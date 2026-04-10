package com.airgo.airgoApp.features.settings.account.titols

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun editTitolScreen(
    currentTitol: String,
    onBackClick: () -> Unit,
    onSave: (String) -> Unit,
) {
    var titol by remember { mutableStateOf(currentTitol) }

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
            onTitolChange = { titol = it },
            currentTitol = currentTitol,
            onSave = {
                onSave(titol)
                onBackClick()
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
    onTitolChange: (String) -> Unit,
    currentTitol: String,
    onSave: () -> Unit,
) {
    val mockTitols = listOf("Velocista nato", "Ciclista urbano", "Resistencia al limite", "Cuenta veterana")

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

        mockTitols.forEach { mockTitol ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onTitolChange(mockTitol) }
                        .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = titol == mockTitol,
                    onClick = { onTitolChange(mockTitol) },
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = mockTitol,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text =
                "Aquest títol és temporal i visual (mockup). " +
                    "Properament s'implementarà amb els valors del servidor.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            enabled = titol.isNotBlank() && titol != currentTitol,
        ) {
            Text(text = "Guardar")
        }
    }
}
