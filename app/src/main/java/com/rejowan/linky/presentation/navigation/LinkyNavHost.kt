package com.rejowan.linky.presentation.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.rejowan.linky.presentation.feature.addlink.AddEditLinkScreen
import com.rejowan.linky.presentation.feature.batchimport.BatchImportScreen
import com.rejowan.linky.presentation.feature.collectiondetail.CollectionDetailScreen
import com.rejowan.linky.presentation.feature.linkdetail.LinkDetailScreen
import com.rejowan.linky.presentation.feature.settings.AppFeaturesScreen
import com.rejowan.linky.presentation.feature.settings.about.AboutScreen
import com.rejowan.linky.presentation.feature.settings.appearance.AppearanceScreen
import com.rejowan.linky.presentation.feature.settings.data_storage.DataStorageScreen
import com.rejowan.linky.presentation.feature.onboarding.OnboardingScreen
import com.rejowan.linky.presentation.feature.settings.duplicates.DuplicateDetectionScreen
import com.rejowan.linky.presentation.feature.settings.healthcheck.LinkHealthCheckScreen
import com.rejowan.linky.presentation.feature.settings.privacy.PrivacySecurityScreen
import com.rejowan.linky.presentation.feature.snapshotviewer.SnapshotViewerScreen
import com.rejowan.linky.presentation.feature.trash.TrashScreen
import com.rejowan.linky.presentation.feature.vault.VaultScreen
import com.rejowan.linky.presentation.feature.vault.VaultSetupScreen
import com.rejowan.linky.presentation.feature.vault.VaultSettingsScreen
import com.rejowan.linky.presentation.feature.vault.VaultUnlockScreen

/**
 * Parent navigation host for the Linky app
 * Contains all app-level navigation EXCEPT the bottom nav screens
 * Bottom nav screens (Home/Collections/Settings) are in BottomNavHost within MainScreen
 *
 * @param navController The NavHostController for app-level navigation
 * @param showOnboarding Whether to show onboarding flow
 * @param onOnboardingComplete Callback when onboarding is completed
 * @param sharedContent Content shared from another app via ACTION_SEND intent
 * @param onSharedContentHandled Callback when shared content has been handled
 */
@Composable
fun LinkyNavHost(
    navController: NavHostController,
    showOnboarding: Boolean = false,
    onOnboardingComplete: () -> Unit = {},
    sharedContent: SharedContent? = null,
    onSharedContentHandled: () -> Unit = {}
) {
    val startDestination: Route = when {
        showOnboarding -> Route.Onboarding
        else -> Route.Main()
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        // ============ ONBOARDING ============
        composable<Route.Onboarding> {
            OnboardingScreen(
                onComplete = {
                    onOnboardingComplete()
                    navController.navigate(Route.Main()) {
                        popUpTo<Route.Onboarding> { inclusive = true }
                    }
                }
            )
        }

        // ============ MAIN SCREEN (Container for Bottom Nav) ============
        composable<Route.Main> { backStackEntry ->
            val mainRoute = backStackEntry.toRoute<Route.Main>()
            MainScreen(
                parentNavController = navController,
                initialTab = mainRoute.initialTab,
                navigateToCollectionId = mainRoute.navigateToCollectionId,
                sharedContent = sharedContent,
                onSharedContentHandled = onSharedContentHandled
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
                onFavoriteClick = { /* Handled internally via ViewModel */ },
                onAddLinkClick = { collectionId ->
                    navController.navigate(Route.AddEditLink(collectionId = collectionId))
                }
            )
        }

        composable<Route.Trash> {
            TrashScreen(
                onNavigateBack = { navController.popBackStack() },
                onLinkClick = { linkId ->
                    navController.navigate(Route.LinkDetail(linkId))
                }
            )
        }

        // ============ SETTINGS DETAIL SCREENS ============

        composable<Route.AppFeatures> {
            AppFeaturesScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<Route.DataStorage> {
            DataStorageScreen(
                onNavigateToTrash = { navController.navigate(Route.Trash) },
                onNavigateToDuplicateDetection = { navController.navigate(Route.DuplicateDetection) },
                onNavigateToHealthCheck = { navController.navigate(Route.LinkHealthCheck) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<Route.Appearance> {
            AppearanceScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<Route.PrivacySecurity> {
            PrivacySecurityScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<Route.About> {
            AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<Route.DuplicateDetection> {
            DuplicateDetectionScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<Route.LinkHealthCheck> {
            LinkHealthCheckScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<Route.BatchImport> { backStackEntry ->
            val batchImportRoute = backStackEntry.toRoute<Route.BatchImport>()
            BatchImportScreen(
                prefillText = batchImportRoute.prefillText,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHome = {
                    // Navigate to Main with Home tab (0) and clear BatchImport from back stack
                    navController.navigate(Route.Main(initialTab = 0)) {
                        popUpTo<Route.BatchImport> { inclusive = true }
                    }
                },
                onNavigateToCollectionDetail = { collectionId ->
                    // Navigate to Main with Collections tab (1) and set to navigate to collection detail
                    // This ensures the Collections tab is active when backing from collection detail
                    navController.navigate(
                        Route.Main(
                            initialTab = 1,
                            navigateToCollectionId = collectionId
                        )
                    ) {
                        popUpTo<Route.BatchImport> { inclusive = true }
                    }
                }
            )
        }

        composable<Route.AdvancedSettings> {
            // TODO: Implement AdvancedSettingsScreen in Phase 3/4
            // AdvancedSettingsScreen(
            //     onNavigateBack = { navController.popBackStack() }
            // )
        }

        // ============ VAULT SCREENS ============

        composable<Route.VaultSetup> {
            VaultSetupScreen(
                onNavigateBack = { navController.popBackStack() },
                onSetupComplete = {
                    // After setup, go to vault unlock then vault
                    navController.navigate(Route.VaultUnlock) {
                        popUpTo<Route.VaultSetup> { inclusive = true }
                    }
                }
            )
        }

        composable<Route.VaultUnlock> {
            VaultUnlockScreen(
                onNavigateBack = { navController.popBackStack() },
                onUnlockSuccess = {
                    // After unlock, go to vault
                    navController.navigate(Route.Vault) {
                        popUpTo<Route.VaultUnlock> { inclusive = true }
                    }
                },
                onNavigateToSetup = {
                    // Vault not setup yet, redirect to setup
                    navController.navigate(Route.VaultSetup) {
                        popUpTo<Route.VaultUnlock> { inclusive = true }
                    }
                }
            )
        }

        composable<Route.Vault> {
            VaultScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate(Route.VaultSettings) },
                onLocked = {
                    // When vault is locked, go back to unlock screen
                    navController.navigate(Route.VaultUnlock) {
                        popUpTo<Route.Vault> { inclusive = true }
                    }
                }
            )
        }

        composable<Route.VaultSettings> {
            VaultSettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onVaultCleared = {
                    // After clearing vault, go back to main
                    navController.navigate(Route.Main(initialTab = 2)) {
                        popUpTo<Route.VaultSettings> { inclusive = true }
                    }
                }
            )
        }
    }
}
