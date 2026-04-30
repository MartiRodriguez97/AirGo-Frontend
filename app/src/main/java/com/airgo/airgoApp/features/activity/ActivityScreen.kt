package com.airgo.airgoApp.features.activity

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.airgo.airgoApp.features.map.osmView

@Composable
fun activityScreen(navController: androidx.navigation.NavController) {
    Box(modifier = Modifier.fillMaxSize()) {
        osmView(
            modifier = Modifier.fillMaxSize(),
            onNavigateToFavorites = { navController.navigate("favorites") },
        )
    }
}
