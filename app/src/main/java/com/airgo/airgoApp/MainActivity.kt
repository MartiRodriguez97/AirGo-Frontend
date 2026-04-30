package com.airgo.airgoApp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airgo.airgoApp.core.theme.airGoTheme
import com.airgo.airgoApp.features.login.ui.loginScreen
import com.airgo.airgoApp.features.onboarding.domain.OnboardingViewModel
import com.airgo.airgoApp.features.onboarding.onboardingScreen
import com.airgo.airgoApp.features.shell.mainShellScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            airGoTheme {
                Surface(
                    modifier =
                        androidx.compose.ui.Modifier
                            .fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    var isLoggedIn by rememberSaveable { mutableStateOf(false) }
                    var userEmail by rememberSaveable { mutableStateOf<String?>(null) }
                    var userPictureUrl by rememberSaveable { mutableStateOf<String?>(null) }
                    var needsOnboarding by rememberSaveable { mutableStateOf(false) }
                    val onboardingViewModel: OnboardingViewModel = viewModel()

                    if (!isLoggedIn) {
                        loginScreen { email, pictureUrl ->
                            userEmail = email
                            userPictureUrl = pictureUrl
                            isLoggedIn = true
                            onboardingViewModel.checkOnboardingNeeded()
                        }
                    } else if (needsOnboarding || onboardingViewModel.uiState.isHandledByOnboarding) {
                        onboardingScreen(
                            onCompleted = { needsOnboarding = false },
                        )
                    } else {
                        mainShellScreen(
                            email = userEmail,
                            pictureUrl = userPictureUrl,
                            onLogout = {
                                isLoggedIn = false
                                userEmail = null
                                userPictureUrl = null
                                com.airgo.airgoApp.core.network.SessionManager.accessToken = null
                            },
                        )
                    }
                }
            }
        }
    }
}
