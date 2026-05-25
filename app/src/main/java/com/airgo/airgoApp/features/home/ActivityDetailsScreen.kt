package com.airgo.airgoApp.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airgo.airgoApp.R
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.features.home.helpers.TemplateMapper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun activityDetailScreen(onBackClick: () -> Unit) {
    val exercici = SessionManager.exerciciActiu
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.activity_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.acc_back_button),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            if (exercici != null) {
                val templateCode = exercici.template?.nom ?: ""

                val titleResId =
                    if (templateCode.isNotEmpty()) {
                        TemplateMapper.getTitleResId(templateCode)
                    } else {
                        R.string.home_pending_activity_card
                    }

                val descResId =
                    if (templateCode.isNotEmpty()) {
                        TemplateMapper.getDescResId(templateCode)
                    } else {
                        R.string.template_default_desc
                    }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(id = titleResId),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(id = descResId),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(40.dp))
            } else {
                Spacer(modifier = Modifier.height(32.dp))
                Text(stringResource(R.string.no_activity_data))
            }
        }
    }
}
