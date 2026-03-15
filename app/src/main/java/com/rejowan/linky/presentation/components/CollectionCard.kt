package com.rejowan.linky.presentation.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.rejowan.linky.domain.model.Collection
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

/**
 * CollectionCard component - Displays a collection with name, color, link count, and preview thumbnails
 *
 * @param collection The collection to display
 * @param linkCount Number of links in this collection
 * @param linkPreviews List of preview image paths (up to 3)
 * @param onClick Callback when card is clicked
 * @param onFavoriteClick Callback when favorite icon is clicked
 * @param modifier Modifier for styling
 */
@Composable
fun CollectionCard(
    collection: Collection,
    linkCount: Int,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier,
    linkPreviews: List<String?> = emptyList()
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Collection Icon (larger, prominent)
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(collection.color?.toColor() ?: MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = "Collection icon",
                    modifier = Modifier.size(36.dp),
                    tint = collection.color?.toColor()?.getContrastingColor()
                        ?: MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Middle: Collection Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Collection Name
                Text(
                    text = collection.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Link Count
                Text(
                    text = "$linkCount ${if (linkCount == 1) "link" else "links"}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )

                // Preview Thumbnails (only show if present)
                if (linkPreviews.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        linkPreviews.take(4).forEach { previewPath ->
                            PreviewThumbnail(
                                previewPath = previewPath,
                                collectionColor = collection.color?.toColor()
                            )
                        }
                    }
                }
            }

            // Right: Favorite Icon Button
            IconButton(
                onClick = onFavoriteClick
            ) {
                Icon(
                    imageVector = if (collection.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (collection.isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (collection.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Preview thumbnail for link preview images
 * Compact size for inline display
 */
@Composable
private fun PreviewThumbnail(
    previewPath: String?,
    collectionColor: Color?,
    modifier: Modifier = Modifier
) {
    if (previewPath != null) {
        AsyncImage(
            model = previewPath,
            contentDescription = "Link preview",
            modifier = modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
    } else {
        // Placeholder for links without preview
        Box(
            modifier = modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(collectionColor?.copy(alpha = 0.3f) ?: MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = "No preview",
                modifier = Modifier.size(10.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * CollectionGridCard - Grid view variant of CollectionCard
 * Displays collection with a color banner header and details below
 */
@Composable
fun CollectionGridCard(
    collection: Collection,
    linkCount: Int,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val collectionColor = collection.color?.toColor() ?: MaterialTheme.colorScheme.primaryContainer
    val iconTint = collection.color?.toColor()?.getContrastingColor() ?: MaterialTheme.colorScheme.onPrimaryContainer

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Color banner with folder icon and favorite button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(collectionColor)
            ) {
                // Folder icon in center
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.Center),
                    tint = iconTint.copy(alpha = 0.9f)
                )

                // Favorite button in top-right
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            if (collection.isFavorite)
                                MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                            else
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                        )
                        .clickable { onFavoriteClick() }
                        .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (collection.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (collection.isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (collection.isFavorite) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Content below banner
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Collection Name
                Text(
                    text = collection.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Link Count
                Text(
                    text = "$linkCount ${if (linkCount == 1) "link" else "links"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Convert hex color string to Color
 * Returns null if invalid format
 */
private fun String.toColor(): Color? {
    return try {
        val cleanHex = this.removePrefix("#")
        when (cleanHex.length) {
            6 -> Color(android.graphics.Color.parseColor("#$cleanHex"))
            8 -> Color(android.graphics.Color.parseColor("#$cleanHex"))
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}

/**
 * Get contrasting color (white or black) based on background luminance
 */
private fun Color.getContrastingColor(): Color {
    val luminance = (0.299 * red + 0.587 * green + 0.114 * blue)
    return if (luminance > 0.5) Color.Black else Color.White
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
