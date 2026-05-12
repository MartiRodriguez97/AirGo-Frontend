package com.airgo.airgoApp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.core.network.UserPreferencesRepository
import com.airgo.airgoApp.core.network.dataStore
import com.airgo.airgoApp.core.theme.airGoTheme
import com.airgo.airgoApp.features.login.ui.loginScreen
import com.airgo.airgoApp.features.onboarding.domain.OnboardingViewModel
import com.airgo.airgoApp.features.onboarding.onboardingScreen
import com.airgo.airgoApp.features.shell.mainShellScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = androidx.compose.ui.platform.LocalContext.current
            val userPreferencesRepository = remember { UserPreferencesRepository(context.dataStore) }
            val userPrefs by userPreferencesRepository.userPreferencesFlow.collectAsState(initial = null)

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
                    val coroutineScope = rememberCoroutineScope()

                    // Variable para evitar el parpadeo inicial de la pantalla de login
                    var isDataStoreLoaded by rememberSaveable { mutableStateOf(false) }

                    // Auto-login basado en Datastore
                    androidx.compose.runtime.LaunchedEffect(userPrefs) {
                        if (userPrefs != null) {
                            if (!userPrefs?.token.isNullOrEmpty() && !isLoggedIn) {
                                // Hay un token guardado, reanudar
                                SessionManager.accessToken = userPrefs?.token
                                SessionManager.refreshToken = userPrefs?.refreshToken
                                SessionManager.username = userPrefs?.username
                                SessionManager.email = userPrefs?.email
                                SessionManager.userId = userPrefs?.userId
                                SessionManager.profilePictureUrl = userPrefs?.profilePictureUrl
                                SessionManager.titol = userPrefs?.titol
                                SessionManager.teBici = userPrefs?.teBici
                                SessionManager.pes = userPrefs?.pes
                                SessionManager.altura = userPrefs?.altura
                                SessionManager.punts = userPrefs?.punts
                                SessionManager.ratxa = userPrefs?.ratxa
                                SessionManager.idioma = userPrefs?.idioma
                                SessionManager.limitRutes = userPrefs?.limitRutes
                                SessionManager.insignies = userPrefs?.insignies

                                userEmail = userPrefs?.email // Usamos el email guardado
                                userPictureUrl = userPrefs?.profilePictureUrl // Usamos la foto guardada
                                isLoggedIn = true
                            }
                            isDataStoreLoaded = true
                        }
                    }

                    if (!isDataStoreLoaded) {
                        // Mostrar caja vacía o mantener fondo del splash mientras se lee de memoria
                        Box(
                            modifier =
                                androidx.compose.ui.Modifier
                                    .fillMaxSize(),
                        )
                    } else if (!isLoggedIn) {
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
                                coroutineScope.launch {
                                    userPreferencesRepository.clearSession()
                                }
                                com.airgo.airgoApp.core.network.SessionManager
                                    .logout()
                                isLoggedIn = false
                            },
                        )
                    }
                }
            }
        }
    }
}
