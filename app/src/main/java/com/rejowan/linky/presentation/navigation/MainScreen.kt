package com.rejowan.linky.presentation.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.rejowan.linky.presentation.components.AnimatedBottomNav
import com.rejowan.linky.presentation.components.NavItem
import timber.log.Timber

/**
 * Main screen - Container for the bottom navigation experience
 * Uses Box layout with AnimatedBottomNav placed outside Scaffold for cutout effect
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
    val snackbarHostState = remember { SnackbarHostState() }

    // Selected nav index
    var selectedNavIndex by rememberSaveable { mutableIntStateOf(initialTab ?: 0) }

    // Navigate to initial tab if specified
    LaunchedEffect(initialTab) {
        if (initialTab != null && initialTab != selectedNavIndex) {
            selectedNavIndex = initialTab
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

    // State for Collections screen FAB action
    var onCreateCollectionClick by remember { mutableStateOf<(() -> Unit)?>(null) }

    // State for exit confirmation dialog
    var showExitDialog by remember { mutableStateOf(false) }

    // State for batch import bottom sheet (smart intent handling)
    var showBatchImportBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Track when share intent was last handled to suppress clipboard checking temporarily
    var lastShareIntentHandledTime by remember { mutableStateOf(0L) }

    // Handle shared content from other apps with smart intent detection
    LaunchedEffect(sharedContent) {
        if (sharedContent != null) {
            lastShareIntentHandledTime = System.currentTimeMillis()
            when {
                sharedContent.hasNoUrls -> {
                    Timber.d("No URLs in shared content")
                    onSharedContentHandled()
                }
                sharedContent.hasSingleUrl -> {
                    Timber.d("Single URL detected: ${sharedContent.firstUrl}")
                    parentNavController.navigate(Route.AddEditLink(
                        url = sharedContent.firstUrl,
                        title = sharedContent.title
                    ))
                    onSharedContentHandled()
                }
                sharedContent.hasMultipleUrls -> {
                    Timber.d("Multiple URLs detected: ${sharedContent.urlCount} URLs")
                    showBatchImportBottomSheet = true
                }
            }
        }
    }

    // State for selection mode (hides bottom nav)
    var isSelectionMode by remember { mutableStateOf(false) }

    // Bottom bar visibility with animation
    var isBottomBarVisible by remember { mutableStateOf(true) }
    val bottomBarHeight = 120.dp
    val bottomBarOffset by animateDpAsState(
        targetValue = if (isBottomBarVisible && !isSelectionMode) 0.dp else bottomBarHeight,
        animationSpec = tween(durationMillis = 300),
        label = "bottomBarOffset"
    )

    // Get current route for FAB logic
    val currentRoute: Route? = when (selectedNavIndex) {
        0 -> Route.Home
        1 -> Route.Collections
        2 -> Route.Settings
        else -> null
    }

    // Determine FAB icon and content description based on current route
    val fabIcon = when (currentRoute) {
        is Route.Collections -> Icons.Default.CreateNewFolder
        else -> Icons.Default.Add
    }
    val fabContentDescription = when (currentRoute) {
        is Route.Collections -> "Create collection"
        else -> "Add link"
    }

    // Main layout: Box with Scaffold inside, AnimatedBottomNav outside
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                // Only show FAB on Home and Collections screens
                if (currentRoute != Route.Settings && !isSelectionMode) {
                    FloatingActionButton(
                        onClick = {
                            when (currentRoute) {
                                is Route.Home -> {
                                    parentNavController.navigate(Route.AddEditLink())
                                }
                                is Route.Collections -> {
                                    onCreateCollectionClick?.invoke()
                                }
                                else -> {
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
            },
            // Keep status bar inset, but content bleeds behind bottom bar for cutout effect
            contentWindowInsets = WindowInsets.statusBars
        ) { paddingValues ->
            // Apply top padding for status bar - content goes behind bottom nav
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                BottomNavHost(
                    navController = bottomNavController,
                    parentNavController = parentNavController,
                    snackbarHostState = snackbarHostState,
                    lastShareIntentHandledTime = lastShareIntentHandledTime,
                    onCreateCollectionClick = { callback ->
                        onCreateCollectionClick = callback
                    },
                    onSearchClick = {
                        parentNavController.navigate(Route.Search)
                    },
                    onSelectionModeChange = { isSelectionMode = it },
                    onExitRequest = { showExitDialog = true }
                )
            }
        }

        // AnimatedBottomNav placed outside Scaffold at the bottom
        AnimatedBottomNav(
            selectedIndex = selectedNavIndex,
            onItemClick = { index ->
                selectedNavIndex = index
                isBottomBarVisible = true // Show bottom bar when switching tabs
                val route = when (index) {
                    0 -> Route.Home
                    1 -> Route.Collections
                    2 -> Route.Settings
                    else -> return@AnimatedBottomNav
                }
                bottomNavController.navigate(route) {
                    popUpTo(Route.Home) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = bottomBarOffset)
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
                parentNavController.navigate(Route.AddEditLink(
                    url = sharedContent.firstUrl,
                    title = sharedContent.title
                ))
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
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

            Text(
                text = "Are you sure you want to exit the app?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

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
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.CenterHorizontally),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Multiple Links Detected",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Text(
                text = "Found $urlCount links in the shared content. How would you like to proceed?",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

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

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
