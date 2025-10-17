package com.rejowan.linky.presentation.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.rejowan.linky.presentation.addlink.AddEditLinkScreen
import com.rejowan.linky.presentation.collections.CollectionsScreen
import com.rejowan.linky.presentation.home.HomeScreen
import com.rejowan.linky.presentation.linkdetail.LinkDetailScreen
import com.rejowan.linky.presentation.settings.SettingsScreen

/**
 * Main navigation host for the Linky app
 * Uses type-safe navigation with Kotlin Serialization
 *
 * @param navController The NavHostController for navigation
 * @param snackbarHostState Shared SnackbarHostState from MainActivity
 * @param onCreateFolderClick Callback to set the create folder action for FAB
 * @param modifier Modifier for the NavHost
 * @param isAuthRequired Whether authentication is required (Phase 2)
 * @param startDestination The starting route
 */
@Composable
fun LinkyNavHost(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    onCreateFolderClick: (() -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    isAuthRequired: Boolean = false, // Phase 2: checks preferences
    startDestination: Route = if (isAuthRequired) Route.Welcome else Route.Home
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // ============ AUTH GRAPH (Phase 2) ============
        // TODO: Uncomment and implement in Phase 2
        /*
        if (isAuthRequired) {
            composable<Route.Welcome> {
                WelcomeScreen(
                    onContinueOffline = {
                        navController.navigate(Route.Home) {
                            popUpTo<Route.Welcome> { inclusive = true }
                        }
                    },
                    onLoginClick = {
                        navController.navigate(Route.Login)
                    }
                )
            }

            composable<Route.Login> {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Route.SyncSetup) {
                            popUpTo<Route.Welcome> { inclusive = true }
                        }
                    },
                    onSkip = {
                        navController.navigate(Route.Home) {
                            popUpTo<Route.Welcome> { inclusive = true }
                        }
                    }
                )
            }

            composable<Route.SyncSetup> {
                SyncSetupScreen(
                    onComplete = {
                        navController.navigate(Route.Home) {
                            popUpTo<Route.Welcome> { inclusive = true }
                        }
                    }
                )
            }
        }
        */

        // ============ HOME GRAPH ============
        composable<Route.Home> {
            HomeScreen(
                onAddLinkClick = {
                    navController.navigate(Route.AddEditLink())
                },
                onLinkClick = { linkId ->
                    navController.navigate(Route.LinkDetail(linkId))
                },
                onNavigateToCollections = {
                    navController.navigate(Route.Collections)
                },
                onNavigateToSettings = {
                    navController.navigate(Route.Settings)
                }
            )
        }

        composable<Route.LinkDetail> { backStackEntry ->
            // Type-safe argument retrieval
            val linkDetail = backStackEntry.toRoute<Route.LinkDetail>()

            LinkDetailScreen(
                linkId = linkDetail.linkId,
                onNavigateBack = { navController.popBackStack() },
                onEditClick = { linkId ->
                    navController.navigate(Route.AddEditLink(linkId))
                },
                onOpenSnapshot = { snapshotId ->
                    navController.navigate(Route.SnapshotViewer(snapshotId))
                }
            )
        }

        composable<Route.SnapshotViewer> { backStackEntry ->
            val snapshotViewer = backStackEntry.toRoute<Route.SnapshotViewer>()

            // TODO: Implement SnapshotViewerScreen in Phase 1
            // SnapshotViewerScreen(
            //     snapshotId = snapshotViewer.snapshotId,
            //     onNavigateBack = { navController.popBackStack() }
            // )
        }

        // ============ ADD/EDIT LINK (Global Modal) ============
        composable<Route.AddEditLink> { backStackEntry ->
            val addEditLink = backStackEntry.toRoute<Route.AddEditLink>()

            AddEditLinkScreen(
                linkId = addEditLink.linkId, // null for new link
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ============ COLLECTIONS GRAPH ============
        composable<Route.Collections> {
            CollectionsScreen(
                snackbarHostState = snackbarHostState,
                onCreateFolderClick = onCreateFolderClick,
                onFolderClick = { folderId ->
                    // Phase 3: Navigate to folder detail
                    navController.navigate(Route.FolderDetail(folderId))
                },
                onNavigateToHome = {
                    navController.navigate(Route.Home)
                },
                onNavigateToSettings = {
                    navController.navigate(Route.Settings)
                }
            )
        }

        composable<Route.FolderDetail> { backStackEntry ->
            // Phase 3
            val folderDetail = backStackEntry.toRoute<Route.FolderDetail>()

            // TODO: Implement FolderDetailScreen in Phase 3
            // FolderDetailScreen(
            //     folderId = folderDetail.folderId,
            //     onNavigateBack = { navController.popBackStack() },
            //     onLinkClick = { linkId ->
            //         navController.navigate(Route.LinkDetail(linkId))
            //     }
            // )
        }

        // ============ SETTINGS GRAPH ============
        composable<Route.Settings> {
            SettingsScreen(
                snackbarHostState = snackbarHostState,
                onNavigateToAdvanced = {
                    // Phase 3/4
                    navController.navigate(Route.AdvancedSettings)
                },
                onNavigateToHome = {
                    navController.navigate(Route.Home)
                },
                onNavigateToCollections = {
                    navController.navigate(Route.Collections)
                },
                onLogout = {
                    // Phase 2: Navigate to welcome after logout
                    navController.navigate(Route.Welcome) {
                        popUpTo(Route.Home) { inclusive = true }
                    }
                }
            )
        }

        composable<Route.AdvancedSettings> {
            // Phase 3/4
            // TODO: Implement AdvancedSettingsScreen in Phase 3/4
            // AdvancedSettingsScreen(
            //     onNavigateBack = { navController.popBackStack() }
            // )
        }
    }
}
