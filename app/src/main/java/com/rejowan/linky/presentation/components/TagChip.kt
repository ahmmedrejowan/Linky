package com.rejowan.linky.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rejowan.linky.domain.model.Tag

/**
 * Parse color string to Color
 * Supports hex colors like "#FF5722" or color names
 */
fun parseTagColor(colorString: String?): Color? {
    if (colorString.isNullOrBlank()) return null
    return try {
        Color(android.graphics.Color.parseColor(colorString))
    } catch (e: Exception) {
        null
    }
}

/**
 * Tag chip component for displaying a single tag
 */
@Composable
fun TagChip(
    tag: Tag,
    onClick: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    val tagColor = parseTagColor(tag.color) ?: MaterialTheme.colorScheme.primary
    val backgroundColor = if (selected) {
        tagColor.copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (selected) {
        tagColor
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(1.dp, tagColor)
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier.padding(
                start = 10.dp,
                end = if (onRemove != null) 4.dp else 10.dp,
                top = 6.dp,
                bottom = 6.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Color dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(tagColor, CircleShape)
            )

            // Tag name
            Text(
                text = tag.name,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Remove button (optional)
            if (onRemove != null) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove tag",
                        modifier = Modifier.size(14.dp),
                        tint = contentColor
                    )
                }
            }
        }
    }
}

/**
 * Small tag chip for compact display (e.g., in link cards)
 */
@Composable
fun TagChipSmall(
    tag: Tag,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val tagColor = parseTagColor(tag.color) ?: MaterialTheme.colorScheme.primary

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(12.dp),
        color = tagColor.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Color dot
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(tagColor, CircleShape)
            )

            // Tag name
            Text(
                text = tag.name,
                style = MaterialTheme.typography.labelSmall,
                color = tagColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Flow row of tags with overflow handling
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagChipRow(
    tags: List<Tag>,
    onTagClick: ((Tag) -> Unit)? = null,
    modifier: Modifier = Modifier,
    maxRows: Int = 2
) {
    if (tags.isEmpty()) return

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        maxLines = maxRows
    ) {
        tags.forEach { tag ->
            TagChipSmall(
                tag = tag,
                onClick = if (onTagClick != null) {
                    { onTagClick(tag) }
                } else null
            )
        }
    }
}

/**
 * Selectable tag chip for tag selection UI
 */
@Composable
fun SelectableTagChip(
    tag: Tag,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TagChip(
        tag = tag,
        onClick = onClick,
        selected = selected,
        modifier = modifier
    )
}
