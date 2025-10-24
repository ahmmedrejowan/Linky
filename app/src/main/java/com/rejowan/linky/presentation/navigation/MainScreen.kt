package com.rejowan.linky.presentation.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import timber.log.Timber

/**
 * Main screen - Container for the bottom navigation experience
 * This screen has its own Scaffold with TopAppBar, SnackbarHost, bottom bar, and FAB
 * It contains the BottomNavHost (nested navigation for Home/Collections/Settings)
 *
 * @param parentNavController Parent NavController for navigating outside bottom nav
 * @param initialTab The initial tab to navigate to (0=Home, 1=Collections, 2=Settings), null=remember last
 * @param navigateToCollectionId If set, navigates to this collection detail after opening Collections tab
 * @param sharedContent Content shared from another app via ACTION_SEND intent
 * @param onSharedContentHandled Callback when shared content has been handled
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    parentNavController: NavHostController,
    initialTab: Int? = null,
    navigateToCollectionId: String? = null,
    sharedContent: SharedContent? = null,
    onSharedContentHandled: () -> Unit = {}
) {
    // Local nav controller for bottom nav (nested navigation)
    val bottomNavController = rememberNavController()

    // Navigate to initial tab if specified
    LaunchedEffect(initialTab) {
        if (initialTab != null) {
            val route = when (initialTab) {
                0 -> Route.Home
                1 -> Route.Collections
                2 -> Route.Settings
                else -> null
            }
            if (route != null) {
                bottomNavController.navigate(route) {
                    popUpTo(bottomNavController.graph.startDestinationId) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }

    // Navigate to collection detail if specified (after tab is set)
    LaunchedEffect(navigateToCollectionId) {
        if (navigateToCollectionId != null) {
            parentNavController.navigate(Route.CollectionDetail(navigateToCollectionId))
        }
    }
    val currentBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // State for Collections screen FAB action
    var onCreateCollectionClick by remember { mutableStateOf<(() -> Unit)?>(null) }

    // State for exit confirmation dialog
    var showExitDialog by remember { mutableStateOf(false) }

    // State for batch import bottom sheet (smart intent handling)
    var showBatchImportBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Handle shared content from other apps with smart intent detection
    LaunchedEffect(sharedContent) {
        if (sharedContent != null) {
            when {
                // 0 URLs: Do nothing (already handled by showing no URLs message in MainActivity)
                sharedContent.hasNoUrls -> {
                    Timber.d("No URLs in shared content")
                    onSharedContentHandled()
                }
                // 1 URL: Navigate directly to Add Link (current behavior)
                sharedContent.hasSingleUrl -> {
                    Timber.d("Single URL detected: ${sharedContent.firstUrl}")
                    parentNavController.navigate(Route.AddEditLink(url = sharedContent.firstUrl))
                    onSharedContentHandled()
                }
                // 2+ URLs: Show bottom sheet with options
                sharedContent.hasMultipleUrls -> {
                    Timber.d("Multiple URLs detected: ${sharedContent.urlCount} URLs")
                    showBatchImportBottomSheet = true
                }
            }
        }
    }

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
                        contentDescription = fabContentDescription,
                        tint = MaterialTheme.colorScheme.onPrimary
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
            onExitRequest = { showExitDialog = true },
            modifier = Modifier.padding(paddingValues)
        )
    }

    // Exit Confirmation Bottom Sheet
    if (showExitDialog) {
        ExitConfirmationBottomSheet(
            onConfirm = { (parentNavController.context as? android.app.Activity)?.finish() },
            onDismiss = { showExitDialog = false }
        )
    }

    // Batch Import Bottom Sheet (Smart Intent Handling)
    if (showBatchImportBottomSheet && sharedContent != null) {
        BatchImportPromptBottomSheet(
            sheetState = sheetState,
            urlCount = sharedContent.urlCount,
            onBatchImport = {
                showBatchImportBottomSheet = false
                parentNavController.navigate(Route.BatchImport(prefillText = sharedContent.text))
                onSharedContentHandled()
            },
            onAddFirstLinkOnly = {
                showBatchImportBottomSheet = false
                parentNavController.navigate(Route.AddEditLink(url = sharedContent.firstUrl))
                onSharedContentHandled()
            },
            onDismiss = {
                showBatchImportBottomSheet = false
                onSharedContentHandled()
            }
        )
    }
}

/**
 * Exit Confirmation Bottom Sheet
 * Asks user to confirm before exiting the app
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExitConfirmationBottomSheet(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header with icon and title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Exit app",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Exit Linky?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Message
            Text(
                text = "Are you sure you want to exit the app?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cancel button
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                // Exit button
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Exit")
                }
            }
        }
    }
}

/**
 * Batch Import Prompt Bottom Sheet
 * Shows options for handling shared text with multiple URLs
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BatchImportPromptBottomSheet(
    sheetState: androidx.compose.material3.SheetState,
    urlCount: Int,
    onBatchImport: () -> Unit,
    onAddFirstLinkOnly: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .align(androidx.compose.ui.Alignment.CenterHorizontally),
                tint = MaterialTheme.colorScheme.primary
            )

            // Title
            Text(
                text = "Multiple Links Detected",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            // Message
            Text(
                text = "Found $urlCount links in the shared content. How would you like to proceed?",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Batch Import Button (Primary)
            Button(
                onClick = onBatchImport,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.CreateNewFolder,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Batch Import ($urlCount links)")
            }

            // Add First Link Only Button (Secondary)
            OutlinedButton(
                onClick = onAddFirstLinkOnly,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add First Link Only")
            }

            // Dismiss Text Button
            androidx.compose.material3.TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}