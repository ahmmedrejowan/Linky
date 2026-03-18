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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
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
 * CollectionCard component - Displays a collection with vibrant design
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
    val baseColor = collection.color?.toColor() ?: MaterialTheme.colorScheme.primary
    val lighterColor = baseColor.lighten(0.15f)
    val darkerColor = baseColor.darken(0.1f)
    val iconTint = baseColor.getContrastingColor()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(baseColor.copy(alpha = 0.03f))
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Gradient Icon Box with decorative elements
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(lighterColor, baseColor, darkerColor),
                            start = Offset(0f, 0f),
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    )
                    .drawBehind {
                        // Decorative circles
                        drawCircle(
                            color = Color.White.copy(alpha = 0.15f),
                            radius = 20.dp.toPx(),
                            center = Offset(size.width * 0.8f, size.height * 0.2f)
                        )
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.08f),
                            radius = 12.dp.toPx(),
                            center = Offset(size.width * 0.2f, size.height * 0.85f)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // Inner glow circle
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = "Collection icon",
                        modifier = Modifier.size(28.dp),
                        tint = iconTint
                    )
                }
            }

            // Collection Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Collection Name
                Text(
                    text = collection.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Link Count Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(baseColor.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$linkCount ${if (linkCount == 1) "link" else "links"}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = baseColor
                    )
                }

                // Preview Thumbnails (only show if present)
                if (linkPreviews.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy((-6).dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        linkPreviews.take(4).forEachIndexed { index, previewPath ->
                            PreviewThumbnail(
                                previewPath = previewPath,
                                collectionColor = baseColor,
                                modifier = Modifier.padding(start = if (index == 0) 0.dp else 0.dp)
                            )
                        }
                        if (linkCount > 4) {
                            // Show +N more indicator
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(baseColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+${linkCount - 4}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = baseColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Preview thumbnail for link preview images
 * Stacked overlapping style for visual interest
 */
@Composable
private fun PreviewThumbnail(
    previewPath: String?,
    collectionColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        collectionColor.copy(alpha = 0.2f),
                        collectionColor.copy(alpha = 0.1f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (previewPath != null) {
            AsyncImage(
                model = previewPath,
                contentDescription = "Link preview",
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = "No preview",
                modifier = Modifier.size(12.dp),
                tint = collectionColor.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * CollectionGridCard - Grid view variant of CollectionCard
 * Displays collection with vibrant gradient banner and modern design
 */
@Composable
fun CollectionGridCard(
    collection: Collection,
    linkCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val baseColor = collection.color?.toColor() ?: MaterialTheme.colorScheme.primary
    val lighterColor = baseColor.copy(alpha = 0.7f).lighten(0.2f)
    val darkerColor = baseColor.darken(0.15f)
    val iconTint = baseColor.getContrastingColor()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Gradient banner with decorative elements
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(lighterColor, baseColor, darkerColor),
                            start = Offset(0f, 0f),
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    )
                    .drawBehind {
                        // Draw decorative circles for visual interest
                        drawCircle(
                            color = Color.White.copy(alpha = 0.1f),
                            radius = 60.dp.toPx(),
                            center = Offset(size.width * 0.9f, size.height * 0.2f)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.08f),
                            radius = 40.dp.toPx(),
                            center = Offset(size.width * 0.1f, size.height * 0.8f)
                        )
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.05f),
                            radius = 30.dp.toPx(),
                            center = Offset(size.width * 0.7f, size.height * 0.9f)
                        )
                    }
            ) {
                // Folder icon with glow effect
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = iconTint.copy(alpha = 0.95f)
                    )
                }

                // Link count badge (top-left)
                Box(
                    modifier = Modifier
                        .padding(10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.25f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .align(Alignment.TopStart)
                ) {
                    Text(
                        text = "$linkCount",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                }
            }

            // Content below banner with subtle color tint
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(baseColor.copy(alpha = 0.04f))
                    .padding(14.dp)
            ) {
                // Collection Name
                Text(
                    text = collection.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Link count text
                Text(
                    text = if (linkCount == 1) "1 link" else "$linkCount links",
                    style = MaterialTheme.typography.bodySmall,
                    color = baseColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * Lighten a color by a factor (0.0 to 1.0)
 */
private fun Color.lighten(factor: Float): Color {
    return Color(
        red = (red + (1f - red) * factor).coerceIn(0f, 1f),
        green = (green + (1f - green) * factor).coerceIn(0f, 1f),
        blue = (blue + (1f - blue) * factor).coerceIn(0f, 1f),
        alpha = alpha
    )
}

/**
 * Darken a color by a factor (0.0 to 1.0)
 */
private fun Color.darken(factor: Float): Color {
    return Color(
        red = (red * (1f - factor)).coerceIn(0f, 1f),
        green = (green * (1f - factor)).coerceIn(0f, 1f),
        blue = (blue * (1f - factor)).coerceIn(0f, 1f),
        alpha = alpha
    )
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
