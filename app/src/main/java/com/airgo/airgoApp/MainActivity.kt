package com.airgo.airgoApp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.airgo.airgoApp.ui.login.ui.loginScreen
import com.airgo.airgoApp.ui.theme.airGoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            airGoTheme {
                loginScreen()
            }
        }
    }
}
