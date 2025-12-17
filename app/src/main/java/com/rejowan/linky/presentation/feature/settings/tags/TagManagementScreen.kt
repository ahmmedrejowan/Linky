package com.rejowan.linky.presentation.feature.settings.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rejowan.linky.domain.model.Tag
import com.rejowan.linky.domain.model.TagWithCount
import com.rejowan.linky.presentation.components.EmptyStates
import com.rejowan.linky.presentation.components.LoadingIndicator
import com.rejowan.linky.presentation.components.parseTagColor
import com.rejowan.linky.presentation.components.tagColors
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagManagementScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TagManagementViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Collect UI events
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is TagManagementUiEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Manage Tags",
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
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onEvent(TagManagementEvent.OnShowCreateDialog) }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create tag"
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading -> {
                    LoadingIndicator(message = "Loading tags...")
                }

                state.tags.isEmpty() -> {
                    EmptyTagsState(
                        onCreateClick = {
                            viewModel.onEvent(TagManagementEvent.OnShowCreateDialog)
                        }
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = state.tags,
                            key = { it.id }
                        ) { tag ->
                            TagItem(
                                tag = tag,
                                onEditClick = {
                                    viewModel.onEvent(TagManagementEvent.OnShowEditDialog(tag))
                                },
                                onDeleteClick = {
                                    viewModel.onEvent(TagManagementEvent.OnShowDeleteConfirm(tag))
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Create Tag Dialog
    if (state.showCreateDialog) {
        CreateEditTagDialog(
            title = "Create Tag",
            initialName = "",
            initialColor = tagColors.first(),
            onConfirm = { name, color ->
                viewModel.onEvent(TagManagementEvent.OnCreateTag(name, color))
            },
            onDismiss = {
                viewModel.onEvent(TagManagementEvent.OnDismissCreateDialog)
            }
        )
    }

    // Edit Tag Dialog
    if (state.showEditDialog && state.editingTag != null) {
        val editingTag = state.editingTag!!
        CreateEditTagDialog(
            title = "Edit Tag",
            initialName = editingTag.name,
            initialColor = editingTag.color ?: tagColors.first(),
            onConfirm = { name, color ->
                viewModel.onEvent(
                    TagManagementEvent.OnUpdateTag(
                        Tag(
                            id = editingTag.id,
                            name = name,
                            color = color,
                            createdAt = editingTag.createdAt,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                )
            },
            onDismiss = {
                viewModel.onEvent(TagManagementEvent.OnDismissEditDialog)
            }
        )
    }

    // Delete Confirmation Dialog
    if (state.showDeleteConfirmDialog && state.deletingTag != null) {
        val deletingTag = state.deletingTag!!
        AlertDialog(
            onDismissRequest = {
                viewModel.onEvent(TagManagementEvent.OnDismissDeleteConfirm)
            },
            title = { Text("Delete Tag") },
            text = {
                Column {
                    Text("Are you sure you want to delete \"${deletingTag.name}\"?")
                    if (deletingTag.linkCount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "This tag is used by ${deletingTag.linkCount} link${if (deletingTag.linkCount > 1) "s" else ""}. It will be removed from all links.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onEvent(TagManagementEvent.OnDeleteTag(deletingTag.id))
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.onEvent(TagManagementEvent.OnDismissDeleteConfirm)
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun TagItem(
    tag: TagWithCount,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tagColor = parseTagColor(tag.color) ?: MaterialTheme.colorScheme.primary

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tag info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Color indicator
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(tagColor)
                )

                Column {
                    Text(
                        text = tag.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${tag.linkCount} link${if (tag.linkCount != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Actions
            Row {
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit tag",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete tag",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateEditTagDialog(
    title: String,
    initialName: String,
    initialColor: String,
    onConfirm: (name: String, color: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedColor by remember { mutableStateOf(initialColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tag name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Color",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tagColors.forEach { color ->
                        ColorOption(
                            color = color,
                            selected = selectedColor == color,
                            onClick = { selectedColor = color }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim(), selectedColor) },
                enabled = name.isNotBlank()
            ) {
                Text(if (title == "Create Tag") "Create" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ColorOption(
    color: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val parsedColor = parseTagColor(color) ?: MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(parsedColor)
            .then(
                if (selected) {
                    Modifier.padding(2.dp)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun EmptyTagsState(
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Label,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No tags yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Create tags to organize your links",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onCreateClick) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Tag")
        }
    }
}
