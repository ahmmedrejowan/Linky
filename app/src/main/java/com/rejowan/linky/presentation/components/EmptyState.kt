package com.rejowan.linky.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * EmptyState component - Displays when there's no data to show
 *
 * @param icon The icon to display
 * @param title The main title text
 * @param message The descriptive message
 * @param actionText Optional action button text
 * @param onActionClick Optional action button callback
 * @param modifier Modifier for styling
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.outlineVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Title
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Message
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        // Action Button (optional)
        if (actionText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onActionClick) {
                Text(text = actionText)
            }
        }
    }
}

/**
 * Predefined empty states for common scenarios
 */
object EmptyStates {
    @Composable
    fun NoLinks(
        onAddLinkClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        EmptyState(
            icon = Icons.Outlined.LinkOff,
            title = "No Links Yet",
            message = "Start saving your favorite links and they'll appear here",
            actionText = "Add Your First Link",
            onActionClick = onAddLinkClick,
            modifier = modifier
        )
    }

    @Composable
    fun NoFolders(
        onCreateFolderClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        EmptyState(
            icon = Icons.Outlined.FolderOff,
            title = "No Folders Yet",
            message = "Create folders to organize your links",
            actionText = "Create Folder",
            onActionClick = onCreateFolderClick,
            modifier = modifier
        )
    }

    @Composable
    fun NoSearchResults(
        searchQuery: String,
        modifier: Modifier = Modifier
    ) {
        EmptyState(
            icon = Icons.Outlined.SearchOff,
            title = "No Results Found",
            message = "No links match \"$searchQuery\". Try a different search term.",
            modifier = modifier
        )
    }

    @Composable
    fun NoFavorites(
        modifier: Modifier = Modifier
    ) {
        EmptyState(
            icon = Icons.Outlined.LinkOff,
            title = "No Favorites",
            message = "Mark links as favorites to see them here",
            modifier = modifier
        )
    }

    @Composable
    fun NoArchivedLinks(
        modifier: Modifier = Modifier
    ) {
        EmptyState(
            icon = Icons.Outlined.LinkOff,
            title = "No Archived Links",
            message = "Archived links will appear here",
            modifier = modifier
        )
    }

    @Composable
    fun NoTrashedLinks(
        modifier: Modifier = Modifier
    ) {
        EmptyState(
            icon = Icons.Outlined.LinkOff,
            title = "Trash is Empty",
            message = "Deleted links will appear here",
            modifier = modifier
        )
    }
}
