package com.rejowan.linky.presentation.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.SelectAll
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Accent colors for action buttons
private val SelectionBlue = Color(0xFF2196F3)
private val AccentRed = Color(0xFFEF5350)
private val AccentPurple = Color(0xFF9575CD)
private val AccentOrange = Color(0xFFFF9800)

/**
 * Bulk actions bar shown when in selection mode
 * Modern design with colored action buttons
 */
@Composable
fun BulkActionsBar(
    selectedCount: Int,
    allSelected: Boolean,
    filterType: FilterType,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onDelete: () -> Unit,
    onFavorite: () -> Unit,
    onUnfavorite: () -> Unit,
    onMove: () -> Unit,
    totalCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            // Top row: Close button, count, select all
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Close button
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close selection",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Selection count
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$selectedCount selected",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = SelectionBlue
                    )
                    if (totalCount > 0) {
                        Text(
                            text = "of $totalCount items",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Select All / Deselect All button
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = if (allSelected) onDeselectAll else onSelectAll),
                    color = if (allSelected) {
                        SelectionBlue.copy(alpha = 0.15f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.SelectAll,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (allSelected) SelectionBlue
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (allSelected) "Deselect" else "All",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = if (allSelected) SelectionBlue
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Action buttons row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, start = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Delete button
                ActionButton(
                    icon = Icons.Default.Delete,
                    label = "Delete",
                    color = AccentRed,
                    enabled = selectedCount > 0,
                    onClick = onDelete,
                    modifier = Modifier.weight(1f)
                )

                // Favorite / Unfavorite based on filter
                if (filterType == FilterType.FAVORITES) {
                    ActionButton(
                        icon = Icons.Default.FavoriteBorder,
                        label = "Unfavorite",
                        color = AccentPurple,
                        enabled = selectedCount > 0,
                        onClick = onUnfavorite,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    ActionButton(
                        icon = Icons.Default.Favorite,
                        label = "Favorite",
                        color = AccentPurple,
                        enabled = selectedCount > 0,
                        onClick = onFavorite,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Move to collection
                ActionButton(
                    icon = Icons.AutoMirrored.Filled.DriveFileMove,
                    label = "Move",
                    color = AccentOrange,
                    enabled = selectedCount > 0,
                    onClick = onMove,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alpha = if (enabled) 1f else 0.4f

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        color = color.copy(alpha = 0.1f * alpha),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(18.dp),
                tint = color.copy(alpha = alpha)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/**
 * Animated wrapper for BulkActionsBar
 * Slides in from bottom when visible
 */
@Composable
fun AnimatedBulkActionsBar(
    visible: Boolean,
    selectedCount: Int,
    allSelected: Boolean,
    filterType: FilterType,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onDelete: () -> Unit,
    onFavorite: () -> Unit,
    onUnfavorite: () -> Unit,
    onMove: () -> Unit,
    totalCount: Int = 0,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        BulkActionsBar(
            selectedCount = selectedCount,
            allSelected = allSelected,
            filterType = filterType,
            onClose = onClose,
            onSelectAll = onSelectAll,
            onDeselectAll = onDeselectAll,
            onDelete = onDelete,
            onFavorite = onFavorite,
            onUnfavorite = onUnfavorite,
            onMove = onMove,
            totalCount = totalCount
        )
    }
}
