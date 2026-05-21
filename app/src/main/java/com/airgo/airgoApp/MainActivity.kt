package com.airgo.airgoApp

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresExtension
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airgo.airgoApp.core.network.RetrofitProvider
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.core.network.UserPreferences
import com.airgo.airgoApp.core.network.UserPreferencesRepository
import com.airgo.airgoApp.core.network.dataStore
import com.airgo.airgoApp.core.theme.airGoTheme
import com.airgo.airgoApp.features.login.data.auth.UserDetailResponse
import com.airgo.airgoApp.features.login.ui.loginScreen
import com.airgo.airgoApp.features.onboarding.domain.OnboardingViewModel
import com.airgo.airgoApp.features.onboarding.onboardingScreen
import com.airgo.airgoApp.features.shell.mainShellScreen
import kotlinx.coroutines.launch
import java.io.IOException

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            airGoApp()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
@Composable
fun airGoApp() {
    val context = LocalContext.current
    val repository = remember { UserPreferencesRepository(context.dataStore) }

    airGoTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            appContent(repository)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
@Composable
private fun appContent(repository: UserPreferencesRepository) {
    val userPrefs by repository.userPreferencesFlow.collectAsState(initial = null)
    val onboardingViewModel: OnboardingViewModel = viewModel()
    val coroutineScope = rememberCoroutineScope()

    var isLoggedIn by rememberSaveable { mutableStateOf(false) }
    var userEmail by rememberSaveable { mutableStateOf<String?>(null) }
    var userPictureUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var needsOnboarding by rememberSaveable { mutableStateOf(false) }
    var isDataStoreLoaded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(userPrefs) {
        val prefs = userPrefs
        if (prefs != null && (!isLoggedIn || SessionManager.accessToken == null)) {
            handleAutoLogin(
                prefs,
                onboardingViewModel,
                repository,
            ) { email, pic, logged, onboarding ->
                userEmail = email
                userPictureUrl = pic
                isLoggedIn = logged
                needsOnboarding = onboarding
            }
        }
        isDataStoreLoaded = true
    }

    appNavigation(
        state =
            AppNavigationState(
                isDataStoreLoaded = isDataStoreLoaded,
                isLoggedIn = isLoggedIn,
                needsOnboarding = needsOnboarding,
                userEmail = userEmail,
                userPictureUrl = userPictureUrl,
            ),
        onboardingViewModel = onboardingViewModel,
        onLoginSuccess = { email, pic ->
            userEmail = email
            userPictureUrl = pic
            isLoggedIn = true
            onboardingViewModel.checkOnboardingNeeded()
        },
        onOnboardingComplete = {
            needsOnboarding = false
            coroutineScope.launch {
                repository.saveSession(SessionManager.toUserPreferences())
            }
        },
        onLogout = {
            coroutineScope.launch { repository.clearSession() }
            SessionManager.logout()
            isLoggedIn = false
        },
    )
}

private data class AppNavigationState(
    val isDataStoreLoaded: Boolean,
    val isLoggedIn: Boolean,
    val needsOnboarding: Boolean,
    val userEmail: String?,
    val userPictureUrl: String?,
)

@RequiresApi(Build.VERSION_CODES.O)
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
@Composable
private fun appNavigation(
    state: AppNavigationState,
    onboardingViewModel: OnboardingViewModel,
    onLoginSuccess: (String?, String?) -> Unit,
    onOnboardingComplete: () -> Unit,
    onLogout: () -> Unit,
) {
    when {
        !state.isDataStoreLoaded -> Box(modifier = Modifier.fillMaxSize())
        !state.isLoggedIn -> loginScreen(onLoginSuccess)
        state.needsOnboarding || onboardingViewModel.uiState.isHandledByOnboarding -> {
            onboardingScreen(onCompleted = onOnboardingComplete)
        }
        else ->
            mainShellScreen(
                email = state.userEmail,
                pictureUrl = state.userPictureUrl,
                onLogout = onLogout,
            )
    }
}

private const val HTTP_ERROR_401 = 401
private const val HTTP_ERROR_403 = 403

private suspend fun handleAutoLogin(
    userPrefs: UserPreferences,
    onboardingViewModel: OnboardingViewModel,
    repository: UserPreferencesRepository,
    onSync: (String?, String?, Boolean, Boolean) -> Unit,
) {
    if (userPrefs.token.isNullOrEmpty()) return

    SessionManager.accessToken = userPrefs.token
    SessionManager.refreshToken = userPrefs.refreshToken
    SessionManager.userId = userPrefs.userId

    try {
        val response = RetrofitProvider.authApi.getCurrentUserProfile()
        if (response.isSuccessful && response.body() != null) {
            updateUserSession(response.body()!!, onboardingViewModel, onSync)
            fetchUserBadges()
        } else if (response.code() == HTTP_ERROR_401 || response.code() == HTTP_ERROR_403) {
            repository.clearSession()
            onSync(null, null, false, false)
        }
    } catch (e: IOException) {
        Log.e("MainActivity", "Error during auto login", e)
    }
}

private suspend fun fetchUserBadges() {
    try {
        SessionManager.userBadges = RetrofitProvider.profileApi.getUserBadges()
    } catch (e: retrofit2.HttpException) {
        Log.e("MainActivity", "Error fetching user badges (HTTP)", e)
    } catch (e: IOException) {
        Log.e("MainActivity", "Error fetching user badges (IO)", e)
    }
}

private fun updateUserSession(
    userDetails: UserDetailResponse,
    onboardingViewModel: OnboardingViewModel,
    onSyncComplete: (String?, String?, Boolean, Boolean) -> Unit,
) {
    with(SessionManager) {
        userId = userDetails.id
        username = userDetails.username
        profilePictureUrl = userDetails.profilePic
        punts = userDetails.punts
        teBici = userDetails.teBici
        pes = userDetails.pes
        altura = userDetails.altura
        ratxa = userDetails.ratxa
        idioma = userDetails.idioma
        limitRutes = userDetails.limitRutes
        titol = userDetails.titol
        insignies = userDetails.insignies
    }

    val needsOnboarding = SessionManager.isProfileIncomplete()
    if (needsOnboarding) onboardingViewModel.checkOnboardingNeeded()

    onSyncComplete(SessionManager.username, userDetails.profilePic, true, needsOnboarding)
}
