package com.airgo.airgoApp.features.login.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airgo.airgoApp.R
import com.airgo.airgoApp.features.login.domain.LoginUiState
import com.airgo.airgoApp.features.login.domain.LoginViewModel

@Composable
fun loginScreen(onLoginSuccess: (email: String?, pictureUrl: String?) -> Unit = { _, _ -> }) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        login(
            modifier = Modifier.align(Alignment.Center),
            onLoginSuccess = onLoginSuccess,
        )
    }
}

@Composable
fun login(
    modifier: Modifier,
    onLoginSuccess: (email: String?, pictureUrl: String?) -> Unit,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        headerImage()
        Spacer(modifier = Modifier.height(16.dp))
        googleLoginButton(onLoginSuccess = onLoginSuccess)
    }
}

@Composable
fun googleLoginButton(
    viewModel: LoginViewModel = viewModel(),
    onLoginSuccess: (email: String?, pictureUrl: String?) -> Unit,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val currentState = uiState
    if (uiState is LoginUiState.Success) {
        LaunchedEffect(uiState) {
            onLoginSuccess(
                (uiState as LoginUiState.Success).email,
                (uiState as LoginUiState.Success).picture,
            )
            viewModel.resetState()
        }
    }

    when (uiState) {
        is LoginUiState.Loading -> CircularProgressIndicator()
        else -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(onClick = { viewModel.onGoogleSignIn(context) }) {
                    Text(text = stringResource(R.string.login_button_google))
                }
                if (currentState is LoginUiState.Error) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = currentState.message.asString(),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
fun headerImage() {
    Image(
        painter = painterResource(id = R.drawable.airgo_logo),
        contentDescription = stringResource(R.string.login_header_description),
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun loginScreenPreview() {
    MaterialTheme {
        loginScreen(onLoginSuccess = { _, _ -> })
    }
}
