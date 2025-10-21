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
                onAddLinkClick = { url ->
                    // Navigate using parent controller to AddEditLink with optional URL
                    parentNavController.navigate(Route.AddEditLink(url = url))
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
                onNavigateToDataStorage = {
                    // Navigate using parent controller to DataStorage
                    parentNavController.navigate(Route.DataStorage)
                },
                onNavigateToAppearance = {
                    // Navigate using parent controller to Appearance
                    parentNavController.navigate(Route.Appearance)
                },
                onNavigateToPrivacySecurity = {
                    // Navigate using parent controller to PrivacySecurity
                    parentNavController.navigate(Route.PrivacySecurity)
                },
                onNavigateToAbout = {
                    // Navigate using parent controller to About
                    parentNavController.navigate(Route.About)
                },
                onNavigateToSync = {
                    // Navigate using parent controller to SyncSettings
                    parentNavController.navigate(Route.SyncSettings)
                }
            )
        }
    }
}