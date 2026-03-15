package com.rejowan.linky.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rejowan.linky.ui.theme.SoftAccents

/**
 * EmptyState component - Displays when there's no data to show
 * Modern design with floating animation and soft accent colors
 *
 * @param icon The icon to display
 * @param title The main title text
 * @param message The descriptive message
 * @param accentColor The accent color for the icon container and button
 * @param actionLabel Optional action button text
 * @param onAction Optional action button callback
 * @param modifier Modifier for styling
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    accentColor: Color? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Use theme-aware accent color, default to purple
    val resolvedAccentColor = accentColor ?: SoftAccents.Purple

    // Subtle floating animation for the icon
    val infiniteTransition = rememberInfiniteTransition(label = "empty state float")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float offset"
    )

    // Use top bias to account for header above - content appears in upper-center
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 32.dp, end = 32.dp, top = 32.dp, bottom = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Soft icon container with floating animation
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset(y = (-floatOffset).dp)
                .size(100.dp)
                .background(
                    color = resolvedAccentColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(28.dp)
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = resolvedAccentColor
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = resolvedAccentColor
                ),
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp)
            ) {
                Text(
                    text = actionLabel,
                    fontWeight = FontWeight.Medium
                )
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
            icon = Icons.Outlined.BookmarkBorder,
            title = "No Links Yet",
            message = "Start building your link collection by saving your first link",
            accentColor = SoftAccents.Blue,
            actionLabel = "Add Link",
            onAction = onAddLinkClick,
            modifier = modifier
        )
    }

    @Composable
    fun NoFavorites(
        modifier: Modifier = Modifier
    ) {
        EmptyState(
            icon = Icons.Outlined.FavoriteBorder,
            title = "No Favorites",
            message = "Tap the heart icon on any link to add it to your favorites",
            accentColor = SoftAccents.Pink,
            modifier = modifier
        )
    }

    @Composable
    fun NoCollections(
        onCreateCollectionClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        EmptyState(
            icon = Icons.Outlined.FolderOff,
            title = "No Collections Yet",
            message = "Create collections to organize your links",
            accentColor = SoftAccents.Purple,
            actionLabel = "Create Collection",
            onAction = onCreateCollectionClick,
            modifier = modifier
        )
    }

    @Composable
    fun EmptyCollection(
        onAddLinkClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        EmptyState(
            icon = Icons.Outlined.FolderOpen,
            title = "Collection is Empty",
            message = "Add links to this collection to keep them organized",
            accentColor = SoftAccents.Teal,
            actionLabel = "Add Link",
            onAction = onAddLinkClick,
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
            title = "No Results",
            message = "No links found matching \"$searchQuery\"",
            accentColor = SoftAccents.Amber,
            modifier = modifier
        )
    }

    @Composable
    fun NoTrashedLinks(
        modifier: Modifier = Modifier
    ) {
        EmptyState(
            icon = Icons.Outlined.BookmarkBorder,
            title = "Trash is Empty",
            message = "Deleted links will appear here for 30 days before permanent deletion",
            accentColor = SoftAccents.Purple,
            modifier = modifier
        )
    }
}
