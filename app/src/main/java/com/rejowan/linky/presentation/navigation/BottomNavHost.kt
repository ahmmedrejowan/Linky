package com.rejowan.linky.presentation.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.rejowan.linky.presentation.feature.collections.CollectionsScreen
import com.rejowan.linky.presentation.feature.home.HomeScreen
import com.rejowan.linky.presentation.feature.search.SearchScreen
import com.rejowan.linky.presentation.feature.settings.SettingsScreen

/**
 * Bottom navigation graph - Nested NavHost for Home/Collections/Settings only
 * This is contained within MainScreen and handles switching between bottom nav tabs
 *
 * @param navController Nested NavController for bottom nav (local to MainScreen)
 * @param parentNavController Parent NavController for navigating outside bottom nav
 * @param snackbarHostState Shared SnackbarHostState
 * @param onCreateCollectionClick Callback to set the create collection action for FAB
 */
@Composable
fun BottomNavHost(
    navController: NavHostController,
    parentNavController: NavHostController,
    snackbarHostState: SnackbarHostState,
    onCreateCollectionClick: (() -> Unit) -> Unit,
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
                onCreateCollectionClick = onCreateCollectionClick,
                onCollectionClick = { collectionId ->
                    // Navigate using parent controller to CollectionDetail
                    parentNavController.navigate(Route.CollectionDetail(collectionId))
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

        // ============ SEARCH SCREEN ============
        composable<Route.Search> {
            SearchScreen(
                onLinkClick = { linkId ->
                    // Navigate using parent controller to LinkDetail
                    parentNavController.navigate(Route.LinkDetail(linkId))
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
                onNavigateToTrash = {
                    // Navigate using parent controller to Trash
                    parentNavController.navigate(Route.Trash)
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