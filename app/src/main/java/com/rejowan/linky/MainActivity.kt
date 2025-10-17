package com.rejowan.linky

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.rejowan.linky.presentation.home.HomeTopAppBar
import com.rejowan.linky.presentation.navigation.BottomNavItem
import com.rejowan.linky.presentation.navigation.LinkyNavHost
import com.rejowan.linky.presentation.navigation.Route
import com.rejowan.linky.presentation.navigation.components.BottomNavigationBar
import com.rejowan.linky.ui.theme.LinkyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()
        setContent {
            LinkyTheme {
                val navController = rememberNavController()
                val currentBackStackEntry by navController.currentBackStackEntryAsState()

                // Determine if bottom bar should be visible
                // Only show on main tabs: Home, Collections, Settings
                val currentDestination = currentBackStackEntry?.destination
                val showBottomBar = currentDestination?.route?.let { route ->
                    route.contains("Home") ||
                    route.contains("Collections") ||
                    route.contains("Settings")
                } ?: false

                // Get current route for bottom nav selection (safe deserialization)
                val currentRoute = try {
                    currentBackStackEntry?.toRoute<Route>()
                } catch (_: Exception) {
                    null
                }

                Scaffold(
                    topBar = {
                        HomeTopAppBar(
                            onAccountClick = {
                                // Phase 2: Navigate to account/login
                            })
                    },
                    bottomBar = {
                        if (showBottomBar) {
                            BottomNavigationBar(
                                items = BottomNavItem.entries,
                                currentRoute = currentRoute,
                                onItemClick = { item ->
                                    navController.navigate(item.route) {
                                        // Pop up to the start destination to avoid large stack
                                        popUpTo(Route.Home) {
                                            saveState = true
                                        }
                                        // Avoid multiple copies of the same destination
                                        launchSingleTop = true
                                        // Restore state when reselecting a previously selected item
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    },
                    floatingActionButton = {
                        if (showBottomBar) {
                            FloatingActionButton(
                                onClick = {
                                    navController.navigate(Route.AddEditLink())
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Link"
                                )
                            }
                        }
                    }
                ) { paddingValues ->
                    LinkyNavHost(
                        navController = navController,
                        modifier = Modifier.padding(paddingValues),
                        isAuthRequired = false // Phase 2: read from preferences
                    )
                }
            }
        }
    }
}
