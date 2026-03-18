package com.rejowan.linky.presentation.feature.settings.dangerzone

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Cached
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DangerZoneScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DangerZoneViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showDeleteSnapshotsDialog by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Danger Zone",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // Warning banner
            WarningBanner(animationDelay = 0)

            Spacer(modifier = Modifier.height(24.dp))

            // Section label
            SectionLabel(text = "DATA MANAGEMENT", delay = 100)

            Spacer(modifier = Modifier.height(12.dp))

            // Clear Cache
            DangerZoneItem(
                icon = Icons.Rounded.Cached,
                title = "Clear Cache",
                subtitle = "Remove cached preview images and temporary files",
                accentColor = Color(0xFFFF9800),
                onClick = { showClearCacheDialog = true },
                animationDelay = 150
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Clear All Snapshots
            DangerZoneItem(
                icon = Icons.Rounded.DeleteSweep,
                title = "Delete All Snapshots",
                subtitle = "Remove all saved webpage snapshots",
                accentColor = Color(0xFFF57C00),
                onClick = { showDeleteSnapshotsDialog = true },
                animationDelay = 200
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Destructive section label
            SectionLabel(text = "DESTRUCTIVE ACTIONS", delay = 250)

            Spacer(modifier = Modifier.height(12.dp))

            // Delete All Data
            DangerZoneItem(
                icon = Icons.Rounded.DeleteForever,
                title = "Delete All Data",
                subtitle = "Permanently delete all links, collections, and settings",
                accentColor = Color(0xFFE53935),
                onClick = { showDeleteAllDialog = true },
                animationDelay = 300,
                isDestructive = true
            )

            // Result message
            state.resultMessage?.let { message ->
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (state.isSuccess) {
                        Color(0xFF4CAF50).copy(alpha = 0.12f)
                    } else {
                        Color(0xFFE53935).copy(alpha = 0.12f)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.isSuccess) Color(0xFF4CAF50) else Color(0xFFE53935),
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    // Clear Cache Dialog
    if (showClearCacheDialog) {
        ConfirmationDialog(
            icon = Icons.Rounded.Cached,
            iconColor = Color(0xFFFF9800),
            title = "Clear Cache?",
            message = "This will remove cached preview images and temporary files. Your links and collections will not be affected.",
            isProcessing = state.isProcessing && state.operationType == OperationType.CLEAR_CACHE,
            processingMessage = "Clearing cache...",
            confirmText = "Clear",
            confirmColor = Color(0xFFFF9800),
            onConfirm = { viewModel.clearCache() },
            onDismiss = { showClearCacheDialog = false }
        )
    }

    // Delete Snapshots Dialog
    if (showDeleteSnapshotsDialog) {
        ConfirmationDialog(
            icon = Icons.Rounded.DeleteSweep,
            iconColor = Color(0xFFF57C00),
            title = "Delete All Snapshots?",
            message = "This will permanently delete all saved webpage snapshots. This action cannot be undone.",
            isProcessing = state.isProcessing && state.operationType == OperationType.DELETE_SNAPSHOTS,
            processingMessage = "Deleting snapshots...",
            confirmText = "Delete",
            confirmColor = Color(0xFFF57C00),
            onConfirm = { viewModel.deleteAllSnapshots() },
            onDismiss = { showDeleteSnapshotsDialog = false }
        )
    }

    // Delete All Data Dialog
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { if (!state.isProcessing) showDeleteAllDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = Color(0xFFE53935)
                )
            },
            title = {
                Text(
                    text = "Delete All Data?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                if (state.isProcessing && state.operationType == OperationType.DELETE_ALL) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Deleting all data...")
                    }
                } else {
                    Column {
                        Text(
                            "This action cannot be undone. All your data will be permanently deleted:",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        BulletPoint("All saved links")
                        BulletPoint("All collections")
                        BulletPoint("All snapshots")
                        BulletPoint("Cached files")
                    }
                }
            },
            confirmButton = {
                if (!state.isProcessing) {
                    Button(
                        onClick = { viewModel.deleteAllData() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE53935)
                        )
                    ) {
                        Text("Delete Everything")
                    }
                }
            },
            dismissButton = {
                if (!state.isProcessing) {
                    OutlinedButton(onClick = { showDeleteAllDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    // Close dialogs on success
    LaunchedEffect(state.resultMessage) {
        if (state.resultMessage != null && state.isSuccess) {
            showClearCacheDialog = false
            showDeleteSnapshotsDialog = false
            showDeleteAllDialog = false
        }
    }
}

@Composable
private fun ConfirmationDialog(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    message: String,
    isProcessing: Boolean,
    processingMessage: String,
    confirmText: String,
    confirmColor: Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor
            )
        },
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (isProcessing) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(processingMessage)
                }
            } else {
                Text(message)
            }
        },
        confirmButton = {
            if (!isProcessing) {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = confirmColor
                    )
                ) {
                    Text(confirmText)
                }
            }
        },
        dismissButton = {
            if (!isProcessing) {
                OutlinedButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
private fun BulletPoint(text: String) {
    Row(
        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFE53935)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun WarningBanner(animationDelay: Int) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(animationDelay.toLong())
        isVisible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.95f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "banner scale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFE53935).copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = null,
                tint = Color(0xFFE53935),
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Caution Required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE53935)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Actions on this page may result in permanent data loss.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(
    text: String,
    delay: Int,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(delay.toLong())
        isVisible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.9f,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "section scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(200),
        label = "section alpha"
    )

    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = MaterialTheme.typography.labelMedium.letterSpacing * 1.5f
        ),
        color = Color(0xFFE53935).copy(alpha = alpha),
        modifier = modifier
            .scale(scale)
            .padding(start = 4.dp, bottom = 4.dp)
    )
}

@Composable
private fun DangerZoneItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit,
    animationDelay: Int,
    isDestructive: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(animationDelay.toLong())
        isVisible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.95f,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "item scale"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = accentColor),
                onClick = onClick
            ),
        shape = RoundedCornerShape(12.dp),
        color = if (isDestructive) {
            accentColor.copy(alpha = 0.08f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accentColor.copy(alpha = 0.15f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(24.dp),
                    tint = accentColor
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (isDestructive) accentColor else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}
