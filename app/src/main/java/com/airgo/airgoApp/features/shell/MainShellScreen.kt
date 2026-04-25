package com.airgo.airgoApp.features.shell

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.airgo.airgoApp.R
import com.airgo.airgoApp.features.activity.activityScreen
import com.airgo.airgoApp.features.calendar.calendarScreen
import com.airgo.airgoApp.features.calendar.domain.viewmodel.CalendarViewModel
import com.airgo.airgoApp.features.home.homeScreen
import com.airgo.airgoApp.features.profile.profileScreen
import com.airgo.airgoApp.features.social.socialScreen

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

@Composable
fun mainShellScreen(
    email: String?,
    pictureUrl: String?,
    onLogout: () -> Unit,
) {
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
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
                                softWrap = false, // Evita que rompa la palabra
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MainShellScreen.HOME.route,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(MainShellScreen.HOME.route) {
                screenWrapper(innerPadding) { homeScreen() }
            }
            composable(MainShellScreen.SOCIAL.route) {
                screenWrapper(innerPadding) { socialScreen() }
            }
            composable(MainShellScreen.ACTIVITY.route) {
                activityScreen()
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
                    )
                }
            }
        }
    }
}

/**
 * Funció auxiliar per aplicar el padding a les pantalles que el necessiten (per exemple,
 * avtivityScreen no el necessita)
 */
@Composable
fun screenWrapper(
    innerPadding: androidx.compose.foundation.layout.PaddingValues,
    content: @Composable () -> Unit,
) {
    androidx.compose.foundation.layout.Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        content()
    }
}
