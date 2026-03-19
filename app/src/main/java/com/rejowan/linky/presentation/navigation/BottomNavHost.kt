package com.rejowan.linky.presentation.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.rejowan.linky.presentation.feature.collections.CollectionsScreen
import com.rejowan.linky.presentation.feature.home.HomeScreen
import com.rejowan.linky.presentation.feature.settings.SettingsScreen
import com.rejowan.linky.presentation.feature.tools.ToolsScreen

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
                onAddLinkClick = { collectionId ->
                    // Navigate to AddEditLink with pre-selected collection
                    parentNavController.navigate(Route.AddEditLink(collectionId = collectionId))
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

        // ============ TOOLS SCREEN ============
        composable<Route.Tools> {
            ToolsScreen(
                onNavigateToBatchImport = {
                    parentNavController.navigate(Route.BatchImport())
                },
                onNavigateToVault = {
                    parentNavController.navigate(Route.VaultUnlock)
                },
                onNavigateToTrash = {
                    parentNavController.navigate(Route.Trash)
                },
                onNavigateToImportExport = {
                    parentNavController.navigate(Route.ImportExport)
                },
                onNavigateToDuplicateDetection = {
                    parentNavController.navigate(Route.DuplicateDetection)
                },
                onNavigateToLinkHealthCheck = {
                    parentNavController.navigate(Route.LinkHealthCheck)
                }
            )
        }

        // ============ SETTINGS SCREEN ============
        composable<Route.Settings> {
            SettingsScreen(
                onNavigateToDangerZone = {
                    parentNavController.navigate(Route.DangerZone)
                }
            )
        }
    }
}