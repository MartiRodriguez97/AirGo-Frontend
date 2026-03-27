package com.airgo.airgoApp.features.shell

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.airgo.airgoApp.features.activity.activityScreen
import com.airgo.airgoApp.features.forum.forumScreen
import com.airgo.airgoApp.features.home.homeScreen
import com.airgo.airgoApp.features.messages.messagesScreen
import com.airgo.airgoApp.features.profile.profileScreen

enum class MainShellScreen(
    val label: String,
    val icon: ImageVector,
    val route: String,
) {
    HOME(label = "Home", icon = Icons.Outlined.Home, route = "home"),
    FORUM(label = "Forum", icon = Icons.Outlined.People, route = "forum"),
    ACTIVITY(label = "Actividad", icon = Icons.AutoMirrored.Outlined.DirectionsRun, route = "activity"),
    MESSAGES(label = "Mensajes", icon = Icons.Outlined.MailOutline, route = "messages"),
    PROFILE(label = "Perfil", icon = Icons.Outlined.Person, route = "profile"),
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
                            Icon(imageVector = tab.icon, contentDescription = tab.label)
                        },
                        label = { Text(text = tab.label) },
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
            composable(MainShellScreen.FORUM.route) {
                screenWrapper(innerPadding) { forumScreen() }
            }
            composable(MainShellScreen.ACTIVITY.route) {
                activityScreen()
            }
            composable(MainShellScreen.MESSAGES.route) {
                screenWrapper(innerPadding) { messagesScreen() }
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
