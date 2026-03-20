package com.rejowan.linky.presentation.feature.batchimport

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rejowan.linky.data.local.preferences.ThemePreferences
import com.rejowan.linky.presentation.components.EmptyState
import com.rejowan.linky.ui.theme.SoftAccents
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import kotlin.random.Random

/**
 * Batch Import Screen - Step 1: Paste Text
 *
 * Features:
 * - Large multiline text field for pasting URLs
 * - Character and line count indicators
 * - Import/Check button (enabled when text is not empty)
 * - Cancel button with confirmation dialog
 * - Auto-expanding text field
 * - Dismissable "How it works" card with preference persistence
 * - Info icon in top bar to show "How it works" dialog
 *
 * @param onNavigateBack Navigate back to Settings
 * @param onStartScan Start scanning for URLs (proceeds to next step)
 * @param modifier Modifier for styling
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchImportScreen(
    prefillText: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToCollectionDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BatchImportViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val themePreferences = remember { ThemePreferences(context) }
    val coroutineScope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    val snackbarHostState = remember { SnackbarHostState() }

    // ViewModel state
    val state by viewModel.state.collectAsState()

    // Confirmation dialog state triggered by ViewModel
    var viewModelConfirmationType by remember { mutableStateOf<ConfirmationType?>(null) }

    // Collect UI events
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is BatchImportUiEvent.NavigateToHome -> {
                    onNavigateToHome()
                }
                is BatchImportUiEvent.NavigateToSettings -> {
                    onNavigateBack()
                }
                is BatchImportUiEvent.NavigateToCollection -> {
                    onNavigateToCollectionDetail(event.collectionId)
                }
                is BatchImportUiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is BatchImportUiEvent.ShowConfirmation -> {
                    viewModelConfirmationType = event.type
                }
            }
        }
    }

    // Local UI state
    var pastedText by remember(prefillText) { mutableStateOf(prefillText ?: "") }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var showClearConfirmationDialog by remember { mutableStateOf(false) }
    var showHowItWorksDialog by remember { mutableStateOf(false) }

    // Observe the preference for showing "How it works" card
    val shouldShowHowItWorks by themePreferences.shouldShowBatchImportHowItWorks()
        .collectAsState(initial = true)

    // Calculate statistics
    val characterCount = pastedText.length
    val lineCount = if (pastedText.isEmpty()) 0 else pastedText.lines().size

    // Note: Scanning completion is handled by ScanResultDialog which shows when
    // state.extractedUrls.isNotEmpty() && !state.isScanning && state.error == null

    // Handle back press with confirmation
    val handleBackPress = {
        if (pastedText.isNotEmpty()) {
            showConfirmationDialog = true
        } else {
            onNavigateBack()
        }
    }

    // Intercept system back gesture/button for Step 1
    if (!state.showSelectionScreen) {
        BackHandler(enabled = pastedText.isNotEmpty()) {
            showConfirmationDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Batch Import Links") },
                navigationIcon = {
                    IconButton(onClick = handleBackPress) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showHowItWorksDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "How it works",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        // Show different screens based on state
        when {
            state.importResult != null -> {
                // Step 7: Import Result
                ImportResultScreen(
                    result = state.importResult, // Null checked in when condition
                    onEvent = viewModel::onEvent,
                    onNavigateBack = onNavigateBack,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            state.showPreviewScreen -> {
                // Step 5: Preview Fetching & Results
                PreviewFetchingScreen(
                    state = state,
                    onEvent = viewModel::onEvent,
                    onNavigateBack = {
                        viewModel.onEvent(BatchImportEvent.OnBackToEdit)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            state.showSelectionScreen -> {
                // Step 4: Link Selection & Review
                LinkSelectionScreen(
                    state = state,
                    onEvent = viewModel::onEvent,
                    onNavigateBack = {
                        viewModel.onEvent(BatchImportEvent.OnBackToEdit)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            else -> {
                // Step 1: Paste Text
                Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // Test Section (for development/testing)
            // TestDataCard(
            //     onLoadNormalText = {
            //         pastedText = getRandomNormalTestData()
            //     },
            //     onLoadLargeText = {
            //         pastedText = getLargeTestData()
            //     }
            // )

            // Instructions Card (dismissable)
            if (shouldShowHowItWorks) {
                HowItWorksCard(
                    onDismiss = {
                        coroutineScope.launch {
                            themePreferences.setShowBatchImportHowItWorks(false)
                        }
                    }
                )
            }

            // Paste and Clear buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                // Clear button
                OutlinedButton(
                    onClick = {
                        if (pastedText.isNotEmpty()) {
                            showClearConfirmationDialog = true
                        }
                    },
                    enabled = pastedText.isNotEmpty(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text("Clear")
                }

                // Paste button
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            val clipEntry = clipboard.getClipEntry()
                            val clipboardText = clipEntry?.clipData?.getItemAt(0)?.text?.toString()
                            if (!clipboardText.isNullOrEmpty()) {
                                // Append to existing text instead of replacing
                                pastedText = if (pastedText.isEmpty()) {
                                    clipboardText
                                } else {
                                    pastedText + "\n" + clipboardText
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = "Paste",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text("Paste")
                }
            }

            // Large multiline text field
            OutlinedTextField(
                value = pastedText,
                onValueChange = { pastedText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                placeholder = {
                    Text(
                        text = "Paste your URLs here...\n\nExample:\nhttps://example.com\nhttps://github.com/user/repo\nCheck out this link: https://blog.com/post",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                textStyle = MaterialTheme.typography.bodyMedium,
                shape = RoundedCornerShape(12.dp),
                maxLines = Int.MAX_VALUE,
                singleLine = false
            )

            // Modern Statistics Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Character count card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SoftAccents.Blue.copy(alpha = 0.12f))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "$characterCount",
                                style = MaterialTheme.typography.headlineSmall,
                                color = SoftAccents.Blue
                            )
                            Text(
                                text = "characters",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Line count card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SoftAccents.Teal.copy(alpha = 0.12f))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "$lineCount",
                                style = MaterialTheme.typography.headlineSmall,
                                color = SoftAccents.Teal
                            )
                            Text(
                                text = "lines",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = handleBackPress,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        viewModel.onEvent(BatchImportEvent.OnTextChanged(pastedText))
                        viewModel.onEvent(BatchImportEvent.OnStartScan)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    enabled = pastedText.isNotEmpty() && !state.isScanning,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (state.isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Check URLs")
                    }
                }
            }
        }
            } // end else (Step 1)
        } // end when
    }

    // Discard content confirmation dialog
    if (showConfirmationDialog) {
        ConfirmationDialog(
            title = "Leave Import?",
            message = "You have pasted text that hasn't been processed yet. Do you want to leave and discard it?",
            confirmText = "Leave",
            onConfirm = {
                showConfirmationDialog = false
                onNavigateBack()
            },
            onDismiss = {
                showConfirmationDialog = false
            }
        )
    }

    // Clear content confirmation dialog
    if (showClearConfirmationDialog) {
        ConfirmationDialog(
            title = "Clear All Text?",
            message = "This will remove all text from the field. This action cannot be undone.",
            confirmText = "Clear",
            onConfirm = {
                showClearConfirmationDialog = false
                pastedText = ""
            },
            onDismiss = {
                showClearConfirmationDialog = false
            }
        )
    }

    // ViewModel-triggered confirmation dialogs
    viewModelConfirmationType?.let { type ->
        when (type) {
            ConfirmationType.DISCARD_PASTE -> {
                ConfirmationDialog(
                    title = "Discard Pasted Text?",
                    message = "You have pasted text that hasn't been processed. Do you want to discard it?",
                    confirmText = "Discard",
                    onConfirm = {
                        viewModelConfirmationType = null
                        viewModel.onEvent(BatchImportEvent.OnBackToEdit)
                    },
                    onDismiss = { viewModelConfirmationType = null }
                )
            }
            ConfirmationType.DISCARD_SELECTION -> {
                ConfirmationDialog(
                    title = "Discard Selection?",
                    message = "Your link selection will be lost. Do you want to continue?",
                    confirmText = "Discard",
                    onConfirm = {
                        viewModelConfirmationType = null
                        viewModel.onEvent(BatchImportEvent.OnBackToEdit)
                    },
                    onDismiss = { viewModelConfirmationType = null }
                )
            }
            ConfirmationType.CANCEL_IMPORT -> {
                ConfirmationDialog(
                    title = "Cancel Import?",
                    message = "The import is in progress. Are you sure you want to cancel?",
                    confirmText = "Cancel Import",
                    onConfirm = {
                        viewModelConfirmationType = null
                        viewModel.onEvent(BatchImportEvent.OnBackToEdit)
                    },
                    onDismiss = { viewModelConfirmationType = null }
                )
            }
            ConfirmationType.CANCEL_AFTER_FETCH_FAILURE -> {
                ConfirmationDialog(
                    title = "Continue Without Previews?",
                    message = "Some previews failed to load. Do you want to cancel or continue with available data?",
                    confirmText = "Cancel",
                    onConfirm = {
                        viewModelConfirmationType = null
                        viewModel.onEvent(BatchImportEvent.OnBackToEdit)
                    },
                    onDismiss = { viewModelConfirmationType = null }
                )
            }
        }
    }

    // How It Works Dialog
    if (showHowItWorksDialog) {
        HowItWorksDialog(
            onDismiss = { showHowItWorksDialog = false }
        )
    }

    // Scanning Dialog
    if (state.isScanning) {
        ScanningDialog(
            progress = state.scanProgress
        )
    }

    // Scanning Result Dialog
    if (state.error is BatchImportError.NoUrlsFound) {
        AlertDialog(
            onDismissRequest = {
                // Clear error
                viewModel.resetState()
            },
            title = {
                Text("No URLs Found")
            },
            text = {
                Text(
                    text = (state.error as BatchImportError.NoUrlsFound).message,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetState()
                    }
                ) {
                    Text("OK")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Success: Show extracted URLs count (only if not already on selection screen)
    if (state.extractedUrls.isNotEmpty() && !state.isScanning && state.error == null && !state.showSelectionScreen) {
        ScanResultDialog(
            totalUrls = state.totalUrls,
            duplicateCount = state.duplicateCount,
            onProceed = {
                viewModel.onEvent(BatchImportEvent.OnProceedToSelection)
            },
            onBackToEdit = {
                viewModel.onEvent(BatchImportEvent.OnBackToEdit)
            }
        )
    }

    // Importing Dialog
    if (state.isImporting) {
        ImportingDialog(
            progress = state.importProgress
        )
    }

    // Create Collection Dialog
    if (state.showCreateCollectionDialog) {
        CreateCollectionDialog(
            collectionName = state.newCollectionName,
            selectedColor = state.newCollectionColor,
            onCollectionNameChange = { viewModel.onEvent(BatchImportEvent.OnNewCollectionNameChange(it)) },
            onColorChange = { viewModel.onEvent(BatchImportEvent.OnNewCollectionColorChange(it)) },
            onSave = { viewModel.onEvent(BatchImportEvent.OnCreateCollectionConfirm) },
            onDismiss = { viewModel.onEvent(BatchImportEvent.OnCreateCollectionDismiss) }
        )
    }
}

/**
 * Test Data Card - Load sample data for testing
 * Styled to match the app's modern design
 */
@Composable
private fun TestDataCard(
    onLoadNormalText: () -> Unit,
    onLoadLargeText: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = SoftAccents.Purple.copy(alpha = 0.12f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = SoftAccents.Purple,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Test Data",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Normal Text Button
                OutlinedButton(
                    onClick = onLoadNormalText,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Normal",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "5-15 links",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Large Text Button
                OutlinedButton(
                    onClick = onLoadLargeText,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Large",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "150+ links",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * How It Works Card - Dismissable with close button
 * Modern design matching the app's visual style
 */
@Composable
private fun HowItWorksCard(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = SoftAccents.Blue

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = accentColor.copy(alpha = 0.12f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(end = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Icon
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "How it works",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Paste text containing multiple URLs in any format. We'll automatically extract and import them for you.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Close button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * How It Works Dialog - Shown when info icon is clicked
 */
@Composable
private fun HowItWorksDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = SoftAccents.Blue,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "How it works",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Paste text containing multiple URLs in any format. We'll automatically extract and import them for you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Examples:",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "• https://example.com\n" +
                          "• Check out https://github.com/user/repo\n" +
                          "• Multiple links on one line: site1.com, site2.com\n" +
                          "• Mixed with regular text",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Got it")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

/**
 * Reusable confirmation dialog component
 */
@Composable
private fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

/**
 * Scanning Dialog - Shows progress while extracting URLs
 */
@Composable
private fun ScanningDialog(
    progress: String
) {
    AlertDialog(
        onDismissRequest = { /* Cannot dismiss while scanning */ },
        title = {
            Text(
                text = "Scanning for URLs...",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = SoftAccents.Blue
                )
                if (progress.isNotEmpty()) {
                    Text(
                        text = progress,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = { /* No button while scanning */ },
        shape = RoundedCornerShape(20.dp)
    )
}

/**
 * Scan Result Dialog - Shows scanning results and allows navigation
 */
@Composable
private fun ScanResultDialog(
    totalUrls: Int,
    duplicateCount: Int,
    onProceed: () -> Unit,
    onBackToEdit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onBackToEdit,
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = SoftAccents.Teal,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "URLs Found",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Found $totalUrls unique URL${if (totalUrls != 1) "s" else ""} ready to import",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (duplicateCount > 0) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = SoftAccents.Amber.copy(alpha = 0.12f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "$duplicateCount URL${if (duplicateCount != 1) "s" else ""} already saved (marked as duplicate)",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Text(
                    text = "New links: ${totalUrls - duplicateCount}",
                    style = MaterialTheme.typography.titleSmall,
                    color = SoftAccents.Teal
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onProceed,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Review Links")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onBackToEdit,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Back to Edit")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

/**
 * Link Selection & Review Screen - Step 4
 *
 * Features:
 * - Modern statistics dashboard with visual indicators
 * - Action buttons to remove all or remove duplicates
 * - List of URLs with domain, full URL, duplicate badge, and remove button
 * - Bottom action buttons (Next and Back)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinkSelectionScreen(
    state: BatchImportState,
    onEvent: (BatchImportEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Local state for confirmation dialogs
    var showRemoveDuplicatesDialog by remember { mutableStateOf(false) }
    var showRemoveUnselectedDialog by remember { mutableStateOf(false) }
    var showBackConfirmationDialog by remember { mutableStateOf(false) }
    var showActionsMenu by remember { mutableStateOf(false) }

    // Intercept system back gesture/button for Step 4
    BackHandler {
        showBackConfirmationDialog = true
    }

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Modern Statistics Dashboard
        StatisticsDashboard(
            totalUrls = state.totalUrls,
            duplicateCount = state.duplicateCount,
            newLinks = state.totalUrls - state.duplicateCount,
            selectedCount = state.selectedCount
        )

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Smart Select All / Deselect All toggle button
            val isAllSelected = state.selectedCount == state.totalUrls && state.totalUrls > 0
            OutlinedButton(
                onClick = {
                    if (isAllSelected) {
                        onEvent(BatchImportEvent.OnDeselectAll)
                    } else {
                        onEvent(BatchImportEvent.OnSelectAll)
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = state.urlStatuses.isNotEmpty()
            ) {
                Icon(
                    imageVector = if (isAllSelected) Icons.Default.Clear else Icons.Default.Check,
                    contentDescription = if (isAllSelected) "Deselect All" else "Select All",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.padding(4.dp))
                Text(if (isAllSelected) "Deselect All" else "Select All")
            }

            // More Actions dropdown menu
            Box(
                modifier = Modifier.weight(1f)
            ) {
                OutlinedButton(
                    onClick = { showActionsMenu = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.urlStatuses.isNotEmpty()
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More Actions",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text("More Actions")
                }

                androidx.compose.material3.DropdownMenu(
                    expanded = showActionsMenu,
                    onDismissRequest = { showActionsMenu = false }
                ) {
                    // Select New Only
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("Select New Only") },
                        onClick = {
                            onEvent(BatchImportEvent.OnSelectNewOnly)
                            showActionsMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null
                            )
                        },
                        enabled = state.duplicateCount > 0
                    )

                    androidx.compose.material3.HorizontalDivider()

                    // Remove Duplicates (destructive)
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("Remove Duplicates", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showActionsMenu = false
                            showRemoveDuplicatesDialog = true
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        enabled = state.duplicateCount > 0
                    )

                    // Remove Unselected (destructive)
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("Remove Unselected", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showActionsMenu = false
                            showRemoveUnselectedDialog = true
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        enabled = state.selectedCount < state.totalUrls
                    )
                }
            }
        }

        // Link List
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            if (state.urlStatuses.isEmpty()) {
                // Empty state
                EmptyState(
                    icon = Icons.Default.Info,
                    title = "No URLs to review",
                    message = "All URLs have been removed",
                    accentColor = SoftAccents.Amber
                )
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(
                        count = state.urlStatuses.size,
                        key = { index -> state.urlStatuses[index].url }
                    ) { index ->
                        val urlStatus = state.urlStatuses[index]
                        LinkListItem(
                            urlStatus = urlStatus,
                            onToggleSelection = { onEvent(BatchImportEvent.OnToggleUrlSelection(urlStatus.url)) },
                            onRemove = { onEvent(BatchImportEvent.OnRemoveUrl(urlStatus.url)) }
                        )
                    }
                }
            }
        }

        // Bottom Actions
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Selected count
            Text(
                text = "${state.selectedCount} links selected",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { showBackConfirmationDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Back")
                }

                Button(
                    onClick = {
                        onEvent(BatchImportEvent.OnStartFetching)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    enabled = state.selectedCount > 0,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Next")
                }
            }
        }
    }

    // Confirmation Dialogs

    // Back confirmation
    if (showBackConfirmationDialog) {
        ConfirmationDialog(
            title = "Leave Import?",
            message = "You have ${state.selectedCount} links selected. Do you want to leave and go back to editing?",
            confirmText = "Leave",
            onConfirm = {
                showBackConfirmationDialog = false
                onNavigateBack()
            },
            onDismiss = {
                showBackConfirmationDialog = false
            }
        )
    }

    // Remove Duplicates confirmation
    if (showRemoveDuplicatesDialog) {
        ConfirmationDialog(
            title = "Remove Duplicates?",
            message = "This will permanently remove ${state.duplicateCount} duplicate URL(s) from the list. This action cannot be undone.",
            confirmText = "Remove",
            onConfirm = {
                showRemoveDuplicatesDialog = false
                onEvent(BatchImportEvent.OnRemoveDuplicates)
            },
            onDismiss = {
                showRemoveDuplicatesDialog = false
            }
        )
    }

    // Remove Unselected confirmation
    if (showRemoveUnselectedDialog) {
        val unselectedCount = state.totalUrls - state.selectedCount
        ConfirmationDialog(
            title = "Remove Unselected?",
            message = "This will permanently remove $unselectedCount unselected URL(s) from the list. This action cannot be undone.",
            confirmText = "Remove",
            onConfirm = {
                showRemoveUnselectedDialog = false
                onEvent(BatchImportEvent.OnRemoveUnselected)
            },
            onDismiss = {
                showRemoveUnselectedDialog = false
            }
        )
    }
}

/**
 * Modern Statistics Dashboard with visual indicators
 */
@Composable
private fun StatisticsDashboard(
    totalUrls: Int,
    duplicateCount: Int,
    newLinks: Int,
    selectedCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text(
                text = "Review Links",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Stats Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Total
                StatCard(
                    modifier = Modifier.weight(1f),
                    value = totalUrls.toString(),
                    label = "Total",
                    color = SoftAccents.Blue
                )
                // New
                StatCard(
                    modifier = Modifier.weight(1f),
                    value = newLinks.toString(),
                    label = "New",
                    color = SoftAccents.Teal
                )
                // Duplicates
                StatCard(
                    modifier = Modifier.weight(1f),
                    value = duplicateCount.toString(),
                    label = "Duplicates",
                    color = if (duplicateCount > 0) SoftAccents.Amber else MaterialTheme.colorScheme.outline
                )
                // Selected
                StatCard(
                    modifier = Modifier.weight(1f),
                    value = selectedCount.toString(),
                    label = "Selected",
                    color = SoftAccents.Purple
                )
            }

            // Selection Progress Bar
            if (totalUrls > 0) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Selection Progress",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(selectedCount * 100 / totalUrls)}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = SoftAccents.Purple
                        )
                    }
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { selectedCount.toFloat() / totalUrls.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = SoftAccents.Purple,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Individual Stat Card for the dashboard
 */
@Composable
private fun StatCard(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Individual Link List Item
 */
@Composable
private fun LinkListItem(
    urlStatus: UrlStatus,
    onToggleSelection: () -> Unit,
    onRemove: () -> Unit
) {
    val accentColor = if (urlStatus.isDuplicate) SoftAccents.Amber else SoftAccents.Blue

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleSelection() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Selection Checkbox with custom styling
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (urlStatus.isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .clickable { onToggleSelection() },
            contentAlignment = Alignment.Center
        ) {
            if (urlStatus.isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Domain Icon/Preview placeholder
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    accentColor.copy(alpha = 0.12f)
                ),
            contentAlignment = Alignment.Center
        ) {
            // Show first letter of domain or link icon
            Text(
                text = urlStatus.domain.firstOrNull()?.uppercase() ?: "",
                style = MaterialTheme.typography.titleMedium,
                color = accentColor
            )
        }

        // URL Info
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Domain name with duplicate indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = urlStatus.domain,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                // Smart duplicate indicator - small dot instead of badge
                if (urlStatus.isDuplicate) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(SoftAccents.Amber)
                    )
                }
            }

            // Full URL (ellipsized)
            Text(
                text = urlStatus.url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Duplicate label if applicable
            if (urlStatus.isDuplicate) {
                Text(
                    text = "Already saved",
                    style = MaterialTheme.typography.labelSmall,
                    color = SoftAccents.Amber
                )
            }
        }

        // Remove button - more subtle
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                .clickable { onRemove() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Preview Fetching Screen - Step 5
 *
 * Features:
 * - Progress indicator showing chunks and current/total
 * - Real-time preview cards as they're fetched
 * - Collection picker for organizing imports
 * - Import button when fetching completes
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreviewFetchingScreen(
    state: BatchImportState,
    onEvent: (BatchImportEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Intercept system back gesture
    BackHandler {
        onNavigateBack()
    }

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Progress Section
        if (state.isFetching) {
            FetchingProgressCard(
                progress = state.fetchProgress,
                currentFetchingUrl = state.currentFetchingUrl
            )
        } else {
            // Completion Summary - Modern design
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Success icon with background
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(SoftAccents.Teal.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = SoftAccents.Teal,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Previews Ready",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${state.previewResults.size} links ready to import",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Count badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SoftAccents.Teal)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${state.previewResults.size}",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Preview Results List
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Link Previews",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (state.previewResults.isNotEmpty()) {
                        Text(
                            text = "${state.previewResults.size} loaded",
                            style = MaterialTheme.typography.labelMedium,
                            color = SoftAccents.Teal
                        )
                    }
                }

                androidx.compose.material3.HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                if (state.previewResults.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(SoftAccents.Blue.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    strokeWidth = 3.dp,
                                    color = SoftAccents.Blue
                                )
                            }
                            Text(
                                text = "Starting preview fetch...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
                    ) {
                        items(
                            count = state.previewResults.size,
                            key = { index -> state.previewResults[index].url }
                        ) { index ->
                            PreviewCard(state.previewResults[index])
                        }
                    }
                }
            }
        }

        // Collection Picker (only show when not fetching and has results)
        if (!state.isFetching && state.previewResults.isNotEmpty()) {
            CollectionPicker(
                collections = state.collections,
                selectedCollectionId = state.selectedCollectionId,
                onCollectionSelected = { collectionId ->
                    onEvent(BatchImportEvent.OnCollectionSelected(collectionId))
                },
                onCreateNewClick = {
                    onEvent(BatchImportEvent.OnCreateCollectionClick)
                }
            )
        }

        // Bottom Actions
        if (!state.isFetching) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Back")
                }

                Button(
                    onClick = {
                        onEvent(BatchImportEvent.OnStartImport)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    enabled = state.previewResults.isNotEmpty(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Import")
                }
            }
        }
    }
}

/**
 * Fetching Progress Card - Modern design with real-time progress
 */
@Composable
private fun FetchingProgressCard(
    progress: FetchProgress?,
    currentFetchingUrl: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with animation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pulsing indicator
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SoftAccents.Blue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 3.dp,
                            color = SoftAccents.Blue
                        )
                    }
                    Column {
                        Text(
                            text = "Fetching Previews",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (progress != null) {
                            Text(
                                text = "${progress.current} of ${progress.total} links processed",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                // Percentage badge
                if (progress != null && progress.total > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SoftAccents.Blue)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${(progress.current * 100 / progress.total)}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }
            }

            // Current URL being fetched
            if (currentFetchingUrl != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Small loading indicator
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = SoftAccents.Blue
                    )
                    Text(
                        text = currentFetchingUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Progress section
            if (progress != null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        val progressFraction = if (progress.total > 0) {
                            progress.current.toFloat() / progress.total.toFloat()
                        } else 0f
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(SoftAccents.Blue)
                        )
                    }

                    // Real-time stats row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Success count
                        FetchStatChip(
                            count = progress.successCount,
                            label = "Success",
                            color = SoftAccents.Teal,
                            modifier = Modifier.weight(1f)
                        )
                        // Error count
                        FetchStatChip(
                            count = progress.errorCount,
                            label = "Error",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                        // Timeout count
                        FetchStatChip(
                            count = progress.timeoutCount,
                            label = "Timeout",
                            color = SoftAccents.Amber,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                // Indeterminate progress
                androidx.compose.material3.LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = SoftAccents.Blue,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

/**
 * Small stat chip for fetch progress
 */
@Composable
private fun FetchStatChip(
    count: Int,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelLarge,
            color = color,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = 0.8f),
            maxLines = 1
        )
    }
}

/**
 * Preview Card for individual link
 * Modern design with preview image support
 */
@Composable
private fun PreviewCard(result: LinkPreviewResult) {
    val accentColor = when (result) {
        is LinkPreviewResult.Success -> SoftAccents.Teal
        is LinkPreviewResult.Error -> MaterialTheme.colorScheme.error
        is LinkPreviewResult.Timeout -> SoftAccents.Amber
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Preview Image / Placeholder
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                when (result) {
                    is LinkPreviewResult.Success -> {
                        if (result.imageUrl != null) {
                            coil.compose.AsyncImage(
                                model = result.imageUrl,
                                contentDescription = "Preview",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            // Domain initial as fallback
                            Text(
                                text = result.domain.firstOrNull()?.uppercase() ?: "",
                                style = MaterialTheme.typography.headlineSmall,
                                color = accentColor
                            )
                        }
                    }
                    is LinkPreviewResult.Error -> {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    is LinkPreviewResult.Timeout -> {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                when (result) {
                    is LinkPreviewResult.Success -> {
                        // Title
                        Text(
                            text = result.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Description
                        if (result.description != null) {
                            Text(
                                text = result.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Domain Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = result.domain,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    is LinkPreviewResult.Error -> {
                        Text(
                            text = result.domain,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Preview failed - will use domain as title",
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor.copy(alpha = 0.8f)
                        )
                    }

                    is LinkPreviewResult.Timeout -> {
                        Text(
                            text = result.domain,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Request timeout - will use domain as title",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Status indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
        }
    }
}

/**
 * Importing Dialog - Shows progress while importing links
 */
@Composable
private fun ImportingDialog(
    progress: ImportProgress?
) {
    AlertDialog(
        onDismissRequest = { /* Cannot dismiss */ },
        title = {
            Text(
                text = "Importing Links",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = SoftAccents.Purple
                )

                if (progress != null) {
                    Text(
                        text = "Saving ${progress.current} of ${progress.total} links...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        text = "Preparing to save links...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {},
        shape = RoundedCornerShape(20.dp)
    )
}

/**
 * Import Result Screen - Shows success/failure summary
 * Modern design with visual indicators
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportResultScreen(
    result: BatchImportResult?,
    onEvent: (BatchImportEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Return early if result is null
    if (result == null) return

    // Intercept system back gesture
    BackHandler {
        onEvent(BatchImportEvent.OnDone)
    }

    val resultAccentColor = when {
        result.isCompleteSuccess -> SoftAccents.Teal
        result.isCompleteFailure -> MaterialTheme.colorScheme.error
        else -> SoftAccents.Amber
    }

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Result Card - Modern design
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon with background
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(resultAccentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (result.isCompleteSuccess) {
                            Icons.Default.Check
                        } else {
                            Icons.Default.Info
                        },
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = resultAccentColor
                    )
                }

                // Title
                Text(
                    text = when {
                        result.isCompleteSuccess -> "Import Complete!"
                        result.isCompleteFailure -> "Import Failed"
                        else -> "Partial Success"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Stats Row - Visual indicators
                if (!result.isCompleteSuccess || !result.isCompleteFailure) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Success stat
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SoftAccents.Teal.copy(alpha = 0.12f))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${result.totalSuccess}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = SoftAccents.Teal
                                )
                                Text(
                                    text = "Imported",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Failed stat (if any)
                        if (result.hasFailures) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${result.totalFailed}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = "Failed",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Simple summary for single-state results
                    Text(
                        text = if (result.isCompleteSuccess) {
                            "Successfully imported ${result.totalSuccess} link${if (result.totalSuccess == 1) "" else "s"}"
                        } else {
                            "Failed to import all ${result.totalFailed} link${if (result.totalFailed == 1) "" else "s"}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Failed Links (if any)
        if (result.hasFailures) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Failed Links",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${result.totalFailed} failed",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    androidx.compose.material3.HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    androidx.compose.foundation.lazy.LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(result.failed.size) { index ->
                            val failedImport = result.failed[index]
                            FailedLinkItem(
                                url = failedImport.url,
                                error = failedImport.error
                            )
                        }
                    }
                }
            }
        }

        // Bottom Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Retry Button (only if there are failures)
            if (result.hasFailures && !result.isCompleteFailure) {
                OutlinedButton(
                    onClick = {
                        onEvent(BatchImportEvent.OnRetryFailed)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Retry Failed")
                }
            }

            // Retry All Button (only if complete failure)
            if (result.isCompleteFailure) {
                OutlinedButton(
                    onClick = {
                        onEvent(BatchImportEvent.OnRetryImport)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Retry All")
                }
            }

            // Done Button
            Button(
                onClick = {
                    onEvent(BatchImportEvent.OnDone)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Done")
            }
        }
    }
}

/**
 * Failed Link Item for the result screen
 */
@Composable
private fun FailedLinkItem(
    url: String,
    error: String?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Error indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = url,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (error != null) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Collection Dropdown - Dropdown to select a collection for imported links
 * Similar to AddEditLinkScreen implementation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollectionPicker(
    collections: List<com.rejowan.linky.domain.model.Collection>,
    selectedCollectionId: String?,
    onCollectionSelected: (String?) -> Unit,
    onCreateNewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    // Find selected collection name - Optimized with remember to avoid recalculation
    val selectedCollectionName = remember(selectedCollectionId, collections) {
        if (selectedCollectionId == null) {
            "(No Collection)"
        } else {
            collections.find { it.id == selectedCollectionId }?.name ?: "(No Collection)"
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedCollectionName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Collection") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // "No Collection" option
            DropdownMenuItem(
                text = {
                    Text(
                        text = "(No Collection)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                onClick = {
                    onCollectionSelected(null)
                    expanded = false
                }
            )

            // Collection options
            collections.forEach { collection ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = collection.name,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = {
                        onCollectionSelected(collection.id)
                        expanded = false
                    }
                )
            }

            // "Create New" option
            DropdownMenuItem(
                text = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create new collection",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Create New Collection",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                onClick = {
                    onCreateNewClick()
                    expanded = false
                }
            )
        }
    }
}

/**
 * Create Collection Dialog
 * Allows users to create a new collection with name and color
 */
@Composable
private fun CreateCollectionDialog(
    collectionName: String,
    selectedColor: String?,
    onCollectionNameChange: (String) -> Unit,
    onColorChange: (String?) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Create Collection",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Collection name input
                OutlinedTextField(
                    value = collectionName,
                    onValueChange = onCollectionNameChange,
                    label = { Text("Collection Name") },
                    placeholder = { Text("Enter collection name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Color picker
                Text(
                    text = "Color (Optional)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ColorBlockPicker(
                    selectedColor = selectedColor,
                    onColorSelected = onColorChange
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = collectionName.isNotBlank()
            ) {
                Text("Create")
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

/**
 * Color block picker with visual color rectangles
 * 15 total: 1 no color + 14 colors, arranged in 3 rows of 5
 */
@Composable
private fun ColorBlockPicker(
    selectedColor: String?,
    onColorSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = listOf(
        null,           // No Color - default
        "#FF6B6B",      // Red
        "#E74C3C",      // Dark Red
        "#4ECDC4",      // Teal
        "#45B7D1",      // Blue
        "#3498DB",      // Strong Blue
        "#FFA07A",      // Orange
        "#E67E22",      // Dark Orange
        "#98D8C8",      // Green
        "#2ECC71",      // Emerald Green
        "#F7B731",      // Yellow
        "#F39C12",      // Golden Yellow
        "#5F27CD",      // Purple
        "#9B59B6",      // Light Purple
        "#EE5A6F"       // Pink
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Row 1: 5 colors
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            colors.take(5).forEach { colorHex ->
                ColorBlock(
                    colorHex = colorHex,
                    isSelected = selectedColor == colorHex,
                    onClick = { onColorSelected(colorHex) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Row 2: 5 colors
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            colors.subList(5, 10).forEach { colorHex ->
                ColorBlock(
                    colorHex = colorHex,
                    isSelected = selectedColor == colorHex,
                    onClick = { onColorSelected(colorHex) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Row 3: 5 colors
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            colors.subList(10, 15).forEach { colorHex ->
                ColorBlock(
                    colorHex = colorHex,
                    isSelected = selectedColor == colorHex,
                    onClick = { onColorSelected(colorHex) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Individual color block component
 */
@Composable
private fun ColorBlock(
    colorHex: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                color = if (colorHex != null) {
                    Color(android.graphics.Color.parseColor(colorHex))
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                },
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Show checkmark for selected color
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = if (colorHex != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ============================================================================
// TEST DATA FUNCTIONS
// ============================================================================

/**
 * Get random normal test data (5-15 links)
 * Returns one of several test cases for variety
 */
private fun getRandomNormalTestData(): String {
    val testDataList = listOf(
        // Test 1: Simple list of URLs
        """
        https://github.com/JetBrains/kotlin
        https://developer.android.com/jetpack/compose
        https://kotlinlang.org/docs/home.html
        https://medium.com/androiddevelopers/effective-state-management-for-jetpack-compose-d7e1bd9c1c5a
        https://stackoverflow.com/questions/tagged/android
        """.trimIndent(),

        // Test 2: Mixed text with URLs
        """
        Check out these amazing resources:
        - Kotlin documentation: https://kotlinlang.org/
        - Android guide at developer.android.com/guide
        - Great blog post: https://proandroiddev.com/jetpack-compose-best-practices
        Also visit https://github.com/android for official samples
        Don't forget reddit.com/r/androiddev for community discussions
        """.trimIndent(),

        // Test 3: URLs in sentences
        """
        I found this great article on https://medium.com/@dev/android-tips yesterday.
        You should also read https://blog.jetbrains.com/kotlin/ for the latest updates.
        The official guide is at developer.android.com/guide/components but there's
        also a good tutorial at https://www.raywenderlich.com/android-tutorials.
        For design inspiration, check out material.io/design.
        """.trimIndent(),

        // Test 4: Multiple URLs per line
        """
        Resources: https://github.com/topics/android, https://stackoverflow.com, developer.android.com
        Blogs: medium.com/tag/android, dev.to/t/android, https://androidweekly.net
        Libraries: github.com/square/retrofit, github.com/coil-kt/coil
        """.trimIndent(),

        // Test 5: URLs with different formats
        """
        https://www.youtube.com/watch?v=abc123
        github.com/user/repo/issues/42
        http://old-site.com/page.html
        www.example.com/path/to/resource
        subdomain.example.com/api/v1/endpoint
        https://docs.google.com/document/d/abc123/edit
        """.trimIndent(),

        // Test 6: News articles and blogs
        """
        Breaking: New Android version announced! https://android-developers.googleblog.com/2024/01/android-15-preview

        Tech news from https://techcrunch.com/android and https://theverge.com/tech

        Tutorial I'm following: https://www.udacity.com/course/android-basics

        Community: reddit.com/r/android, xda-developers.com
        """.trimIndent(),

        // Test 7: Development resources
        """
        Official docs: developer.android.com/docs

        Libraries to check:
        • Networking: github.com/square/retrofit
        • Images: github.com/coil-kt/coil
        • DI: insert-koin.io
        • Database: developer.android.com/training/data-storage/room

        Tutorials: https://www.vogella.com/tutorials/android.html
        """.trimIndent(),

        // Test 8: Mixed content with duplicates (for testing duplicate detection)
        """
        Important links for the project:

        Backend API: https://api.example.com/v2/docs
        Frontend repo: github.com/company/frontend
        Design system: https://www.figma.com/file/abc123

        Also see:
        https://api.example.com/v2/docs (same as above)
        Backup link: github.com/company/frontend

        Slack: company.slack.com/archives/C123
        """.trimIndent(),

        // Test 9: Social media and video links
        """
        Follow these channels:
        YouTube tutorial: https://www.youtube.com/watch?v=dQw4w9WgXcQ
        Twitter updates: twitter.com/androiddev
        LinkedIn post: https://www.linkedin.com/posts/user_android-mobile-dev

        Conference talk: https://www.youtube.com/watch?v=example
        Slides: speakerdeck.com/user/presentation
        """.trimIndent(),

        // Test 10: E-commerce and mixed domains
        """
        Shopping list:
        Phone case: https://www.amazon.com/dp/B08XYZ123
        Screen protector: amazon.com/gp/product/B09ABC456

        Reviews:
        https://www.cnet.com/reviews/android-phones/
        gsmarena.com/samsung_galaxy_s24-review-123.php

        Price comparison: camelcamelcamel.com
        Deals: slickdeals.net/android
        """.trimIndent(),

        // Test 11: Android development tools
        """
        My Android dev toolkit:

        IDE: https://developer.android.com/studio
        Emulator alternatives: https://www.genymotion.com/

        Testing:
        - https://firebase.google.com/products/test-lab
        - https://appium.io/

        CI/CD: https://bitrise.io/, https://circleci.com/
        Analytics: https://firebase.google.com/products/analytics
        """.trimIndent(),

        // Test 12: Design resources
        """
        Design inspiration for the app:

        Icons: https://fonts.google.com/icons
        Colors: https://m3.material.io/styles/color/overview
        Typography: https://fonts.google.com/

        Figma plugins:
        - figma.com/community/plugin/material-design
        - figma.com/community/plugin/iconify

        Dribbble: https://dribbble.com/tags/android
        """.trimIndent(),

        // Test 13: Documentation links
        """
        Project documentation:

        API Reference: https://api.myproject.com/docs/v2
        Wiki: https://github.com/myorg/myproject/wiki
        Changelog: github.com/myorg/myproject/blob/main/CHANGELOG.md

        External docs:
        https://kotlinlang.org/api/latest/jvm/stdlib/
        https://square.github.io/okhttp/
        https://insert-koin.io/docs/quickstart/kotlin
        """.trimIndent(),

        // Test 14: Podcast and learning links
        """
        Podcasts I listen to:

        Android Developers Backstage: https://adbackstage.libsyn.com/
        Fragmented: https://fragmentedpodcast.com/
        Talking Kotlin: https://talkingkotlin.com/

        Courses:
        https://www.udacity.com/course/developing-android-apps-with-kotlin
        https://developer.android.com/courses
        """.trimIndent(),

        // Test 15: Open source projects
        """
        Interesting open source Android apps:

        https://github.com/nickcedwards/android-architecture-samples
        https://github.com/android/nowinandroid
        https://github.com/android/sunflower
        https://github.com/chrisbanes/tivi
        https://github.com/skydoves/Pokedex

        Libraries I use:
        github.com/airbnb/lottie-android
        github.com/bumptech/glide
        """.trimIndent()
    )

    return testDataList[Random.nextInt(testDataList.size)]
}

/**
 * Get large test data with 150+ links for load testing
 * Contains URLs across various categories
 */
private fun getLargeTestData(): String {
    return """
    📚 COMPREHENSIVE WEB DEVELOPMENT & ANDROID RESOURCES COLLECTION

    This is a curated list of essential resources gathered from various sources.
    Perfect for testing link import functionality with a large dataset.

    ═══════════════════════════════════════════════════════════════════════════════
    ANDROID DEVELOPMENT
    ═══════════════════════════════════════════════════════════════════════════════

    Official Documentation:
    https://developer.android.com/
    https://developer.android.com/jetpack/compose
    https://developer.android.com/kotlin
    https://developer.android.com/studio
    https://developer.android.com/guide
    https://developer.android.com/reference
    https://developer.android.com/training
    https://developer.android.com/courses
    https://developer.android.com/games
    https://developer.android.com/health-and-fitness

    Jetpack Libraries:
    https://developer.android.com/jetpack/androidx/releases/room
    https://developer.android.com/jetpack/androidx/releases/navigation
    https://developer.android.com/jetpack/androidx/releases/lifecycle
    https://developer.android.com/jetpack/androidx/releases/work
    https://developer.android.com/jetpack/androidx/releases/paging
    https://developer.android.com/jetpack/androidx/releases/datastore
    https://developer.android.com/jetpack/androidx/releases/hilt

    ═══════════════════════════════════════════════════════════════════════════════
    KOTLIN
    ═══════════════════════════════════════════════════════════════════════════════

    Official Resources:
    https://kotlinlang.org/
    https://kotlinlang.org/docs/home.html
    https://kotlinlang.org/docs/coroutines-overview.html
    https://kotlinlang.org/docs/flow.html
    https://kotlinlang.org/docs/multiplatform.html
    https://play.kotlinlang.org/
    https://kotlinlang.org/api/latest/jvm/stdlib/

    JetBrains:
    https://blog.jetbrains.com/kotlin/
    https://www.jetbrains.com/kotlin/
    https://www.jetbrains.com/idea/

    ═══════════════════════════════════════════════════════════════════════════════
    FRONTEND FRAMEWORKS
    ═══════════════════════════════════════════════════════════════════════════════

    React:
    https://react.dev/
    https://react.dev/learn
    https://react.dev/reference/react
    https://nextjs.org/
    https://nextjs.org/docs
    https://remix.run/
    https://remix.run/docs

    Vue:
    https://vuejs.org/
    https://vuejs.org/guide/introduction.html
    https://nuxt.com/
    https://nuxt.com/docs

    Angular:
    https://angular.io/
    https://angular.io/docs
    https://angular.io/tutorial

    Other:
    https://svelte.dev/
    https://svelte.dev/tutorial
    https://kit.svelte.dev/
    https://docs.astro.build/
    https://solidjs.com/

    ═══════════════════════════════════════════════════════════════════════════════
    BACKEND & APIs
    ═══════════════════════════════════════════════════════════════════════════════

    Node.js:
    https://nodejs.org/
    https://nodejs.org/docs/latest/api/
    https://expressjs.com/
    https://expressjs.com/en/guide/routing.html
    https://fastify.io/
    https://nestjs.com/

    Python:
    https://docs.djangoproject.com/
    https://flask.palletsprojects.com/
    https://fastapi.tiangolo.com/
    https://www.python.org/doc/

    Other:
    https://guides.rubyonrails.org/
    https://laravel.com/docs
    https://docs.spring.io/spring-boot/docs/current/reference/html/
    https://go.dev/doc/
    https://doc.rust-lang.org/

    ═══════════════════════════════════════════════════════════════════════════════
    LEARNING PLATFORMS
    ═══════════════════════════════════════════════════════════════════════════════

    Free Resources:
    https://www.freecodecamp.org/
    https://www.freecodecamp.org/learn
    https://www.khanacademy.org/computing/computer-programming
    https://www.theodinproject.com/
    https://fullstackopen.com/en/

    Paid Platforms:
    https://www.udemy.com/
    https://www.coursera.org/
    https://www.edx.org/
    https://www.pluralsight.com/
    https://egghead.io/
    https://frontendmasters.com/
    https://www.udacity.com/
    https://www.codecademy.com/

    ═══════════════════════════════════════════════════════════════════════════════
    DEVELOPER COMMUNITIES
    ═══════════════════════════════════════════════════════════════════════════════

    Forums:
    https://stackoverflow.com/
    https://stackoverflow.com/questions/tagged/android
    https://stackoverflow.com/questions/tagged/kotlin
    https://www.reddit.com/r/androiddev/
    https://www.reddit.com/r/Kotlin/
    https://www.reddit.com/r/webdev/
    https://www.reddit.com/r/programming/

    Social:
    https://dev.to/
    https://hashnode.com/
    https://news.ycombinator.com/
    https://lobste.rs/
    https://twitter.com/AndroidDev
    https://twitter.com/kotlin

    ═══════════════════════════════════════════════════════════════════════════════
    GITHUB REPOSITORIES
    ═══════════════════════════════════════════════════════════════════════════════

    Android:
    https://github.com/android
    https://github.com/android/nowinandroid
    https://github.com/android/sunflower
    https://github.com/android/architecture-samples
    https://github.com/android/compose-samples
    https://github.com/android/camera-samples

    Kotlin:
    https://github.com/JetBrains/kotlin
    https://github.com/Kotlin/kotlinx.coroutines
    https://github.com/Kotlin/kotlinx.serialization

    Popular Libraries:
    https://github.com/square/retrofit
    https://github.com/square/okhttp
    https://github.com/coil-kt/coil
    https://github.com/bumptech/glide
    https://github.com/airbnb/lottie-android
    https://github.com/InsertKoinIO/koin
    https://github.com/google/dagger

    ═══════════════════════════════════════════════════════════════════════════════
    CSS & DESIGN
    ═══════════════════════════════════════════════════════════════════════════════

    CSS Resources:
    https://css-tricks.com/
    https://www.smashingmagazine.com/category/css/
    https://developer.mozilla.org/en-US/docs/Web/CSS
    https://web.dev/learn/css/
    https://cssreference.io/

    CSS Frameworks:
    https://tailwindcss.com/
    https://tailwindcss.com/docs
    https://getbootstrap.com/
    https://getbootstrap.com/docs/
    https://bulma.io/
    https://chakra-ui.com/

    Design Systems:
    https://m3.material.io/
    https://m3.material.io/develop/android/jetpack-compose
    https://developer.apple.com/design/human-interface-guidelines/
    https://ant.design/
    https://www.radix-ui.com/

    ═══════════════════════════════════════════════════════════════════════════════
    TESTING & QUALITY
    ═══════════════════════════════════════════════════════════════════════════════

    Testing Frameworks:
    https://jestjs.io/
    https://jestjs.io/docs/getting-started
    https://vitest.dev/
    https://vitest.dev/guide/
    https://docs.cypress.io/
    https://playwright.dev/
    https://testing-library.com/

    Android Testing:
    https://developer.android.com/training/testing
    https://developer.android.com/training/testing/espresso
    https://developer.android.com/training/testing/junit-rules
    https://mockk.io/

    ═══════════════════════════════════════════════════════════════════════════════
    DEPLOYMENT & HOSTING
    ═══════════════════════════════════════════════════════════════════════════════

    Platforms:
    https://vercel.com/
    https://vercel.com/docs
    https://www.netlify.com/
    https://docs.netlify.com/
    https://railway.app/
    https://render.com/
    https://fly.io/

    Cloud Providers:
    https://aws.amazon.com/
    https://docs.aws.amazon.com/
    https://cloud.google.com/
    https://cloud.google.com/docs
    https://azure.microsoft.com/
    https://docs.microsoft.com/azure/
    https://www.digitalocean.com/

    ═══════════════════════════════════════════════════════════════════════════════
    DATABASES
    ═══════════════════════════════════════════════════════════════════════════════

    SQL:
    https://www.postgresql.org/
    https://www.postgresql.org/docs/
    https://dev.mysql.com/
    https://dev.mysql.com/doc/
    https://www.sqlite.org/

    NoSQL:
    https://www.mongodb.com/
    https://docs.mongodb.com/
    https://redis.io/
    https://redis.io/documentation

    Mobile/Cloud:
    https://firebase.google.com/
    https://firebase.google.com/docs
    https://supabase.com/
    https://supabase.com/docs
    https://planetscale.com/

    ═══════════════════════════════════════════════════════════════════════════════
    TOOLS & UTILITIES
    ═══════════════════════════════════════════════════════════════════════════════

    Developer Tools:
    https://caniuse.com/
    https://regexr.com/
    https://jsonformatter.org/
    https://codebeautify.org/
    https://www.epochconverter.com/
    https://www.base64encode.org/
    https://htmlcolorcodes.com/color-picker/

    Design Tools:
    https://www.figma.com/
    https://fonts.google.com/
    https://fonts.google.com/icons
    https://unsplash.com/
    https://www.pexels.com/

    ═══════════════════════════════════════════════════════════════════════════════
    NEWS & BLOGS
    ═══════════════════════════════════════════════════════════════════════════════

    Tech News:
    https://www.theverge.com/
    https://techcrunch.com/
    https://www.wired.com/
    https://arstechnica.com/
    https://www.engadget.com/

    Developer Blogs:
    https://overreacted.io/
    https://kentcdodds.com/blog
    https://www.joshwcomeau.com/
    https://leerob.io/blog
    https://jakewharton.com/

    Android Blogs:
    https://android-developers.googleblog.com/
    https://proandroiddev.com/
    https://androidweekly.net/

    ═══════════════════════════════════════════════════════════════════════════════
    YOUTUBE CHANNELS
    ═══════════════════════════════════════════════════════════════════════════════

    Web Development:
    https://www.youtube.com/@Fireship
    https://www.youtube.com/@TraversyMedia
    https://www.youtube.com/@WebDevSimplified
    https://www.youtube.com/@NetNinja
    https://www.youtube.com/@KevinPowell

    Android:
    https://www.youtube.com/@AndroidDevelopers
    https://www.youtube.com/@PhilippLackner
    https://www.youtube.com/@CodingWithMitch
    https://www.youtube.com/@stevdza-san

    ═══════════════════════════════════════════════════════════════════════════════
    API RESOURCES
    ═══════════════════════════════════════════════════════════════════════════════

    Public APIs:
    https://github.com/public-apis/public-apis
    https://rapidapi.com/
    https://any-api.com/
    https://apilist.fun/

    API Tools:
    https://www.postman.com/
    https://insomnia.rest/
    https://hoppscotch.io/

    ═══════════════════════════════════════════════════════════════════════════════
    CODE CHALLENGES
    ═══════════════════════════════════════════════════════════════════════════════

    Practice Platforms:
    https://leetcode.com/
    https://www.hackerrank.com/
    https://www.codewars.com/
    https://exercism.org/
    https://projecteuler.net/
    https://adventofcode.com/
    https://www.codingame.com/

    ═══════════════════════════════════════════════════════════════════════════════

    This collection contains 200+ unique URLs across various categories.
    Use this to test batch import performance, duplicate detection, and URL extraction.
    """.trimIndent()
}
