package com.airgo.airgoApp.features.profile.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.airgo.airgoApp.R
import com.airgo.airgoApp.features.profile.domain.ProfileUiState

@Composable
fun deleteAccountSection(
    uiState: ProfileUiState,
    onDeleteClicked: () -> Unit,
) {
    val state = uiState
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (state is ProfileUiState.Error) {
            Text(
                text = state.message.asString(),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        if (state is ProfileUiState.Loading) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        } else {
            deleteAccountButton(onDeleteClicked = onDeleteClicked)
        }
    }
}

@Composable
fun deleteAccountButton(onDeleteClicked: () -> Unit) {
    OutlinedButton(
        onClick = onDeleteClicked,
        colors =
            ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
        border =
            ButtonDefaults.outlinedButtonBorder.copy(
                brush =
                    androidx.compose.ui.graphics
                        .SolidColor(MaterialTheme.colorScheme.error),
            ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.profile_delete_account_button))
    }
}

@Composable
fun deleteAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.profile_delete_account_dialog_title))
        },
        text = {
            Text(text = stringResource(R.string.profile_delete_account_dialog_text))
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
            ) {
                Text(stringResource(R.string.profile_delete_account_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.profile_delete_account_cancel))
            }
        },
    )
}
