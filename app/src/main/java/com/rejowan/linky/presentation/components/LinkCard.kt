package com.rejowan.linky.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
 * Supports selection mode for bulk operations
 *
 * @param link The link to display
 * @param onClick Callback when card is clicked
 * @param onFavoriteClick Callback when favorite icon is clicked
 * @param onMoreClick Callback when more icon is clicked (shows info sheet)
 * @param tags Optional list of tags associated with the link
 * @param isSelectionMode Whether selection mode is active
 * @param isSelected Whether this link is selected
 * @param onLongPress Callback when card is long-pressed (enters selection mode)
 * @param onToggleSelection Callback when selection is toggled
 * @param modifier Modifier for styling
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LinkCard(
    link: Link,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onMoreClick: () -> Unit,
    tags: List<Tag> = emptyList(),
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onLongPress: (() -> Unit)? = null,
    onToggleSelection: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                    onClick = {
                        if (isSelectionMode) {
                            onToggleSelection?.invoke()
                        } else {
                            onClick()
                        }
                    },
                    onLongClick = {
                        onLongPress?.invoke()
                    }
                )
                .then(
                    if (isSelected) {
                        Modifier.border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(12.dp)
                        )
                    } else {
                        Modifier
                    }
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection indicator (shown in selection mode)
            if (isSelectionMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = if (isSelected) "Selected" else "Not selected",
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
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

            // Action Icons (Favorite + More)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Favorite Icon
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (link.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (link.isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (link.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // More Icon
                IconButton(
                    onClick = onMoreClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
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

/**
 * LinkGridCard component - Grid view variant of LinkCard
 * Displays link as a vertical card with image on top, title, URL below
 * Supports selection mode for bulk operations
 *
 * @param link The link to display
 * @param onClick Callback when card is clicked
 * @param onFavoriteClick Callback when favorite icon is clicked
 * @param onMoreClick Callback when more icon is clicked (shows info sheet)
 * @param isSelectionMode Whether selection mode is active
 * @param isSelected Whether this link is selected
 * @param onLongPress Callback when card is long-pressed (enters selection mode)
 * @param onToggleSelection Callback when selection is toggled
 * @param modifier Modifier for styling
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LinkGridCard(
    link: Link,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onMoreClick: () -> Unit,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onLongPress: (() -> Unit)? = null,
    onToggleSelection: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onToggleSelection?.invoke()
                    } else {
                        onClick()
                    }
                },
                onLongClick = {
                    onLongPress?.invoke()
                }
            )
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Modifier
                }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            // Preview Image with Status Badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                if (link.previewImagePath != null || link.previewUrl != null) {
                    AsyncImage(
                        model = link.previewImagePath ?: link.previewUrl,
                        contentDescription = "Preview for ${link.title}",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Placeholder when no preview
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "No preview",
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Top-left: Favorite icon (clickable)
                if (!isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .size(28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                            .clickable { onFavoriteClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (link.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (link.isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (link.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Top-right: More icon or Selection indicator
                if (isSelectionMode) {
                    // Selection indicator overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                            contentDescription = if (isSelected) "Selected" else "Not selected",
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    // More icon (clickable)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                            .clickable { onMoreClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Status Banner (Deleted/Archived) - at bottom of image
                if (link.isDeleted || link.isArchived) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(
                                if (link.isDeleted) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                                else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (link.isDeleted) "Deleted" else "Archived",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (link.isDeleted) MaterialTheme.colorScheme.onErrorContainer
                            else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // Content
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Title
                Text(
                    text = link.title,
                    style = MaterialTheme.typography.titleSmall,
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

                // Timestamp
                Text(
                    text = if (link.isDeleted) {
                        val daysLeft = calculateDaysUntilAutoDelete(link.deletedAt ?: 0)
                        "$daysLeft days left"
                    } else {
                        link.createdAt.toRelativeTime()
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (link.isDeleted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
