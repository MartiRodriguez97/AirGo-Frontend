package com.airgo.airgoApp.features.shell

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresExtension
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.airgo.airgoApp.R
import com.airgo.airgoApp.features.activity.activityScreen
import com.airgo.airgoApp.features.calendar.calendarScreen
import com.airgo.airgoApp.features.calendar.domain.viewmodel.CalendarViewModel
import com.airgo.airgoApp.features.home.activityDetailScreen
import com.airgo.airgoApp.features.home.homeScreen
import com.airgo.airgoApp.features.home.planDetailsScreen
import com.airgo.airgoApp.features.home.questionnaireScreen
import com.airgo.airgoApp.features.map.favoritesScreen
import com.airgo.airgoApp.features.profile.profileScreen
import com.airgo.airgoApp.features.social.chat.chatScreen
import com.airgo.airgoApp.features.social.friends.friendProfileScreen
import com.airgo.airgoApp.features.social.socialScreen
import com.airgo.airgoApp.features.tracking.domain.TrackingViewModel
import com.airgo.airgoApp.features.tracking.trackingSummaryScreen

enum class MainShellScreen(
    val label: Int,
    val icon: ImageVector,
    val route: String,
) {
    HOME(label = R.string.nav_home, icon = Icons.Outlined.Home, route = "home"),
    SOCIAL(label = R.string.nav_social, icon = Icons.Outlined.People, route = "social"),
    ACTIVITY(label = R.string.nav_activity, icon = Icons.AutoMirrored.Outlined.DirectionsRun, route = "activity"),
    CALENDAR(label = R.string.nav_calendar, icon = Icons.Outlined.CalendarMonth, route = "calendar"),
    PROFILE(label = R.string.nav_profile, icon = Icons.Outlined.Person, route = "profile"),
}

@RequiresApi(Build.VERSION_CODES.O)
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
@Composable
fun mainShellScreen(
    email: String?,
    pictureUrl: String?,
    onLogout: () -> Unit,
    pendingNavData: android.os.Bundle? = null,
    onNavigated: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Manejar la navegación pendiente de la notificación
    androidx.compose.runtime.LaunchedEffect(pendingNavData) {
        if (pendingNavData != null) {
            val convId = pendingNavData.getString("convId")
            val otherUserId = pendingNavData.getString("otherUserId")
            val username = pendingNavData.getString("username") ?: "Usuari"
            val otherProfilePic = pendingNavData.getString("otherProfilePic")
            
            if (convId != null && otherUserId != null) {
                val encodedPic = if (!otherProfilePic.isNullOrEmpty()) {
                    java.net.URLEncoder.encode(otherProfilePic, "UTF-8")
                } else "none"

                // Primero navegamos a SOCIAL para que quede en el backstack
                navController.navigate(MainShellScreen.SOCIAL.route) {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
                // Luego navegamos al chat concreto con todos los parámetros
                navController.navigate("chat/$convId/$otherUserId/$username/$encodedPic") {
                    launchSingleTop = true
                }
                onNavigated() // Limpiamos el estado en MainActivity
            }
        }
    }

    Scaffold(
        bottomBar = {
            bottomNavigationBar(
                currentRoute = currentRoute,
                navController = navController,
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MainShellScreen.HOME.route,
            modifier = Modifier.fillMaxSize(),
        ) {
            homeNavGraph(navController, innerPadding)
            featuresNavGraph(navController, innerPadding, email, pictureUrl, onLogout)
        }
    }
}

@Composable
fun screenWrapper(
    innerPadding: PaddingValues,
    content: @Composable () -> Unit,
) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        content()
    }
}

@Composable
private fun bottomNavigationBar(
    currentRoute: String?,
    navController: NavHostController,
) {
    val hiddenRoutes = setOf("questionnaire", "favorites")
    val isFriendProfile = currentRoute.orEmpty().startsWith("friend_profile")
    val isChatScreen = currentRoute.orEmpty().startsWith("chat")
    val isPlanDetails = currentRoute.orEmpty().startsWith("planDetails")
    val isActivityDetail = currentRoute == "activityDetail"
    val isTrackingSummary = currentRoute == "tracking_summary"
    val showBar =
        currentRoute !in hiddenRoutes &&
            !isFriendProfile &&
            !isChatScreen &&
            !isPlanDetails &&
            !isActivityDetail &&
            !isTrackingSummary

    if (showBar) {
        NavigationBar {
            MainShellScreen.entries.forEach { tab ->
                NavigationBarItem(
                    selected = currentRoute == tab.route,
                    onClick = {
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = {
                        Icon(imageVector = tab.icon, contentDescription = stringResource(tab.label))
                    },
                    label = {
                        Text(
                            text = stringResource(tab.label),
                            maxLines = 1,
                            softWrap = false,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                )
            }
        }
    }
}

private fun NavGraphBuilder.homeNavGraph(
    navController: NavHostController,
    innerPadding: PaddingValues,
) {
    composable(MainShellScreen.HOME.route) {
        screenWrapper(innerPadding) {
            homeScreen(
                onNavigateToQuestionnaire = { navController.navigate("questionnaire") },
                onNavigateToDetails = { planId -> navController.navigate("planDetails/$planId") },
                onNavigateToPendingActivity = { navController.navigate("activityDetail") },
            )
        }
    }
    composable("planDetails/{planId}") { backStackEntry ->
        val planId = backStackEntry.arguments?.getString("planId")?.toIntOrNull() ?: -1
        screenWrapper(innerPadding) {
            planDetailsScreen(planId = planId, onBack = { navController.popBackStack() })
        }
    }
    composable("questionnaire") {
        screenWrapper(innerPadding) {
            questionnaireScreen(onClose = { navController.popBackStack() })
        }
    }
    composable("activityDetail") {
        screenWrapper(innerPadding) {
            activityDetailScreen(
                onBackClick = { navController.popBackStack() },
                onStartActivityClick = {
                    navController.navigate(MainShellScreen.ACTIVITY.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }
    }
}

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
@RequiresApi(Build.VERSION_CODES.O)
private fun NavGraphBuilder.featuresNavGraph(
    navController: NavHostController,
    innerPadding: PaddingValues,
    email: String?,
    pictureUrl: String?,
    onLogout: () -> Unit,
) {
    composable(MainShellScreen.SOCIAL.route) {
        screenWrapper(innerPadding) {
            socialScreen(
                onFriendClick = { userId -> navController.navigate("friend_profile/$userId") },
                onChatClick = { convId, otherUserId, username, profilePic ->
                    val encodedPic = if (!profilePic.isNullOrEmpty()) {
                        java.net.URLEncoder.encode(profilePic, "UTF-8")
                    } else "none"
                    navController.navigate("chat/$convId/$otherUserId/$username/$encodedPic")
                },
            )
        }
    }
    composable("friend_profile/{userId}") { backStackEntry ->
        val userId = backStackEntry.arguments?.getString("userId")?.toIntOrNull() ?: return@composable
        screenWrapper(innerPadding) {
            friendProfileScreen(userId = userId, onBackClick = { navController.popBackStack() })
        }
    }
    composable(MainShellScreen.ACTIVITY.route) {
        activityScreen(navController = navController)
    }
    composable(MainShellScreen.CALENDAR.route) {
        screenWrapper(innerPadding) {
            val calendarViewModel: CalendarViewModel = viewModel(factory = CalendarViewModel.Factory)
            calendarScreen(viewModel = calendarViewModel)
        }
    }
    composable(MainShellScreen.PROFILE.route) {
        screenWrapper(innerPadding) {
            profileScreen(
                email = email,
                pictureUrl = pictureUrl,
                onAccountDeleted = onLogout,
                onLogout = onLogout,
            )
        }
    }
    composable("favorites") {
        favoritesScreen(onBack = { navController.popBackStack() })
    }
    composable("tracking_summary") {
        val activityEntry =
            remember(it) {
                navController.getBackStackEntry(MainShellScreen.ACTIVITY.route)
            }
        val summaryViewModel: TrackingViewModel = viewModel(activityEntry)
        trackingSummaryScreen(
            onBack = { navController.popBackStack() },
            onSave = { navController.popBackStack() },
            viewModel = summaryViewModel,
        )
    }
    composable("chat/{convId}/{otherUserId}/{username}/{otherProfilePic}") { backStackEntry ->
        val convId = backStackEntry.arguments?.getString("convId") ?: return@composable
        val otherUserId = backStackEntry.arguments?.getString("otherUserId")?.toIntOrNull() ?: return@composable
        val username = backStackEntry.arguments?.getString("username") ?: ""
        val rawPic = backStackEntry.arguments?.getString("otherProfilePic")
        val otherProfilePic = if (rawPic == "none") null else rawPic

        chatScreen(
            conversationId = convId,
            otherUserId = otherUserId,
            otherUsername = username,
            otherProfilePic = otherProfilePic,
            onBack = { navController.popBackStack() },
        )
    }
}
