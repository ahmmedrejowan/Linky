package com.rejowan.linky.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rejowan.linky.presentation.components.BottomNavigationBar

/**
 * Main screen - Container for the bottom navigation experience
 * This screen has its own Scaffold with TopAppBar, SnackbarHost, bottom bar, and FAB
 * It contains the BottomNavHost (nested navigation for Home/Collections/Settings)
 *
 * @param parentNavController Parent NavController for navigating outside bottom nav
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    parentNavController: NavHostController,
) {
    // Local nav controller for bottom nav (nested navigation)
    val bottomNavController = rememberNavController()
    val currentBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // State for Collections screen FAB action
    var onCreateCollectionClick by remember { mutableStateOf<(() -> Unit)?>(null) }

    // Get current route for bottom nav selection and FAB logic
    // Match route by checking destination route string (contains route class name)
    val currentRoute: Route? = when {
        currentBackStackEntry == null -> null
        else -> try {
            // Match against known bottom nav routes by checking the destination route string
            when {
                currentBackStackEntry?.destination?.route?.contains("Home") == true -> Route.Home
                currentBackStackEntry?.destination?.route?.contains("Collections") == true -> Route.Collections
                currentBackStackEntry?.destination?.route?.contains("Search") == true -> Route.Search
                currentBackStackEntry?.destination?.route?.contains("Settings") == true -> Route.Settings
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    // Determine FAB icon and content description based on current route
    val fabIcon = when (currentRoute) {
        is Route.Collections -> Icons.Default.CreateNewFolder
        else -> Icons.Default.Add // Home or null
    }
    val fabContentDescription = when (currentRoute) {
        is Route.Collections -> "Create collection"
        else -> "Add link"
    }

    // Determine TopAppBar title based on current route
    val topBarTitle = when (currentRoute) {
        is Route.Home -> "Linky"
        is Route.Collections -> "Collections"
        is Route.Search -> "Search"
        is Route.Settings -> "Settings"
        null -> "Linky" // Default on app launch
        else -> ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = topBarTitle,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 22.sp
                        )
                    )
                },
                actions = {
                    IconButton(onClick = { /* TODO: Phase 2 - Account screen */ }) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Account",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
        bottomBar = {
            BottomNavigationBar(
                items = BottomNavItem.entries,
                currentRoute = currentRoute,
                onItemClick = { item ->
                    bottomNavController.navigate(item.route) {
                        // Pop up to start destination to avoid building up a large stack
                        popUpTo(Route.Home) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        },
        floatingActionButton = {
            // Only show FAB on Home and Collections screens, not on Settings or Search
            if (currentRoute != Route.Settings && currentRoute != Route.Search) {
                FloatingActionButton(
                    onClick = {
                        when (currentRoute) {
                            is Route.Home -> {
                                // Navigate to AddEditLink using parent controller
                                parentNavController.navigate(Route.AddEditLink())
                            }
                            is Route.Collections -> {
                                // Trigger create collection dialog in CollectionsScreen
                                onCreateCollectionClick?.invoke()
                            }
                            else -> {
                                // Default to Add Link (for null state on launch)
                                parentNavController.navigate(Route.AddEditLink())
                            }
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
        // BottomNavHost contains the nested navigation for Home/Collections/Settings
        BottomNavHost(
            navController = bottomNavController,
            parentNavController = parentNavController,
            snackbarHostState = snackbarHostState,
            onCreateCollectionClick = { callback ->
                onCreateCollectionClick = callback
            },
            modifier = Modifier.padding(paddingValues)
        )
    }
}