package com.rejowan.linky.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * LinkCard component - Displays a link with preview image, title, URL, and actions
 *
 * @param link The link to display
 * @param onClick Callback when card is clicked
 * @param onFavoriteClick Callback when favorite icon is clicked
 * @param modifier Modifier for styling
 */
@Composable
fun LinkCard(
    link: Link,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
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

                // Status Banner Overlay (Archived/Deleted)
                if (link.isArchived || link.isDeleted) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    ) {
                        if (link.isDeleted) {
                            StatusBanner(
                                text = "Deleted",
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                labelColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        if (link.isArchived) {
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

                // Timestamp
                Text(
                    text = link.createdAt.toRelativeTime(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
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
