package com.rejowan.linky.presentation.feature.trash

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.outlined.AutoDelete
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.rejowan.linky.domain.model.Link
import com.rejowan.linky.presentation.components.ErrorStates
import com.rejowan.linky.presentation.components.LoadingIndicator
import com.rejowan.linky.ui.theme.SoftAccents
import org.koin.androidx.compose.koinViewModel
import java.net.URI
import java.util.concurrent.TimeUnit

// Colors for trash screen
private val RestoreGreen = Color(0xFF4CAF50)
private val DeleteRed = Color(0xFFE53935)
private val WarningAmber = Color(0xFFFF9800)

/**
 * Trash Screen - Modern design with swipe gestures and visual feedback
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
    var showEmptyTrashSheet by remember { mutableStateOf(false) }

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
                        viewModel.onEvent(TrashEvent.OnUndoDelete(event.linkId))
                    }
                }
                is TrashUiEvent.ShowAllRestored -> {
                    snackbarHostState.showSnackbar(
                        message = "${event.count} links restored",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Trash")
                        if (state.trashedLinks.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${state.trashedLinks.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
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
                    if (state.trashedLinks.isNotEmpty()) {
                        // Restore All button
                        IconButton(onClick = { viewModel.onEvent(TrashEvent.OnRestoreAll) }) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = "Restore all",
                                tint = RestoreGreen
                            )
                        }
                        // Empty Trash button
                        IconButton(onClick = { showEmptyTrashSheet = true }) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Empty trash",
                                tint = DeleteRed
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
        PullToRefreshBox(
            isRefreshing = state.isLoading && state.trashedLinks.isNotEmpty(),
            onRefresh = { viewModel.onEvent(TrashEvent.OnRefresh) },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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

    // Empty Trash Bottom Sheet
    if (showEmptyTrashSheet) {
        EmptyTrashBottomSheet(
            itemCount = state.trashedLinks.size,
            onConfirm = {
                viewModel.onEvent(TrashEvent.OnEmptyTrash)
                showEmptyTrashSheet = false
            },
            onDismiss = { showEmptyTrashSheet = false }
        )
    }
}

/**
 * Content area - Shows loading, error, empty, or trashed links list
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
            state.isLoading && state.trashedLinks.isEmpty() -> {
                LoadingIndicator(message = "Loading trash...")
            }

            state.error != null -> {
                ErrorStates.GenericError(
                    errorMessage = state.error,
                    onRetryClick = onRetry
                )
            }

            state.trashedLinks.isEmpty() -> {
                TrashEmptyState()
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Info Banner
                    item {
                        TrashInfoBanner()
                    }

                    // Trashed Links
                    items(
                        items = state.trashedLinks,
                        key = { it.id }
                    ) { link ->
                        TrashLinkCard(
                            link = link,
                            onClick = { onLinkClick(link.id) },
                            onRestore = { onRestoreClick(link.id) },
                            onDelete = { onDeleteClick(link.id) }
                        )
                    }

                    // Bottom spacing
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

/**
 * Modern info banner with icon
 */
@Composable
private fun TrashInfoBanner(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = SoftAccents.Amber.copy(alpha = 0.15f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Schedule,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = SoftAccents.Amber
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "Items here will be automatically deleted after 30 days",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Modern trash link card with preview image and days remaining badge
 */
@Composable
private fun TrashLinkCard(
    link: Link,
    onClick: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val daysRemaining = calculateDaysRemaining(link.deletedAt)
    val domain = link.url.extractDomain()
    val accentColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Main content row with preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Preview Image
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.15f),
                                    accentColor.copy(alpha = 0.08f),
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                            )
                        )
                        .drawBehind {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.2f),
                                radius = 20.dp.toPx(),
                                center = Offset(size.width * 0.85f, size.height * 0.15f)
                            )
                        }
                ) {
                    if (link.previewImagePath != null || link.previewUrl != null) {
                        AsyncImage(
                            model = link.previewImagePath ?: link.previewUrl,
                            contentDescription = "Preview for ${link.title}",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Placeholder
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = accentColor.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                // Content
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Domain + Days remaining
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = domain,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        DaysRemainingBadge(daysRemaining = daysRemaining)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Title
                    Text(
                        text = link.title.ifBlank { "Untitled" },
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Description (if available)
                    if (!link.description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = link.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Restore button
                FilledTonalButton(
                    onClick = onRestore,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = RestoreGreen.copy(alpha = 0.15f),
                        contentColor = RestoreGreen
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restore", fontWeight = FontWeight.Medium)
                }

                // Delete button
                FilledTonalButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = DeleteRed.copy(alpha = 0.15f),
                        contentColor = DeleteRed
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

/**
 * Days remaining badge with color coding
 */
@Composable
private fun DaysRemainingBadge(
    daysRemaining: Int,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when {
        daysRemaining <= 3 -> DeleteRed.copy(alpha = 0.15f) to DeleteRed
        daysRemaining <= 7 -> WarningAmber.copy(alpha = 0.15f) to WarningAmber
        else -> MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Schedule,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = textColor
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (daysRemaining <= 0) "Today" else "${daysRemaining}d",
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Custom empty state for trash with floating animation
 */
@Composable
private fun TrashEmptyState(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "trash float")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float offset"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated icon container
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset(y = (-floatOffset).dp)
                .size(100.dp)
                .background(
                    color = SoftAccents.Purple.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(28.dp)
                )
        ) {
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = SoftAccents.Purple
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Trash is Empty",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Deleted links will appear here for 30 days before being permanently removed",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Empty trash confirmation bottom sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmptyTrashBottomSheet(
    itemCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Warning icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        color = DeleteRed.copy(alpha = 0.12f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoDelete,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = DeleteRed
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Empty Trash?",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "This will permanently delete $itemCount ${if (itemCount == 1) "link" else "links"}. This action cannot be undone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }

                FilledTonalButton(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = DeleteRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Empty Trash", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

/**
 * Calculate days remaining before auto-deletion (30 days from deletedAt)
 */
private fun calculateDaysRemaining(deletedAt: Long?): Int {
    if (deletedAt == null) return 30
    val now = System.currentTimeMillis()
    val deleteAfter = deletedAt + TimeUnit.DAYS.toMillis(30)
    val remaining = deleteAfter - now
    return maxOf(0, TimeUnit.MILLISECONDS.toDays(remaining).toInt())
}

/**
 * Extract domain from URL
 */
private fun String.extractDomain(): String {
    return try {
        val uri = URI(this)
        uri.host?.removePrefix("www.") ?: this
    } catch (e: Exception) {
        this
    }
}
