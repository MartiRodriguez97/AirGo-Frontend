package com.airgo.airgoApp.features.social.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.airgo.airgoApp.R

@Composable
internal fun chatInputBar(
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf("") }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = {
                    Text(
                        stringResource(R.string.message),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                },
                modifier = Modifier.weight(1f),
                maxLines = 5,
                shape = RoundedCornerShape(24.dp),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
            )
            Spacer(modifier = Modifier.width(8.dp))
            sendButton(text = text, onSend = {
                onSend(text)
                text = ""
            })
        }
    }
}

@Composable
private fun sendButton(
    text: String,
    onSend: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (text.isNotBlank()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                ),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = { if (text.isNotBlank()) onSend() }, enabled = text.isNotBlank()) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = stringResource(R.string.send),
                tint =
                    if (text.isNotBlank()) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
