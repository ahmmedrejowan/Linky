package com.rejowan.linky.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.rejowan.linky.domain.model.Link
import com.rejowan.linky.domain.model.Tag
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * LinkCard component - Displays a link with preview image, title, URL, and actions
 * Supports swipe gestures: swipe right to archive, swipe left to trash
 *
 * @param link The link to display
 * @param onClick Callback when card is clicked
 * @param onFavoriteClick Callback when favorite icon is clicked
 * @param onArchiveClick Callback when link is archived via swipe
 * @param onTrashClick Callback when link is moved to trash via swipe
 * @param tags Optional list of tags associated with the link
 * @param modifier Modifier for styling
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkCard(
    link: Link,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onArchiveClick: (() -> Unit)? = null,
    onTrashClick: (() -> Unit)? = null,
    tags: List<Tag> = emptyList(),
    modifier: Modifier = Modifier
) {
    // Disable swipe for deleted links
    val swipeEnabled = !link.isDeleted

    // Track pending action to execute when user releases finger
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            // Store the action when threshold is met, but don't execute yet
            // Return false to prevent auto-dismiss and make it bounce back
            // Action executes when user releases (detected in LaunchedEffect)
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    // User swiped past threshold for trash
                    if (swipeEnabled && !link.isDeleted) {
                        pendingAction = onTrashClick
                    }
                    false // Don't auto-dismiss, bounce back
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    // User swiped past threshold for archive
                    if (swipeEnabled && !link.isDeleted) {
                        pendingAction = onArchiveClick
                    }
                    false // Don't auto-dismiss, bounce back
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        },
        // Require 70% swipe distance to trigger action (prevents accidental swipes)
        positionalThreshold = { distance -> distance * 0.7f }
    )

    // Execute pending action when user releases and swipe settles back
    LaunchedEffect(dismissState.currentValue, pendingAction) {
        // When swipe bounces back to Settled and we have a pending action
        if (dismissState.currentValue == SwipeToDismissBoxValue.Settled && pendingAction != null) {
            // User has released finger - execute the action now
            pendingAction?.invoke()
            pendingAction = null
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = swipeEnabled,
        enableDismissFromEndToStart = swipeEnabled,
        backgroundContent = {
            if (swipeEnabled) {
                // Calculate alpha based on swipe progress for better visual feedback
                // Progress ranges from 0.0 (no swipe) to 1.0 (full swipe)
                val swipeProgress = dismissState.progress
                // Scale alpha from 0.3 to 1.0 as user swipes
                val backgroundAlpha = (0.3f + (swipeProgress * 0.7f)).coerceIn(0.3f, 1.0f)

                val color = when (dismissState.dismissDirection) {
                    SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.secondaryContainer
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                    SwipeToDismissBoxValue.Settled -> MaterialTheme.colorScheme.surface
                }
                val icon = when (dismissState.dismissDirection) {
                    SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Archive
                    SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                    SwipeToDismissBoxValue.Settled -> null
                }
                val alignment = when (dismissState.dismissDirection) {
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    SwipeToDismissBoxValue.Settled -> Alignment.Center
                }
                // Text label for swipe action
                val actionText = when (dismissState.dismissDirection) {
                    SwipeToDismissBoxValue.StartToEnd -> if (link.isArchived) "Unarchive" else "Archive"
                    SwipeToDismissBoxValue.EndToStart -> "Trash"
                    SwipeToDismissBoxValue.Settled -> null
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color.copy(alpha = backgroundAlpha), shape = RoundedCornerShape(12.dp))
                        .padding(horizontal = 24.dp),
                    contentAlignment = alignment
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        icon?.let {
                            Icon(
                                imageVector = it,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = when (dismissState.dismissDirection) {
                                    SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.onSecondaryContainer
                                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.onErrorContainer
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                        actionText?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelLarge,
                                color = when (dismissState.dismissDirection) {
                                    SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.onSecondaryContainer
                                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.onErrorContainer
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Preview Image with Status Badge Overlay
            Box(
                modifier = Modifier.size(72.dp)
            ) {
                if (link.previewImagePath != null || link.previewUrl != null) {
                    AsyncImage(
                        model = link.previewImagePath ?: link.previewUrl,
                        contentDescription = "Preview for ${link.title}",
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Placeholder when no preview
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "No preview",
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Status Banner Overlay (Deleted takes priority over Archived)
                if (link.isDeleted || link.isArchived) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    ) {
                        // Show only Deleted badge if deleted (deleted overrides archived)
                        if (link.isDeleted) {
                            StatusBanner(
                                text = "Deleted",
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                labelColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        } else if (link.isArchived) {
                            // Only show Archived if NOT deleted
                            StatusBanner(
                                text = "Archived",
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // Content (Title, URL, Timestamp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Title
                Text(
                    text = link.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // URL
                Text(
                    text = link.url.shortenUrl(),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Tags Row (if any)
                if (tags.isNotEmpty()) {
                    TagChipRow(
                        tags = tags,
                        maxRows = 1
                    )
                }

                // Timestamp or Deletion Countdown
                Text(
                    text = if (link.isDeleted) {
                        val daysLeft = calculateDaysUntilAutoDelete(link.deletedAt ?: 0)
                        "$daysLeft days until auto-deletion"
                    } else {
                        link.createdAt.toRelativeTime()
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (link.isDeleted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                )
            }

            // Favorite Icon
            IconButton(
                onClick = onFavoriteClick
            ) {
                Icon(
                    imageVector = if (link.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (link.isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (link.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    }
}

/**
 * Shorten URL for display (remove protocol, limit length)
 */
private fun String.shortenUrl(): String {
    return this
        .removePrefix("https://")
        .removePrefix("http://")
        .removePrefix("www.")
        .take(40)
        .let { if (this.length > 40) "$it..." else it }
}

/**
 * Convert timestamp to relative time string
 */
private fun Long.toRelativeTime(): String {
    val now = System.currentTimeMillis()
    val diff = now - this

    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        diff < 604800_000 -> "${diff / 86400_000}d ago"
        diff < 2592000_000 -> "${diff / 604800_000}w ago"
        else -> {
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            sdf.format(Date(this))
        }
    }
}

/**
 * Status banner for overlay on preview image
 * Full-width banner at the top of the image with centered text
 */
@Composable
private fun StatusBanner(
    text: String,
    containerColor: androidx.compose.ui.graphics.Color,
    labelColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = labelColor,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .background(color = containerColor.copy(alpha = 0.92f))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    )
}

/**
 * Calculate days remaining until auto-deletion (30 days from deletedAt)
 */
private fun calculateDaysUntilAutoDelete(deletedAt: Long): Int {
    val now = System.currentTimeMillis()
    val deletionTime = deletedAt + (30 * 24 * 60 * 60 * 1000L) // 30 days in milliseconds
    val remainingTime = deletionTime - now
    val daysLeft = (remainingTime / (24 * 60 * 60 * 1000L)).toInt()
    return maxOf(0, daysLeft) // Ensure it's never negative
}
