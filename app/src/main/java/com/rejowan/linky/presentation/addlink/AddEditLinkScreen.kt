package com.rejowan.linky.presentation.addlink

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
 * Add/Edit Link Screen
 * Allows users to add a new link or edit an existing one
 * TODO: Implement full screen with URL input, preview, title, note, and folder selection
 *
 * @param linkId Optional link ID for edit mode. Null for add mode
 * @param onNavigateBack Callback to navigate back
 */
@Composable
fun AddEditLinkScreen(
    linkId: String?,
    onNavigateBack: () -> Unit,
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
            text = if (linkId == null) "Add Link Screen" else "Edit Link Screen",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Placeholder - Coming Soon",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        if (linkId != null) {
            Text(
                text = "Link ID: $linkId",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
