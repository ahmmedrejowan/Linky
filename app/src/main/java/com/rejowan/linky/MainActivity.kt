package com.rejowan.linky

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.rejowan.linky.presentation.navigation.LinkyNavHost
import com.rejowan.linky.ui.theme.LinkyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()
        setContent {
            LinkyTheme {
                val navController = rememberNavController()

                LinkyNavHost(
                    navController = navController,
                    isAuthRequired = false)
            }
        }
    }
}
