package com.rejowan.linky.presentation.feature.linkdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.rejowan.linky.domain.model.Link
import com.rejowan.linky.domain.model.Snapshot
import com.rejowan.linky.presentation.components.ErrorStates
import com.rejowan.linky.presentation.components.LoadingIndicator
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Link Detail Screen
 * Shows full details of a saved link including snapshots
 *
 * Features:
 * - Large preview image
 * - Complete link information (title, URL, note, folder)
 * - Timestamps (created, modified)
 * - Actions: Edit, Favorite, Archive, Delete
 * - Snapshots list
 * - Loading and error states
 *
 * @param linkId The ID of the link to display
 * @param onNavigateBack Callback to navigate back
 * @param onEditClick Callback when edit button is clicked
 * @param onOpenSnapshot Callback when a snapshot is clicked
 * @param modifier Modifier for styling
 * @param viewModel LinkDetailViewModel injected via Koin
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkDetailScreen(
    linkId: String,
    onNavigateBack: () -> Unit,
    onEditClick: (String) -> Unit,
    onOpenSnapshot: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LinkDetailViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showMoreMenu by remember { mutableStateOf(false) }

    // Navigate back on delete
    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) {
            onNavigateBack()
        }
    }

    // Show error in Snackbar
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Link Details",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                },
                actions = {
                    // Edit button
                    IconButton(
                        onClick = { state.link?.let { onEditClick(it.id) } },
                        enabled = state.link != null
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit link"
                        )
                    }

                    // Favorite toggle
                    IconButton(
                        onClick = { viewModel.onEvent(LinkDetailEvent.OnToggleFavorite) },
                        enabled = state.link != null
                    ) {
                        Icon(
                            imageVector = if (state.link?.isFavorite == true) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = if (state.link?.isFavorite == true) "Remove from favorites" else "Add to favorites",
                            tint = if (state.link?.isFavorite == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // More options menu
                    Box {
                        IconButton(
                            onClick = { showMoreMenu = true },
                            enabled = state.link != null
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "More options"
                            )
                        }

                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            // Archive/Unarchive
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (state.link?.isArchived == true) "Unarchive" else "Archive"
                                    )
                                },
                                onClick = {
                                    viewModel.onEvent(LinkDetailEvent.OnArchiveLink)
                                    showMoreMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (state.link?.isArchived == true) Icons.Filled.Unarchive else Icons.Filled.Archive,
                                        contentDescription = null
                                    )
                                }
                            )

                            // Delete
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    viewModel.onEvent(LinkDetailEvent.OnDeleteLink)
                                    showMoreMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                // Loading state
                state.isLoading -> {
                    LoadingIndicator(message = "Loading link...")
                }

                // Error state
                state.error != null && state.link == null -> {
                    ErrorStates.GenericError(
                        errorMessage = state.error ?: "Unknown error",
                        onRetryClick = { viewModel.onEvent(LinkDetailEvent.OnRefresh) }
                    )
                }

                // Link content
                state.link != null -> {
                    LinkContent(
                        link = state.link!!,
                        snapshots = state.snapshots,
                        onOpenUrl = { url ->
                            // Open URL in browser
                        },
                        onOpenSnapshot = onOpenSnapshot
                    )
                }
            }
        }
    }
}

/**
 * Link content display
 */
@Composable
private fun LinkContent(
    link: Link,
    snapshots: List<Snapshot>,
    onOpenUrl: (String) -> Unit,
    onOpenSnapshot: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Preview Image (if available)
        if (link.previewImagePath != null || link.previewUrl != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                AsyncImage(
                    model = link.previewImagePath ?: link.previewUrl,
                    contentDescription = "Link preview image",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Title
        Text(
            text = link.title,
            style = MaterialTheme.typography.headlineMedium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )

        // URL (clickable)
        TextButton(
            onClick = { uriHandler.openUri(link.url) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = link.url,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Note (if available)
        if (!link.note.isNullOrBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Note",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = link.note,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Metadata Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Folder (if assigned)
                link.folderId?.let { folderId ->
                    MetadataRow(
                        label = "Folder",
                        value = folderId // TODO: Resolve folder name from folderId
                    )
                }

                // Created date
                MetadataRow(
                    label = "Created",
                    value = formatTimestamp(link.createdAt)
                )

                // Modified date (if different from created)
                if (link.updatedAt != link.createdAt) {
                    MetadataRow(
                        label = "Modified",
                        value = formatTimestamp(link.updatedAt)
                    )
                }

                // Status badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (link.isFavorite) {
                        StatusBadge(text = "⭐ Favorite")
                    }
                    if (link.isArchived) {
                        StatusBadge(text = "📦 Archived")
                    }
                }
            }
        }

        // Snapshots section
        if (snapshots.isNotEmpty()) {
            HorizontalDivider()

            Text(
                text = "Snapshots (${snapshots.size})",
                style = MaterialTheme.typography.titleMedium
            )

            snapshots.forEach { snapshot ->
                SnapshotCard(
                    snapshot = snapshot,
                    onClick = { onOpenSnapshot(snapshot.id) }
                )
            }
        }

        // Open URL button
        Button(
            onClick = { uriHandler.openUri(link.url) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = "Open in Browser",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

/**
 * Metadata row for displaying label-value pairs
 */
@Composable
private fun MetadataRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Status badge (Favorite, Archived, etc.)
 */
@Composable
private fun StatusBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Snapshot card for displaying snapshot information
 */
@Composable
private fun SnapshotCard(
    snapshot: Snapshot,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = snapshot.type.name.replace("_", " "),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = formatTimestamp(snapshot.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = formatFileSize(snapshot.fileSize),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Format timestamp to readable date
 */
private fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
    return formatter.format(date)
}

/**
 * Format file size to human-readable format
 */
private fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    val gb = mb / 1024.0
    return "%.1f GB".format(gb)
}
