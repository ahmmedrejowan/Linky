package com.rejowan.linky.presentation.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdvancedFilterSheet(
    currentFilter: AdvancedFilter,
    availableDomains: List<DomainInfo>,
    availableCollections: List<CollectionFilterInfo>,
    onApply: (AdvancedFilter) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Local state for editing
    var localFilter by remember(currentFilter) { mutableStateOf(currentFilter) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Advanced Filters",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Date Range Section
            FilterSection(title = "Date Range") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DateRangeFilter.entries.forEach { dateRange ->
                        FilterChip(
                            selected = localFilter.dateRange == dateRange,
                            onClick = {
                                localFilter = localFilter.copy(dateRange = dateRange)
                            },
                            label = { Text(dateRange.displayName) },
                            leadingIcon = if (localFilter.dateRange == dateRange) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else null
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Domains Section (only show if there are domains)
            if (availableDomains.isNotEmpty()) {
                FilterSection(title = "Domains") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableDomains.take(10).forEach { domainInfo ->
                            val isSelected = localFilter.domains.contains(domainInfo.domain)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    localFilter = if (isSelected) {
                                        localFilter.copy(domains = localFilter.domains - domainInfo.domain)
                                    } else {
                                        localFilter.copy(domains = localFilter.domains + domainInfo.domain)
                                    }
                                },
                                label = { Text("${domainInfo.domain} (${domainInfo.count})") },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                } else null
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            }

            // Collections Section (only show if there are collections)
            if (availableCollections.isNotEmpty()) {
                FilterSection(title = "Collections") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableCollections.forEach { collection ->
                            val isSelected = localFilter.collectionIds.contains(collection.id)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    localFilter = if (isSelected) {
                                        localFilter.copy(collectionIds = localFilter.collectionIds - collection.id)
                                    } else {
                                        localFilter.copy(collectionIds = localFilter.collectionIds + collection.id)
                                    }
                                },
                                label = { Text("${collection.name} (${collection.count})") },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                } else null
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            }

            // Additional Filters Section
            FilterSection(title = "Additional Filters") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Has Notes filter
                    TriStateFilterChip(
                        label = "Has Notes",
                        state = localFilter.hasNote,
                        onStateChange = { newState ->
                            localFilter = localFilter.copy(hasNote = newState)
                        }
                    )

                    // Has Preview filter
                    TriStateFilterChip(
                        label = "Has Preview",
                        state = localFilter.hasPreview,
                        onStateChange = { newState ->
                            localFilter = localFilter.copy(hasPreview = newState)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        localFilter = AdvancedFilter.EMPTY
                    },
                    modifier = Modifier.weight(1f),
                    enabled = localFilter.isActive
                ) {
                    Text("Clear All")
                }
                Button(
                    onClick = {
                        onApply(localFilter)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Apply Filters")
                }
            }
        }
    }
}

@Composable
private fun FilterSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}

/**
 * Tri-state filter chip: null (unset), true (include), false (exclude)
 */
@Composable
private fun TriStateFilterChip(
    label: String,
    state: Boolean?,
    onStateChange: (Boolean?) -> Unit
) {
    val (displayLabel, colors) = when (state) {
        null -> label to FilterChipDefaults.filterChipColors()
        true -> "$label: Yes" to FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
        )
        false -> "$label: No" to FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.errorContainer
        )
    }

    FilterChip(
        selected = state != null,
        onClick = {
            // Cycle through: null -> true -> false -> null
            val newState = when (state) {
                null -> true
                true -> false
                false -> null
            }
            onStateChange(newState)
        },
        label = { Text(displayLabel) },
        colors = colors,
        leadingIcon = if (state != null) {
            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
        } else null
    )
}
