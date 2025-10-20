package com.rejowan.linky.presentation.feature.linkdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.FavoriteBorder
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.rejowan.linky.domain.model.Link
import com.rejowan.linky.domain.model.Snapshot
import com.rejowan.linky.presentation.components.ErrorStates
import com.rejowan.linky.presentation.components.LoadingIndicator
import kotlinx.coroutines.launch
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
 * - Complete link information (title, URL, note, collection)
 * - Timestamps (created, modified)
 * - Actions: Edit, Favorite, Archive, Delete
 * - Snapshots list
 * - Loading and error states
 *
 * @param linkId The ID of the link to display
 * @param onNavigateBack Callback to navigate back
 * @param onEditClick Callback when edit button is clicked
 * @param onOpenSnapshot Callback when a snapshot is clicked
 * @param onNavigateToCollection Callback when collection is clicked
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
    onNavigateToCollection: (String) -> Unit = {},
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

    // Show success message for snapshot capture
    LaunchedEffect(state.snapshotCaptured) {
        if (state.snapshotCaptured) {
            snackbarHostState.showSnackbar("Snapshot captured successfully")
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
                            imageVector = if (state.link?.isFavorite == true) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (state.link?.isFavorite == true) "Remove from favorites" else "Add to favorites",
                            tint = if (state.link?.isFavorite == true) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
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
                        collection = state.collection,
                        snapshots = state.snapshots,
                        isCapturingSnapshot = state.isCapturingSnapshot,
                        onOpenUrl = { url ->
                            // Open URL in browser
                        },
                        onOpenSnapshot = onOpenSnapshot,
                        onNavigateToCollection = onNavigateToCollection,
                        onCreateSnapshot = { viewModel.onEvent(LinkDetailEvent.OnCreateSnapshot) },
                        onDeleteSnapshot = { snapshotId ->
                            viewModel.onEvent(LinkDetailEvent.OnDeleteSnapshot(snapshotId))
                        },
                        snackbarHostState = snackbarHostState
                    )
                }
            }
        }
    }
}

/**
 * Link content display with new layout
 */
@Composable
private fun LinkContent(
    link: Link,
    collection: com.rejowan.linky.domain.model.Collection?,
    snapshots: List<Snapshot>,
    isCapturingSnapshot: Boolean,
    onOpenUrl: (String) -> Unit,
    onOpenSnapshot: (String) -> Unit,
    onNavigateToCollection: (String) -> Unit,
    onCreateSnapshot: () -> Unit,
    onDeleteSnapshot: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var descriptionExpanded by remember { mutableStateOf(false) }
    var noteExpanded by remember { mutableStateOf(false) }
    var snapshotsExpanded by remember { mutableStateOf(false) }

    // Launch coroutine scope for snackbar
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Preview Image (if available)
        if (link.previewImagePath != null || link.previewUrl != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(200.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                AsyncImage(
                    model = link.previewImagePath ?: link.previewUrl,
                    contentDescription = "Link preview image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Title and URL Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = link.title,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = link.url,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Action Buttons Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Share button
            OutlinedButton(
                onClick = {
                    val shareIntent = android.content.Intent().apply {
                        action = android.content.Intent.ACTION_SEND
                        putExtra(android.content.Intent.EXTRA_TEXT, link.url)
                        type = "text/plain"
                    }
                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Share link"))
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share")
            }

            // Copy button
            OutlinedButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(link.url))
                    scope.launch {
                        snackbarHostState.showSnackbar("Link copied")
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Copy")
            }
        }

        // Open in Browser button
        Button(
            onClick = { uriHandler.openUri(link.url) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.OpenInBrowser,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open in Browser")
        }

        // Metadata Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Collection (if assigned)
                collection?.let { coll ->
                    Card(
                        onClick = { onNavigateToCollection(coll.id) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(
                                        color = parseColor(coll.color),
                                        shape = CircleShape
                                    )
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Collection",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = coll.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Timestamps
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Created",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatTimestamp(link.createdAt),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    if (link.updatedAt != link.createdAt) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Updated",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatTimestamp(link.updatedAt),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        // Description Section
        if (!link.description.isNullOrBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Description",
                    style = MaterialTheme.typography.titleLarge
                )
                ExpandableTextSection(
                    text = link.description,
                    maxLines = 2,
                    isExpanded = descriptionExpanded,
                    onToggleExpand = { descriptionExpanded = !descriptionExpanded }
                )
            }
        }

        // Note Section
        if (!link.note.isNullOrBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Note",
                    style = MaterialTheme.typography.titleLarge
                )
                ExpandableTextSection(
                    text = link.note,
                    maxLines = 4,
                    isExpanded = noteExpanded,
                    onToggleExpand = { noteExpanded = !noteExpanded }
                )
            }
        }

        // Snapshots Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Snapshots (${snapshots.size})",
                    style = MaterialTheme.typography.titleLarge
                )

                TextButton(
                    onClick = onCreateSnapshot,
                    enabled = !isCapturingSnapshot
                ) {
                    if (isCapturingSnapshot) {
                        Text("Capturing...")
                    } else {
                        Text("Capture")
                    }
                }
            }

            // Content
            if (snapshots.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No snapshots yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Capture a clean, readable version of this page for offline reading",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Show max 3 snapshots, or all if expanded
                val displaySnapshots = if (snapshotsExpanded) snapshots else snapshots.take(3)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    displaySnapshots.forEach { snapshot ->
                        SnapshotCard(
                            snapshot = snapshot,
                            onClick = { onOpenSnapshot(snapshot.id) },
                            onDeleteClick = { onDeleteSnapshot(snapshot.id) }
                        )
                    }
                }

                // See More / Hide button
                if (snapshots.size > 3) {
                    TextButton(
                        onClick = { snapshotsExpanded = !snapshotsExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (snapshotsExpanded) "Show Less" else "See More (${snapshots.size - 3} more)",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

/**
 * Snapshot card for displaying snapshot information with reader mode metadata
 */
@Composable
private fun SnapshotCard(
    snapshot: Snapshot,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Title (from reader mode)
                Text(
                    text = snapshot.title ?: "Untitled Snapshot",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Excerpt
                if (snapshot.excerpt != null) {
                    Text(
                        text = snapshot.excerpt,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Metadata row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Word count & read time
                    if (snapshot.wordCount != null && snapshot.estimatedReadTime != null) {
                        Text(
                            text = "${snapshot.wordCount} words · ${snapshot.estimatedReadTime} min read",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Timestamp & size
                Text(
                    text = "${formatTimestamp(snapshot.createdAt)} · ${formatFileSize(snapshot.fileSize)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Delete button
            IconButton(
                onClick = { showDeleteDialog = true }
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete snapshot",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Snapshot?") },
            text = { Text("This will permanently delete this snapshot. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteClick()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
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

/**
 * Expandable text section with See More/Hide functionality
 */
@Composable
private fun ExpandableTextSection(
    text: String,
    maxLines: Int,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isExpanded) {
                // Show full text
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium
                )
                // Hide button
                TextButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Show Less")
                }
            } else {
                // Show truncated text
                var showSeeMore by remember { mutableStateOf(false) }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = maxLines,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { textLayoutResult ->
                        showSeeMore = textLayoutResult.hasVisualOverflow
                    }
                )
                // See More button (only if text is truncated)
                if (showSeeMore) {
                    TextButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("See More")
                    }
                }
            }
        }
    }
}

/**
 * Parse color string to Color
 * Supports hex colors (#RRGGBB) and falls back to primary color
 */
private fun parseColor(colorString: String?): Color {
    if (colorString == null) return Color(0xFF6200EE) // Default primary color

    return try {
        val cleanColor = colorString.removePrefix("#")
        val colorInt = cleanColor.toLong(16)
        Color(colorInt or 0xFF000000) // Ensure alpha is set
    } catch (_: Exception) {
        Color(0xFF6200EE) // Default primary color on error
    }
}
