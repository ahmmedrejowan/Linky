package com.rejowan.linky.presentation.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.rejowan.linky.presentation.feature.addlink.AddEditLinkScreen
import com.rejowan.linky.presentation.feature.collectiondetail.CollectionDetailScreen
import com.rejowan.linky.presentation.feature.linkdetail.LinkDetailScreen
import com.rejowan.linky.presentation.feature.snapshotviewer.SnapshotViewerScreen

/**
 * Parent navigation host for the Linky app
 * Contains all app-level navigation EXCEPT the bottom nav screens
 * Bottom nav screens (Home/Collections/Settings) are in BottomNavHost within MainScreen
 *
 * @param navController The NavHostController for app-level navigation
 * @param snackbarHostState Shared SnackbarHostState from MainActivity
 * @param modifier Modifier for the NavHost
 * @param isAuthRequired Whether authentication is required (Phase 2)
 */
@Composable
fun LinkyNavHost(
    navController: NavHostController,
    isAuthRequired: Boolean = false // Phase 2: checks preferences
) {
    val startDestination: Route = if (isAuthRequired) Route.Welcome else Route.Main

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        // ============ AUTH GRAPH (Phase 2) ============
        // TODO: Uncomment and implement in Phase 2
        /*
        if (isAuthRequired) {
            composable<Route.Welcome> {
                WelcomeScreen(
                    onContinueOffline = {
                        navController.navigate(Route.Main) {
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
                        navController.navigate(Route.Main) {
                            popUpTo<Route.Welcome> { inclusive = true }
                        }
                    }
                )
            }

            composable<Route.SyncSetup> {
                SyncSetupScreen(
                    onComplete = {
                        navController.navigate(Route.Main) {
                            popUpTo<Route.Welcome> { inclusive = true }
                        }
                    }
                )
            }
        }
        */

        // ============ MAIN SCREEN (Container for Bottom Nav) ============
        composable<Route.Main> {
            MainScreen(
                parentNavController = navController,
            )
        }

        // ============ APP-LEVEL SCREENS ============

        composable<Route.LinkDetail> { backStackEntry ->
            val linkDetail = backStackEntry.toRoute<Route.LinkDetail>()

            LinkDetailScreen(
                linkId = linkDetail.linkId,
                onNavigateBack = { navController.popBackStack() },
                onEditClick = { linkId ->
                    navController.navigate(Route.AddEditLink(linkId))
                },
                onOpenSnapshot = { snapshotId ->
                    navController.navigate(Route.SnapshotViewer(snapshotId))
                },
                onNavigateToCollection = { collectionId ->
                    navController.navigate(Route.CollectionDetail(collectionId))
                }
            )
        }

        composable<Route.SnapshotViewer> { backStackEntry ->
            val snapshotViewer = backStackEntry.toRoute<Route.SnapshotViewer>()

            SnapshotViewerScreen(
                snapshotId = snapshotViewer.snapshotId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<Route.AddEditLink> { backStackEntry ->
            val addEditLink = backStackEntry.toRoute<Route.AddEditLink>()

            AddEditLinkScreen(
                linkId = addEditLink.linkId, // null for new link
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<Route.CollectionDetail> { backStackEntry ->
            val collectionDetail = backStackEntry.toRoute<Route.CollectionDetail>()

            CollectionDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onLinkClick = { linkId ->
                    navController.navigate(Route.LinkDetail(linkId))
                },
                onFavoriteClick = { linkId ->
                    // TODO: Implement toggle favorite for link from collection detail
                },
                onAddLinkClick = { collectionId ->
                    navController.navigate(Route.AddEditLink(collectionId = collectionId))
                }
            )
        }

        composable<Route.AdvancedSettings> {
            // TODO: Implement AdvancedSettingsScreen in Phase 3/4
            // AdvancedSettingsScreen(
            //     onNavigateBack = { navController.popBackStack() }
            // )
        }
    }
}
