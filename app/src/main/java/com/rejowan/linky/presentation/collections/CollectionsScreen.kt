package com.rejowan.linky.presentation.collections

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
 * Collections Screen
 * Shows all folders for organizing links
 * TODO: Implement full screen with folder list, create folder dialog, and folder management
 *
 * @param onFolderClick Callback when a folder is clicked
 * @param onNavigateToHome Callback to navigate to home
 * @param onNavigateToSettings Callback to navigate to settings
 */
@Composable
fun CollectionsScreen(
    onFolderClick: (String) -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToSettings: () -> Unit,
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
            text = "Collections Screen",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Placeholder - Coming Soon",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
