package com.rejowan.linky.presentation.components

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rejowan.linky.presentation.navigation.BottomNavItem
import com.rejowan.linky.presentation.navigation.Route
import timber.log.Timber

/**
 * Bottom navigation bar component
 * Displays navigation items for Home, Collections, and Settings
 *
 * @param items List of bottom navigation items
 * @param currentRoute Currently active route
 * @param onItemClick Callback when an item is clicked
 * @param modifier Modifier for styling
 */
@Composable
fun BottomNavigationBar(
    items: List<BottomNavItem>,
    currentRoute: Route?,
    onItemClick: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(modifier = modifier) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route
            Timber.d("BottomNavigationBar: item=${item.label}, isSelected=$isSelected")

            NavigationBarItem(
                selected = isSelected,
                onClick = { onItemClick(item) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label) }
            )
        }
    }
}
