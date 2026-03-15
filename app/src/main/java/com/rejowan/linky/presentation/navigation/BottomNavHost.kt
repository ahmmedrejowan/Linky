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
 * @param lastShareIntentHandledTime Timestamp of when share intent was last handled (0 = never)
 * @param onCreateCollectionClick Callback to set the create collection action for FAB
 * @param onSearchClick Callback to navigate to search screen
 * @param onSelectionModeChange Callback when selection mode changes (to hide/show bottom nav)
 */
@Composable
fun BottomNavHost(
    navController: NavHostController,
    parentNavController: NavHostController,
    snackbarHostState: SnackbarHostState,
    lastShareIntentHandledTime: Long = 0L,
    onCreateCollectionClick: (() -> Unit) -> Unit,
    onSearchClick: () -> Unit = {},
    onSelectionModeChange: (Boolean) -> Unit = {},
    onExitRequest: () -> Unit = {},
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
                snackbarHostState = snackbarHostState,
                lastShareIntentHandledTime = lastShareIntentHandledTime,
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
                },
                onSearchClick = onSearchClick,
                onSelectionModeChange = onSelectionModeChange,
                onExitRequest = onExitRequest
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
                },
                snackbarHostState = snackbarHostState
            )
        }

        // ============ SETTINGS SCREEN ============
        composable<Route.Settings> {
            SettingsScreen(
                onNavigateToAppFeatures = {
                    // Navigate using parent controller to AppFeatures
                    parentNavController.navigate(Route.AppFeatures)
                },
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
                onNavigateToBatchImport = {
                    // Navigate using parent controller to BatchImport
                    parentNavController.navigate(Route.BatchImport())
                },
                onNavigateToVault = {
                    // Navigate to vault - VaultUnlock will check if setup is needed
                    parentNavController.navigate(Route.VaultUnlock)
                }
            )
        }
    }
}