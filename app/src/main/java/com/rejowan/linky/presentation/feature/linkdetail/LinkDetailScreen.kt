package com.rejowan.linky.presentation.feature.linkdetail

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Note
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.rejowan.linky.domain.model.Link
import com.rejowan.linky.domain.model.Snapshot
import com.rejowan.linky.presentation.components.LoadingIndicator
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.rejowan.linky.ui.theme.SoftAccents

/**
 * Link Detail Screen - Modern redesigned version
 * Beautiful hero section, floating actions, clean cards
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
    val context = LocalContext.current
    var showMoreMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showPermanentDeleteDialog by remember { mutableStateOf(false) }

    // Navigate back on delete
    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) {
            onNavigateBack()
        }
    }

    // Collect and handle UI events
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is LinkDetailUiEvent.ShowSuccess -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
                is LinkDetailUiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Long
                    )
                }
                is LinkDetailUiEvent.ShowFavoriteToggled -> {
                    if (event.isFavorite) {
                        val result = snackbarHostState.showSnackbar(
                            message = "Added to favorites",
                            actionLabel = "Undo",
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.onEvent(LinkDetailEvent.OnToggleFavorite(silent = true))
                        }
                    } else {
                        Toast.makeText(context, "Removed from favorites", Toast.LENGTH_SHORT).show()
                    }
                }
                is LinkDetailUiEvent.ShowArchiveToggled -> {
                    val message = if (event.isArchived) "Link archived" else "Link unarchived"
                    snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
                }
                is LinkDetailUiEvent.ShowLinkTrashed -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "Link moved to trash",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onEvent(LinkDetailEvent.OnRestoreLink(silent = true))
                    }
                }
                is LinkDetailUiEvent.ShowLinkRestored -> {
                    snackbarHostState.showSnackbar(
                        message = "Link restored",
                        duration = SnackbarDuration.Short
                    )
                }
                is LinkDetailUiEvent.ShowLinkDeleted -> {
                    snackbarHostState.showSnackbar(
                        message = "Link permanently deleted",
                        duration = SnackbarDuration.Short
                    )
                }
                is LinkDetailUiEvent.NavigateToSnapshot -> {
                    onOpenSnapshot(event.snapshotId)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading -> {
                    LoadingIndicator(message = "Loading link...")
                }

                state.link != null -> {
                    val link = requireNotNull(state.link)
                    LinkDetailContent(
                        link = link,
                        collection = state.collection,
                        snapshots = state.snapshots,
                        isCapturingSnapshot = state.isCapturingSnapshot,
                        onNavigateBack = onNavigateBack,
                        onEditClick = { onEditClick(link.id) },
                        onOpenSnapshot = onOpenSnapshot,
                        onNavigateToCollection = onNavigateToCollection,
                        onToggleFavorite = { viewModel.onEvent(LinkDetailEvent.OnToggleFavorite()) },
                        onCreateSnapshot = { viewModel.onEvent(LinkDetailEvent.OnCreateSnapshot) },
                        onDeleteSnapshot = { viewModel.onEvent(LinkDetailEvent.OnDeleteSnapshot(it)) },
                        onDeleteLink = { showDeleteDialog = true },
                        onRestoreLink = { showRestoreDialog = true },
                        onPermanentDelete = { showPermanentDeleteDialog = true },
                        onOpenReaderMode = { viewModel.onEvent(LinkDetailEvent.OnOpenReaderMode) },
                        snackbarHostState = snackbarHostState
                    )
                }
            }
        }
    }

    // Delete confirmation bottom sheet
    if (showDeleteDialog) {
        ConfirmationBottomSheet(
            icon = Icons.Outlined.Delete,
            iconColor = MaterialTheme.colorScheme.error,
            title = "Move to Trash?",
            message = "This link will be moved to trash. You can restore it within 30 days.",
            confirmText = "Move to Trash",
            confirmColor = MaterialTheme.colorScheme.error,
            onConfirm = {
                viewModel.onEvent(LinkDetailEvent.OnDeleteLink())
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    // Restore confirmation bottom sheet
    if (showRestoreDialog) {
        ConfirmationBottomSheet(
            icon = Icons.Filled.RestoreFromTrash,
            iconColor = SoftAccents.Teal,
            title = "Restore Link?",
            message = "This link will be restored from trash and moved back to your links.",
            confirmText = "Restore",
            confirmColor = SoftAccents.Teal,
            onConfirm = {
                viewModel.onEvent(LinkDetailEvent.OnRestoreLink())
                showRestoreDialog = false
            },
            onDismiss = { showRestoreDialog = false }
        )
    }

    // Permanent delete bottom sheet
    if (showPermanentDeleteDialog) {
        ConfirmationBottomSheet(
            icon = Icons.Filled.Delete,
            iconColor = MaterialTheme.colorScheme.error,
            title = "Delete Permanently?",
            message = "This will permanently delete this link and all its snapshots. This action cannot be undone.",
            confirmText = "Delete Forever",
            confirmColor = MaterialTheme.colorScheme.error,
            onConfirm = {
                viewModel.onEvent(LinkDetailEvent.OnPermanentlyDeleteLink())
                showPermanentDeleteDialog = false
            },
            onDismiss = { showPermanentDeleteDialog = false }
        )
    }
}

@Composable
private fun LinkDetailContent(
    link: Link,
    collection: com.rejowan.linky.domain.model.Collection?,
    snapshots: List<Snapshot>,
    isCapturingSnapshot: Boolean,
    onNavigateBack: () -> Unit,
    onEditClick: () -> Unit,
    onOpenSnapshot: (String) -> Unit,
    onNavigateToCollection: (String) -> Unit,
    onToggleFavorite: () -> Unit,
    onCreateSnapshot: () -> Unit,
    onDeleteSnapshot: (String) -> Unit,
    onDeleteLink: () -> Unit,
    onRestoreLink: () -> Unit,
    onPermanentDelete: () -> Unit,
    onOpenReaderMode: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var showFullScreenImage by remember { mutableStateOf(false) }
    val hasImage = link.previewImagePath != null || link.previewUrl != null

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Hero Section with Image
        HeroSection(
            link = link,
            onNavigateBack = onNavigateBack,
            onEditClick = onEditClick,
            onToggleFavorite = onToggleFavorite,
            onImageClick = { if (hasImage) showFullScreenImage = true }
        )

        // Content Section
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // URL with click to copy
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        clipboardManager.setText(AnnotatedString(link.url))
                        Toast.makeText(context, "URL copied", Toast.LENGTH_SHORT).show()
                    },
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = link.url,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(12.dp)
                )
            }

            // Trash Status Banner
            if (link.isDeleted) {
                TrashStatusBanner(
                    deletedAt = link.deletedAt ?: 0,
                    onRestore = onRestoreLink,
                    onPermanentDelete = onPermanentDelete
                )
            }

            // Quick Actions Row
            if (!link.isDeleted) {
                QuickActionsRow(
                    onOpenInBrowser = { uriHandler.openUri(link.url) },
                    onShare = {
                        val shareIntent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(android.content.Intent.EXTRA_TEXT, link.url)
                            type = "text/plain"
                        }
                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share"))
                    },
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(link.url))
                        Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
                    },
                    onDelete = onDeleteLink
                )

                // Reader Mode Button
                ReaderModeButton(
                    isLoading = isCapturingSnapshot,
                    onClick = onOpenReaderMode
                )
            }

            // Collection Card
            collection?.let {
                CollectionCard(
                    collection = it,
                    onClick = { onNavigateToCollection(it.id) }
                )
            }

            // Description Section
            if (!link.description.isNullOrBlank()) {
                InfoSection(
                    icon = Icons.Outlined.Description,
                    title = "Description",
                    content = link.description,
                    accentColor = SoftAccents.Blue
                )
            }

            // Note Section
            if (!link.note.isNullOrBlank()) {
                InfoSection(
                    icon = Icons.Outlined.Note,
                    title = "Note",
                    content = link.note,
                    accentColor = SoftAccents.Teal
                )
            }

            // Timestamps Card
            TimestampsCard(
                createdAt = link.createdAt,
                updatedAt = link.updatedAt
            )

            // Snapshots Section
            SnapshotsSection(
                snapshots = snapshots,
                isCapturing = isCapturingSnapshot,
                isDeleted = link.isDeleted,
                onCreateSnapshot = onCreateSnapshot,
                onOpenSnapshot = onOpenSnapshot,
                onDeleteSnapshot = onDeleteSnapshot
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Full Screen Image Viewer
    if (showFullScreenImage && hasImage) {
        FullScreenImageViewer(
            imageUrl = link.previewImagePath ?: link.previewUrl ?: "",
            onDismiss = { showFullScreenImage = false }
        )
    }
}

@Composable
private fun HeroSection(
    link: Link,
    onNavigateBack: () -> Unit,
    onEditClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onImageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
    val hasImage = link.previewImagePath != null || link.previewUrl != null

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
    ) {
        // Background Image or Placeholder
        if (hasImage) {
            AsyncImage(
                model = link.previewImagePath ?: link.previewUrl,
                contentDescription = "Link preview",
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onImageClick),
                contentScale = ContentScale.Crop
            )
        } else {
            // Gradient placeholder
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                SoftAccents.Purple.copy(alpha = 0.3f),
                                SoftAccents.Blue.copy(alpha = 0.3f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }

        // Gradient overlay - darker at bottom for title visibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black.copy(alpha = 0.1f),
                            Color.Black.copy(alpha = 0.4f),
                            Color.Black.copy(alpha = 0.85f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusBarPadding.calculateTopPadding())
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back Button
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            // Action Buttons
            if (!link.isDeleted) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Edit Button
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = Color.Black.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit",
                            tint = Color.White
                        )
                    }

                    // Favorite Button
                    FavoriteButton(
                        isFavorite = link.isFavorite,
                        onClick = onToggleFavorite
                    )
                }
            }
        }

        // Title at bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = link.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FavoriteButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isFavorite) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "favorite scale"
    )

    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(40.dp)
            .scale(scale)
            .background(
                color = if (isFavorite) SoftAccents.Pink.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.3f),
                shape = CircleShape
            )
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
            tint = Color.White
        )
    }
}

@Composable
private fun QuickActionsRow(
    onOpenInBrowser: () -> Unit,
    onShare: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        QuickActionButton(
            icon = Icons.Filled.OpenInBrowser,
            label = "Open",
            color = SoftAccents.Blue,
            onClick = onOpenInBrowser
        )
        QuickActionButton(
            icon = Icons.Filled.Share,
            label = "Share",
            color = SoftAccents.Teal,
            onClick = onShare
        )
        QuickActionButton(
            icon = Icons.Filled.ContentCopy,
            label = "Copy",
            color = SoftAccents.Purple,
            onClick = onCopy
        )
        QuickActionButton(
            icon = Icons.Outlined.Delete,
            label = "Delete",
            color = MaterialTheme.colorScheme.error,
            onClick = onDelete
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = color.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ReaderModeButton(
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        enabled = !isLoading,
        modifier = modifier.fillMaxWidth(),
        color = SoftAccents.Teal.copy(alpha = 0.12f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = SoftAccents.Teal
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Loading Reader Mode...",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = SoftAccents.Teal
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.AutoStories,
                    contentDescription = null,
                    tint = SoftAccents.Teal,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Reader Mode",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = SoftAccents.Teal
                )
            }
        }
    }
}

@Composable
private fun TrashStatusBanner(
    deletedAt: Long,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val daysLeft = calculateDaysUntilAutoDelete(deletedAt)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "In Trash",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Auto-deletes in $daysLeft days",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    onClick = onRestore,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.RestoreFromTrash,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Restore",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Surface(
                    onClick = onPermanentDelete,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.error,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Delete",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onError
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionCard(
    collection: com.rejowan.linky.domain.model.Collection,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val collectionColor = parseColor(collection.color)

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        color = collectionColor.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = collectionColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Folder,
                    contentDescription = null,
                    tint = collectionColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Collection",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = collection.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = collectionColor
                )
            }
        }
    }
}

@Composable
private fun InfoSection(
    icon: ImageVector,
    title: String,
    content: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = accentColor.copy(alpha = 0.08f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor
                )
            }

            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (isExpanded) Int.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { isExpanded = !isExpanded }
            )

            if (content.length > 200) {
                Text(
                    text = if (isExpanded) "Show less" else "Show more",
                    style = MaterialTheme.typography.labelMedium,
                    color = accentColor,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { isExpanded = !isExpanded }
                )
            }
        }
    }
}

@Composable
private fun TimestampsCard(
    createdAt: Long,
    updatedAt: Long,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TimestampRow(
                icon = Icons.Outlined.CalendarToday,
                label = "Created",
                timestamp = createdAt
            )
            if (updatedAt != createdAt) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                TimestampRow(
                    icon = Icons.Outlined.AccessTime,
                    label = "Updated",
                    timestamp = updatedAt
                )
            }
        }
    }
}

@Composable
private fun TimestampRow(
    icon: ImageVector,
    label: String,
    timestamp: Long,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = formatTimestamp(timestamp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SnapshotsSection(
    snapshots: List<Snapshot>,
    isCapturing: Boolean,
    isDeleted: Boolean,
    onCreateSnapshot: () -> Unit,
    onOpenSnapshot: (String) -> Unit,
    onDeleteSnapshot: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Snapshots",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (!isDeleted) {
                Surface(
                    onClick = onCreateSnapshot,
                    enabled = !isCapturing,
                    color = SoftAccents.Purple,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CameraAlt,
                            contentDescription = null,
                            tint = if (isCapturing) Color.White.copy(alpha = 0.5f) else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isCapturing) "Capturing..." else "Capture",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isCapturing) Color.White.copy(alpha = 0.5f) else Color.White
                        )
                    }
                }
            }
        }

        if (snapshots.isEmpty()) {
            // Empty state
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No snapshots yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Capture a readable version for offline access",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            snapshots.forEach { snapshot ->
                SnapshotCard(
                    snapshot = snapshot,
                    onClick = { onOpenSnapshot(snapshot.id) },
                    onDeleteClick = { onDeleteSnapshot(snapshot.id) }
                )
            }
        }
    }
}

@Composable
private fun SnapshotCard(
    snapshot: Snapshot,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = snapshot.title ?: "Untitled Snapshot",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (snapshot.excerpt != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = snapshot.excerpt,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (snapshot.wordCount != null && snapshot.estimatedReadTime != null) {
                        Text(
                            text = "${snapshot.estimatedReadTime} min read",
                            style = MaterialTheme.typography.labelSmall,
                            color = SoftAccents.Purple,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "·",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = formatFileSize(snapshot.fileSize),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete snapshot",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    if (showDeleteDialog) {
        ConfirmationBottomSheet(
            icon = Icons.Outlined.Delete,
            iconColor = MaterialTheme.colorScheme.error,
            title = "Delete Snapshot?",
            message = "This will permanently delete this snapshot.",
            confirmText = "Delete",
            confirmColor = MaterialTheme.colorScheme.error,
            onConfirm = {
                onDeleteClick()
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfirmationBottomSheet(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    message: String,
    confirmText: String,
    confirmColor: Color = MaterialTheme.colorScheme.primary,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        color = iconColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Message
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cancel button
                Surface(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 14.dp)
                    )
                }

                // Confirm button
                Surface(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    color = confirmColor,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = confirmText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FullScreenImageViewer(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            // Image
            AsyncImage(
                model = imageUrl,
                contentDescription = "Full screen preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            // Close button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(
                        top = WindowInsets.statusBars
                            .asPaddingValues()
                            .calculateTopPadding() + 8.dp,
                        end = 8.dp
                    )
                    .size(44.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return formatter.format(date)
}

private fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    return "%.1f MB".format(mb)
}

private fun parseColor(colorString: String?): Color {
    if (colorString == null) return Color(0xFF6200EE)
    return try {
        val cleanColor = colorString.removePrefix("#")
        val colorInt = cleanColor.toLong(16)
        Color(colorInt or 0xFF000000)
    } catch (_: Exception) {
        Color(0xFF6200EE)
    }
}

private fun calculateDaysUntilAutoDelete(deletedAt: Long): Int {
    val now = System.currentTimeMillis()
    val deletionTime = deletedAt + (30 * 24 * 60 * 60 * 1000L)
    val remainingTime = deletionTime - now
    val daysLeft = (remainingTime / (24 * 60 * 60 * 1000L)).toInt()
    return maxOf(0, daysLeft)
}
