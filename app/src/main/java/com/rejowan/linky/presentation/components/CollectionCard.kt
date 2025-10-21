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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
 * @param modifier Modifier for styling
 */
@Composable
fun CollectionCard(
    collection: Collection,
    linkCount: Int,
    onClick: () -> Unit,
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
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left: Collection Icon (restored to original)
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(collection.color?.toColor() ?: MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = "Collection icon",
                        modifier = Modifier.size(32.dp),
                        tint = collection.color?.toColor()?.getContrastingColor()
                            ?: MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                // Middle: Collection Info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
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
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Last Modified Timestamp
                    Text(
                        text = collection.updatedAt.toRelativeTime(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // Right: Favorite Icon
                if (collection.isFavorite) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "Favorite collection",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Link Preview Thumbnails - Bottom Right Corner
            if (linkPreviews.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    linkPreviews.take(3).forEach { previewPath ->
                        PreviewThumbnail(
                            previewPath = previewPath,
                            collectionColor = collection.color?.toColor()
                        )
                    }
                }
            }
        }
    }
}

/**
 * Preview thumbnail for link preview images
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
                .size(18.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
    } else {
        // Placeholder for links without preview
        Box(
            modifier = modifier
                .size(18.dp)
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
