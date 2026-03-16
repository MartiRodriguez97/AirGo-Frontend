package com.airgo.airgoApp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.airgo.airgoApp.ui.login.ui.loginScreen
import com.airgo.airgoApp.ui.main.ui.mainShellScreen
import com.airgo.airgoApp.ui.theme.airGoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            airGoTheme {
                var isLoggedIn by rememberSaveable { mutableStateOf(false) }
                var userEmail by rememberSaveable { mutableStateOf<String?>(null) }
                var userPictureUrl by rememberSaveable { mutableStateOf<String?>(null) }

                if (isLoggedIn) {
                    mainShellScreen(
                        email = userEmail,
                        pictureUrl = userPictureUrl,
                    )
                } else {
                    loginScreen { email, pictureUrl ->
                        userEmail = email
                        userPictureUrl = pictureUrl
                        isLoggedIn = true
                    }
                }
            }
        }
    }
}
