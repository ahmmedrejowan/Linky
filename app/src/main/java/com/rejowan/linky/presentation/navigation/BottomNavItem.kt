package com.rejowan.linky.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Bottom navigation items with Material Icons
 * Each item has a route, selected/unselected icons, and label
 * Search is now in the top bar, not bottom nav
 */
enum class BottomNavItem(
    val route: Route,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String
) {
    /**
     * Home tab - Shows all saved links with search and filters
     */
    HOME(
        route = Route.Home,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        label = "Home"
    ),

    /**
     * Collections tab - Shows all collections for organizing links
     */
    COLLECTIONS(
        route = Route.Collections,
        selectedIcon = Icons.Filled.Folder,
        unselectedIcon = Icons.Outlined.Folder,
        label = "Collections"
    ),

    /**
     * Tools tab - Batch import, vault, trash, data management tools
     */
    TOOLS(
        route = Route.Tools,
        selectedIcon = Icons.Filled.Build,
        unselectedIcon = Icons.Outlined.Build,
        label = "Tools"
    ),

    /**
     * Settings tab - App settings, theme, sync, and preferences
     */
    SETTINGS(
        route = Route.Settings,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        label = "Settings"
    )
}
