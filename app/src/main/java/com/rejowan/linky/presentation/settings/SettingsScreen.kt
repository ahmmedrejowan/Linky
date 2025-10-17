package com.rejowan.linky.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Settings Screen
 * Shows app settings, theme toggle, storage stats, and sync options
 * TODO: Implement full screen with settings list, theme toggle, and statistics
 *
 * @param onNavigateToAdvanced Callback to navigate to advanced settings
 * @param onNavigateToHome Callback to navigate to home
 * @param onNavigateToCollections Callback to navigate to collections
 * @param onLogout Callback when logout is clicked (Phase 2)
 */
@Composable
fun SettingsScreen(
    onNavigateToAdvanced: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToCollections: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Settings Screen",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Placeholder - Coming Soon",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
