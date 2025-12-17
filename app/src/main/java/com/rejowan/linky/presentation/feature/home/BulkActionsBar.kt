package com.rejowan.linky.presentation.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Bulk actions bar shown when in selection mode
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
    onArchive: () -> Unit,
    onUnarchive: () -> Unit,
    onFavorite: () -> Unit,
    onUnfavorite: () -> Unit,
    onMove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            // Top row: Selection info and close
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Exit selection mode"
                        )
                    }
                    Text(
                        text = "$selectedCount selected",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Select all / Deselect all
                TextButton(
                    onClick = if (allSelected) onDeselectAll else onSelectAll
                ) {
                    Icon(
                        imageVector = if (allSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (allSelected) "Deselect all" else "Select all")
                }
            }

            // Bottom row: Actions
            AnimatedVisibility(visible = selectedCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Delete
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete selected",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }

                    // Archive / Unarchive based on filter
                    if (filterType == FilterType.ARCHIVED) {
                        IconButton(onClick = onUnarchive) {
                            Icon(
                                imageVector = Icons.Default.Unarchive,
                                contentDescription = "Unarchive selected"
                            )
                        }
                    } else {
                        IconButton(onClick = onArchive) {
                            Icon(
                                imageVector = Icons.Default.Archive,
                                contentDescription = "Archive selected"
                            )
                        }
                    }

                    // Favorite / Unfavorite based on filter
                    if (filterType == FilterType.FAVORITES) {
                        IconButton(onClick = onUnfavorite) {
                            Icon(
                                imageVector = Icons.Outlined.StarBorder,
                                contentDescription = "Remove from favorites"
                            )
                        }
                    } else {
                        IconButton(onClick = onFavorite) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Add to favorites",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Move to collection
                    IconButton(onClick = onMove) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.DriveFileMove,
                            contentDescription = "Move to collection"
                        )
                    }
                }
            }
        }
    }
}
