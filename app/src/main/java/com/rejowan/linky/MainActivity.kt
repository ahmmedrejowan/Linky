package com.rejowan.linky

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.rejowan.linky.presentation.navigation.BottomNavItem
import com.rejowan.linky.presentation.navigation.LinkyNavHost
import com.rejowan.linky.presentation.navigation.Route
import com.rejowan.linky.presentation.navigation.components.BottomNavigationBar
import com.rejowan.linky.ui.theme.LinkyTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()
        setContent {
            LinkyTheme {
                val navController = rememberNavController()
                val currentBackStackEntry by navController.currentBackStackEntryAsState()
                val snackbarHostState = remember { SnackbarHostState() }

                // State for Collections screen FAB action
                var onCreateFolderClick by remember { mutableStateOf<(() -> Unit)?>(null) }

                // Get current route for bottom nav selection and FAB logic
                val currentRoute = try {
                    currentBackStackEntry?.toRoute<Route>()
                } catch (_: Exception) {
                    null
                }

                // Determine if bottom bar should be visible
                val showBottomBar = currentRoute is Route.Home ||
                                  currentRoute is Route.Collections ||
                                  currentRoute is Route.Settings

                // Determine FAB visibility and action
                val showFAB = currentRoute is Route.Home || currentRoute is Route.Collections
                val fabIcon = when (currentRoute) {
                    is Route.Collections -> Icons.Default.CreateNewFolder
                    else -> Icons.Default.Add
                }
                val fabContentDescription = when (currentRoute) {
                    is Route.Collections -> "Create folder"
                    else -> "Add link"
                }

                // Determine TopAppBar title
                val topBarTitle = when (currentRoute) {
                    is Route.Home -> "Linky"
                    is Route.Collections -> "Collections"
                    is Route.Settings -> "Settings"
                    else -> ""
                }

                Scaffold(
                    topBar = {
                        if (showBottomBar) {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = topBarTitle,
                                        style = MaterialTheme.typography.headlineSmall
                                    )
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        if (showBottomBar) {
                            BottomNavigationBar(
                                items = BottomNavItem.entries,
                                currentRoute = currentRoute,
                                onItemClick = { item ->
                                    navController.navigate(item.route) {
                                        popUpTo(Route.Home) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    },
                    floatingActionButton = {
                        if (showFAB) {
                            FloatingActionButton(
                                onClick = {
                                    when (currentRoute) {
                                        is Route.Home -> {
                                            navController.navigate(Route.AddEditLink())
                                        }
                                        is Route.Collections -> {
                                            onCreateFolderClick?.invoke()
                                        }
                                        else -> {}
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = fabIcon,
                                    contentDescription = fabContentDescription
                                )
                            }
                        }
                    }
                ) { paddingValues ->
                    LinkyNavHost(
                        navController = navController,
                        snackbarHostState = snackbarHostState,
                        onCreateFolderClick = { callback ->
                            onCreateFolderClick = callback
                        },
                        modifier = Modifier.padding(paddingValues),
                        isAuthRequired = false // Phase 2: read from preferences
                    )
                }
            }
        }
    }
}
