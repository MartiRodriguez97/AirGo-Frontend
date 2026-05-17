package com.airgo.airgoApp.features.profile

import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.airgo.airgoApp.R
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.features.profile.components.badgesAndActivitiesZone
import com.airgo.airgoApp.features.profile.components.deleteAccountDialog
import com.airgo.airgoApp.features.profile.components.deleteAccountSection
import com.airgo.airgoApp.features.profile.components.profileUserInfoCard
import com.airgo.airgoApp.features.profile.domain.ProfileUiState
import com.airgo.airgoApp.features.profile.domain.ProfileViewModel
import com.airgo.airgoApp.features.settings.account.accountSettingsScreen
import com.airgo.airgoApp.features.settings.settingsScreen

enum class ProfileNestedRoute(
    val route: String,
) {
    HOME("profile_home"),
    SETTINGS("settings"),
    ACCOUNT_SETTINGS("account_settings"),
    ACTIVITY_DETAIL("activity_detail/{activityId}"),
}

data class ProfileScreen(
    val displayName: String,
    val email: String?,
    val titol: String?,
    val currentPictureUrl: String?,
    val initial: String,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun profileScreen(
    email: String?,
    pictureUrl: String?,
    viewModel: ProfileViewModel = viewModel(),
    onAccountDeleted: () -> Unit,
) {
    val profileNavController = rememberNavController()
    // Observe back stack to trigger recompositions on return
    profileNavController.currentBackStackEntryAsState().value

    var showDeleteDialog by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()

    // Dynamic values from SessionManager or arguments
    val currentPictureUrl = SessionManager.profilePictureUrl ?: pictureUrl
    val context = LocalContext.current
    val displayName =
        SessionManager.username
            ?: email?.substringBefore('@')
            ?: "Usuario"
    val titol = SessionManager.titol ?: ""
    val initial = displayName.firstOrNull()?.toString() ?: "U"

    val profileInfo =
        ProfileScreen(
            displayName = displayName,
            email = email,
            titol = titol,
            currentPictureUrl = currentPictureUrl,
            initial = initial,
        )

    val singlePhotoPickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
            onResult = { uri ->
                if (uri != null) {
                    viewModel.updateProfilePicture(context, uri.toString())
                }
            },
        )

    profileScreenLaunchedEffect(
        uiState = uiState,
        context = context,
        onAccountDeleted = onAccountDeleted,
        onResetUiState = { viewModel.resetUiState() },
        onPictureUpdateSuccess = { /* Automatically updated via recomposition */ },
    )

    Box(modifier = Modifier.fillMaxSize()) {
        profileNavGraph(
            navController = profileNavController,
            profileInfo = profileInfo,
            uiState = uiState,
            singlePhotoPickerLauncher = singlePhotoPickerLauncher,
            onShowDeleteDialog = { showDeleteDialog = true },
        )
    }

    profileDeleteDialog(
        showDialog = showDeleteDialog,
        onDismiss = { showDeleteDialog = false },
        onConfirm = {
            showDeleteDialog = false
            viewModel.deleteAccount()
        },
    )
}

@Composable
private fun profileScreenLaunchedEffect(
    uiState: ProfileUiState,
    context: android.content.Context,
    onAccountDeleted: () -> Unit,
    onResetUiState: () -> Unit,
    onPictureUpdateSuccess: () -> Unit,
) {
    LaunchedEffect(uiState) {
        when (uiState) {
            is ProfileUiState.DeleteSuccess -> onAccountDeleted()
            is ProfileUiState.PictureUpdateSuccess -> {
                onPictureUpdateSuccess()
                Toast
                    .makeText(
                        context,
                        context.getString(R.string.profile_picture_update_success),
                        Toast.LENGTH_SHORT,
                    ).show()
                onResetUiState() // Resetear el estado tras mostrar el mensaje
            }
            is ProfileUiState.Error -> {
                Toast.makeText(context, uiState.message.asString(context), Toast.LENGTH_LONG).show()
                onResetUiState()
            }
            else -> {}
        }
    }
}

@Composable
private fun profileDeleteDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (showDialog) {
        deleteAccountDialog(
            onDismiss = onDismiss,
            onConfirm = onConfirm,
        )
    }
}

@Composable
fun showProfileInfo(
    profileInfo: ProfileScreen,
    uiState: ProfileUiState,
    singlePhotoPickerLauncher: ActivityResultLauncher<PickVisualMediaRequest>,
    onDeleteClicked: () -> Unit,
    onSettingsClick: () -> Unit,
    navController: NavHostController,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        showProfileInfoContent(
            profileInfo = profileInfo,
            uiState = uiState,
            singlePhotoPickerLauncher = singlePhotoPickerLauncher,
            onDeleteClicked = onDeleteClicked,
            navController = navController,
        )

        showProfileInfoSettingsButton(onSettingsClick = onSettingsClick)
    }
}

@Composable
private fun showProfileInfoContent(
    profileInfo: ProfileScreen,
    uiState: ProfileUiState,
    singlePhotoPickerLauncher: ActivityResultLauncher<PickVisualMediaRequest>,
    onDeleteClicked: () -> Unit,
    navController: NavHostController,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        profileUserInfoCard(
            displayName = profileInfo.displayName,
            titol = profileInfo.titol,
            pictureUrl = profileInfo.currentPictureUrl,
            initial = profileInfo.initial,
            isUpdatingPicture = uiState is ProfileUiState.PictureUpdateLoading,
            onEditProfilePictureClick = {
                singlePhotoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
        )
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 16.dp),
            thickness = DividerDefaults.Thickness,
            color = DividerDefaults.color,
        )
        badgesAndActivitiesZone(
            onActivityClick = { id ->
                navController.navigate("activity_detail/$id")
            },
        )

        Spacer(modifier = Modifier.height(32.dp))

        deleteAccountSection(
            uiState = uiState,
            onDeleteClicked = onDeleteClicked,
        )
    }
}

@Composable
private fun showProfileInfoSettingsButton(onSettingsClick: () -> Unit) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(top = 8.dp, end = 8.dp),
    ) {
        IconButton(
            onClick = onSettingsClick,
            modifier =
                Modifier
                    .align(Alignment.TopEnd),
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "Settings",
            )
        }
    }
}

@Composable
fun profileNavGraph(
    navController: NavHostController,
    profileInfo: ProfileScreen,
    uiState: ProfileUiState,
    singlePhotoPickerLauncher: ManagedActivityResultLauncher<PickVisualMediaRequest, android.net.Uri?>,
    onShowDeleteDialog: () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = ProfileNestedRoute.HOME.route,
    ) {
        // main profile view
        composable(
            route = ProfileNestedRoute.HOME.route,
            enterTransition = { slideInHorizontally { -it } + fadeIn() },
            exitTransition = { slideOutHorizontally { -it } + fadeOut() },
        ) {
            showProfileInfo(
                profileInfo = profileInfo,
                uiState = uiState,
                singlePhotoPickerLauncher = singlePhotoPickerLauncher,
                onDeleteClicked = onShowDeleteDialog,
                onSettingsClick = {
                    navController.navigate(ProfileNestedRoute.SETTINGS.route)
                },
                navController = navController,
            )
        }
        // Settings main menu
        composable(
            route = ProfileNestedRoute.SETTINGS.route,
            enterTransition = { slideInHorizontally { it } + fadeIn() },
            exitTransition = { slideOutHorizontally { -it } + fadeOut() },
            popEnterTransition = { slideInHorizontally { -it } + fadeIn() },
            popExitTransition = { slideOutHorizontally { it } + fadeOut() },
        ) {
            settingsScreen(
                email = profileInfo.email,
                displayName = profileInfo.displayName,
                pictureUrl = profileInfo.currentPictureUrl,
                initial = profileInfo.initial,
                onBackClick = { navController.popBackStack() },
                onAccountSettingsClick = {
                    // Navigate to Account Settings
                    navController.navigate(ProfileNestedRoute.ACCOUNT_SETTINGS.route)
                },
            )
        }
        // account settings
        composable(
            route = ProfileNestedRoute.ACCOUNT_SETTINGS.route,
            enterTransition = { slideInHorizontally { it } + fadeIn() },
            popExitTransition = { slideOutHorizontally { it } + fadeOut() },
        ) {
            accountSettingsScreen(
                displayName = profileInfo.displayName,
                onBackClick = { navController.popBackStack() }, // Go back
            )
        }

        // activitat detallada
        composable(
            route = ProfileNestedRoute.ACTIVITY_DETAIL.route,
            arguments =
                listOf(
                    androidx.navigation.navArgument(
                        "activityId",
                    ) { type = androidx.navigation.NavType.IntType },
                ),
            enterTransition = { slideInHorizontally { it } + fadeIn() },
            exitTransition = { slideOutHorizontally { -it } + fadeOut() },
            popEnterTransition = { slideInHorizontally { -it } + fadeIn() },
            popExitTransition = { slideOutHorizontally { it } + fadeOut() },
        ) { backStackEntry ->
            val activityId = backStackEntry.arguments?.getInt("activityId") ?: -1
            // Obtenemos el viewmodel que tiene la lista cargada
            val activityViewModel: com.airgo.airgoApp.features.activity.domain.ActivityViewModel = viewModel()

            pastActivityDetailScreen(
                activityId = activityId,
                activityViewModel = activityViewModel,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
