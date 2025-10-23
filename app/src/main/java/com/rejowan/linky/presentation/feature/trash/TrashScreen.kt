package com.rejowan.linky.presentation.feature.trash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rejowan.linky.domain.model.Link
import com.rejowan.linky.presentation.components.EmptyStates
import com.rejowan.linky.presentation.components.ErrorStates
import com.rejowan.linky.presentation.components.LinkCard
import com.rejowan.linky.presentation.components.LoadingIndicator
import org.koin.androidx.compose.koinViewModel

/**
 * Trash Screen - Shows all trashed links with restore/delete options
 *
 * @param onNavigateBack Callback to navigate back
 * @param onLinkClick Callback when a link is clicked
 * @param modifier Modifier for styling
 * @param viewModel TrashViewModel injected via Koin
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    onNavigateBack: () -> Unit,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TrashViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showEmptyTrashDialog by remember { mutableStateOf(false) }

    // Listen to UI events
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is TrashUiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Long
                    )
                }
                is TrashUiEvent.ShowLinkRestored -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "Link restored",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        // Undo the restore by moving back to trash (silent to prevent another snackbar)
                        viewModel.onEvent(TrashEvent.OnPermanentlyDeleteLink(event.linkId, silent = true))
                    }
                }
                is TrashUiEvent.ShowLinkDeleted -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "Link permanently deleted",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        // Undo the permanent delete by restoring
                        viewModel.onEvent(TrashEvent.OnUndoDelete(event.linkId))
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trash") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                },
                actions = {
                    if (state.trashedLinks.isNotEmpty()) {
                        TextButton(onClick = { showEmptyTrashDialog = true }) {
                            Text("Empty Trash")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Info card about trash
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(
                    text = "Links in trash will be permanently deleted after 30 days. You can restore or delete them manually.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Content Area with Pull-to-Refresh
            PullToRefreshBox(
                isRefreshing = state.isLoading && state.trashedLinks.isNotEmpty(),
                onRefresh = { viewModel.onEvent(TrashEvent.OnRefresh) },
                modifier = Modifier.fillMaxSize()
            ) {
                TrashContent(
                    state = state,
                    onLinkClick = onLinkClick,
                    onRestoreClick = { linkId ->
                        viewModel.onEvent(TrashEvent.OnRestoreLink(linkId))
                    },
                    onDeleteClick = { linkId ->
                        viewModel.onEvent(TrashEvent.OnPermanentlyDeleteLink(linkId))
                    },
                    onRetry = { viewModel.onEvent(TrashEvent.OnRefresh) }
                )
            }
        }
    }

    // Empty Trash Confirmation Dialog
    if (showEmptyTrashDialog) {
        EmptyTrashDialog(
            itemCount = state.trashedLinks.size,
            onConfirm = {
                viewModel.onEvent(TrashEvent.OnEmptyTrash)
                showEmptyTrashDialog = false
            },
            onDismiss = { showEmptyTrashDialog = false }
        )
    }
}

/**
 * Content area - Shows loading, error, empty, or trashed links list based on state
 */
@Composable
private fun TrashContent(
    state: TrashState,
    onLinkClick: (String) -> Unit,
    onRestoreClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            // Loading state (initial load only)
            state.isLoading && state.trashedLinks.isEmpty() -> {
                LoadingIndicator(message = "Loading trashed links...")
            }

            // Error state
            state.error != null -> {
                ErrorStates.GenericError(
                    errorMessage = state.error,
                    onRetryClick = onRetry
                )
            }

            // Empty state
            state.trashedLinks.isEmpty() -> {
                EmptyStates.NoTrashedLinks()
            }

            // Trashed links list
            else -> {
                TrashedLinksList(
                    links = state.trashedLinks,
                    onLinkClick = onLinkClick,
                    onRestoreClick = onRestoreClick,
                    onDeleteClick = onDeleteClick
                )
            }
        }
    }
}

/**
 * Trashed links list with restore and delete actions
 */
@Composable
private fun TrashedLinksList(
    links: List<Link>,
    onLinkClick: (String) -> Unit,
    onRestoreClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(
            items = links,
            key = { it.id }
        ) { link ->
            TrashLinkItem(
                link = link,
                onClick = { onLinkClick(link.id) },
                onRestoreClick = { onRestoreClick(link.id) },
                onDeleteClick = { onDeleteClick(link.id) }
            )
        }
    }
}

/**
 * Individual trash link item with restore and delete buttons
 */
@Composable
private fun TrashLinkItem(
    link: Link,
    onClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Link card without favorite button
            LinkCard(
                link = link,
                onClick = onClick,
                onFavoriteClick = {}, // No favorite action in trash
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons in a single row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onRestoreClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.RestoreFromTrash,
                        contentDescription = null
                    )
                    Text(
                        text = "Restore",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                OutlinedButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Delete",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

/**
 * Empty trash confirmation dialog
 */
@Composable
private fun EmptyTrashDialog(
    itemCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Empty Trash?") },
        text = {
            Text(
                "This will permanently delete $itemCount ${if (itemCount == 1) "item" else "items"}. This action cannot be undone."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Empty Trash", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        modifier = modifier
    )
}
