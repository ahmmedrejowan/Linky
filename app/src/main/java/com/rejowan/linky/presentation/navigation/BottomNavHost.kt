package com.rejowan.linky.presentation.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.rejowan.linky.presentation.feature.collections.CollectionsScreen
import com.rejowan.linky.presentation.feature.home.HomeScreen
import com.rejowan.linky.presentation.settings.SettingsScreen

/**
 * Bottom navigation graph - Nested NavHost for Home/Collections/Settings only
 * This is contained within MainScreen and handles switching between bottom nav tabs
 *
 * @param navController Nested NavController for bottom nav (local to MainScreen)
 * @param parentNavController Parent NavController for navigating outside bottom nav
 * @param snackbarHostState Shared SnackbarHostState
 * @param onCreateFolderClick Callback to set the create folder action for FAB
 */
@Composable
fun BottomNavHost(
    navController: NavHostController,
    parentNavController: NavHostController,
    snackbarHostState: SnackbarHostState,
    onCreateFolderClick: (() -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Route.Home,
        modifier = modifier
    ) {
        // ============ HOME SCREEN ============
        composable<Route.Home> {
            HomeScreen(
                onAddLinkClick = {
                    // Navigate using parent controller to AddEditLink
                    parentNavController.navigate(Route.AddEditLink())
                },
                onLinkClick = { linkId ->
                    // Navigate using parent controller to LinkDetail
                    parentNavController.navigate(Route.LinkDetail(linkId))
                },
                onNavigateToCollections = {
                    // Navigate within bottom nav using local controller
                    navController.navigate(Route.Collections)
                },
                onNavigateToSettings = {
                    // Navigate within bottom nav using local controller
                    navController.navigate(Route.Settings)
                }
            )
        }

        // ============ COLLECTIONS SCREEN ============
        composable<Route.Collections> {
            CollectionsScreen(
                snackbarHostState = snackbarHostState,
                onCreateFolderClick = onCreateFolderClick,
                onFolderClick = { folderId ->
                    // Navigate using parent controller to FolderDetail
                    parentNavController.navigate(Route.FolderDetail(folderId))
                },
                onNavigateToHome = {
                    // Navigate within bottom nav using local controller
                    navController.navigate(Route.Home)
                },
                onNavigateToSettings = {
                    // Navigate within bottom nav using local controller
                    navController.navigate(Route.Settings)
                }
            )
        }

        // ============ SETTINGS SCREEN ============
        composable<Route.Settings> {
            SettingsScreen(
                snackbarHostState = snackbarHostState,
                onNavigateToAdvanced = {
                    // Navigate using parent controller to AdvancedSettings
                    parentNavController.navigate(Route.AdvancedSettings)
                },
                onNavigateToHome = {
                    // Navigate within bottom nav using local controller
                    navController.navigate(Route.Home)
                },
                onNavigateToCollections = {
                    // Navigate within bottom nav using local controller
                    navController.navigate(Route.Collections)
                },
                onLogout = {
                    // Navigate using parent controller to Welcome (Phase 2)
                    parentNavController.navigate(Route.Welcome) {
                        // Clear entire back stack
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}