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
import com.airgo.airgoApp.core.network.ApiEndpoints
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
    var isAutoLoggingIn by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(userPrefs) {
        val prefs = userPrefs
        if (prefs != null) {
            isDataStoreLoaded = true
            if (!isLoggedIn || SessionManager.accessToken == null) {
                if (!prefs.token.isNullOrEmpty()) {
                    isAutoLoggingIn = true
                }
                handleAutoLogin(
                    prefs,
                    onboardingViewModel,
                    repository,
                ) { email, pic, logged, onboarding ->
                    userEmail = email
                    userPictureUrl = pic
                    isLoggedIn = logged
                    needsOnboarding = onboarding
                    isAutoLoggingIn = false
                }
            }
        }
    }

    appNavigation(
        state =
            AppNavigationState(
                isDataStoreLoaded = isDataStoreLoaded,
                isLoggedIn = isLoggedIn,
                isAutoLoggingIn = isAutoLoggingIn,
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
    val isAutoLoggingIn: Boolean,
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
        !state.isDataStoreLoaded -> loadingScreen()
        !state.isLoggedIn -> loginScreen(onLoginSuccess, isLoading = state.isAutoLoggingIn)
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

@Composable
fun loadingScreen() {
    Box(
        modifier =
            androidx.compose.ui.Modifier
                .fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        androidx.compose.material3.CircularProgressIndicator()
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
            val userDetails = response.body()!!
            applyUserSession(userDetails)

            // Fetch everything before signaling login is complete
            fetchUserBadges()
            fetchPendingTitles()
            fetchAvailableTitles()
            fetchTrainingPlans()

            val needsOnboarding = SessionManager.isProfileIncomplete()
            if (needsOnboarding) onboardingViewModel.checkOnboardingNeeded()

            onSync(SessionManager.username, userDetails.profilePic, true, needsOnboarding)
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

private suspend fun fetchPendingTitles() {
    try {
        val titlesResponse = RetrofitProvider.trackingApi.getAvailableTitles()
        SessionManager.titolsPendents = titlesResponse.titolsPendents
    } catch (e: IOException) {
        Log.e("MainActivity", "IO Error fetching pending titles", e)
    } catch (e: retrofit2.HttpException) {
        Log.e("MainActivity", "HTTP Error fetching pending titles", e)
    }
}

private suspend fun fetchAvailableTitles() {
    val userId = SessionManager.userId ?: return
    try {
        val url = ApiEndpoints.getAvailableTitolsEndpoint(userId)
        val response = RetrofitProvider.titolApi.getAvailableTitols(url)
        if (response.isSuccessful && response.body() != null) {
            SessionManager.availableTitols = response.body()!!.map { it.titol }
        }
    } catch (e: IOException) {
        Log.e("MainActivity", "IO Error fetching available titles", e)
    } catch (e: retrofit2.HttpException) {
        Log.e("MainActivity", "HTTP Error fetching available titles", e)
    }
}

private suspend fun fetchTrainingPlans() {
    try {
        val repo =
            com.airgo.airgoApp.features.home.data
                .TrainingPlanRepository()
        SessionManager.trainingPlans = repo.fetchFullTrainingPlans()
    } catch (e: IOException) {
        Log.e("MainActivity", "IO Error fetching training plans", e)
    } catch (e: retrofit2.HttpException) {
        Log.e("MainActivity", "HTTP Error fetching training plans", e)
    }
}

private fun applyUserSession(userDetails: UserDetailResponse) {
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
}
