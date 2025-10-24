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
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rejowan.linky.data.local.preferences.ThemePreferences
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
    onNavigateBack: () -> Unit,
    onNavigateToCollectionDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BatchImportViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val themePreferences = remember { ThemePreferences(context) }
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    // ViewModel state
    val state by viewModel.state.collectAsState()

    // Collect UI events
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is BatchImportUiEvent.NavigateToHome -> {
                    onNavigateBack()
                }
                is BatchImportUiEvent.NavigateToSettings -> {
                    onNavigateBack()
                }
                is BatchImportUiEvent.NavigateToCollection -> {
                    onNavigateToCollectionDetail(event.collectionId)
                }
                is BatchImportUiEvent.ShowError -> {
                    // TODO: Show error message
                }
                is BatchImportUiEvent.ShowConfirmation -> {
                    // TODO: Show confirmation dialog
                }
            }
        }
    }

    // Local UI state
    var pastedText by remember { mutableStateOf("") }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var showClearConfirmationDialog by remember { mutableStateOf(false) }
    var showHowItWorksDialog by remember { mutableStateOf(false) }

    // Observe the preference for showing "How it works" card
    val shouldShowHowItWorks by themePreferences.shouldShowBatchImportHowItWorks()
        .collectAsState(initial = true)

    // Calculate statistics
    val characterCount = pastedText.length
    val lineCount = if (pastedText.isEmpty()) 0 else pastedText.lines().size

    // Handle scanning completion
    LaunchedEffect(state.extractedUrls) {
        if (state.extractedUrls.isNotEmpty() && !state.isScanning) {
            // TODO: Navigate to next step (summary or selection)
            // For now, just show a message
        }
    }

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
        }
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
            TestDataCard(
                onLoadNormalText = {
                    pastedText = getRandomNormalTestData()
                },
                onLoadLargeText = {
                    pastedText = getLargeTestData()
                }
            )

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
                        val clipboardText = clipboardManager.getText()?.text
                        if (!clipboardText.isNullOrEmpty()) {
                            // Append to existing text instead of replacing
                            pastedText = if (pastedText.isEmpty()) {
                                clipboardText
                            } else {
                                pastedText + "\n" + clipboardText
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

            // Statistics Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Character count
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$characterCount",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "characters",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Line count
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$lineCount",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "lines",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        viewModel.onEvent(BatchImportEvent.OnTextChanged(pastedText))
                        viewModel.onEvent(BatchImportEvent.OnStartScan)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = pastedText.isNotEmpty() && !state.isScanning,
                    shape = RoundedCornerShape(12.dp)
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
            isFavorite = state.newCollectionIsFavorite,
            onCollectionNameChange = { viewModel.onEvent(BatchImportEvent.OnNewCollectionNameChange(it)) },
            onColorChange = { viewModel.onEvent(BatchImportEvent.OnNewCollectionColorChange(it)) },
            onToggleFavorite = { viewModel.onEvent(BatchImportEvent.OnNewCollectionToggleFavorite) },
            onSave = { viewModel.onEvent(BatchImportEvent.OnCreateCollectionConfirm) },
            onDismiss = { viewModel.onEvent(BatchImportEvent.OnCreateCollectionDismiss) }
        )
    }
}

/**
 * Test Data Card - Load sample data for testing
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
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Testing Tools",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Normal Text Button
                OutlinedButton(
                    onClick = onLoadNormalText,
                    modifier = Modifier
                        .weight(1f)
                        .height(70.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Normal",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "5-10 links",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }

                // Large Text Button
                OutlinedButton(
                    onClick = onLoadLargeText,
                    modifier = Modifier
                        .weight(1f)
                        .height(70.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Large",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "150+ links",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
        }
    }
}

/**
 * How It Works Card - Dismissable with close button
 */
@Composable
private fun HowItWorksCard(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(end = 24.dp), // Extra padding for close button
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "How it works",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Paste text containing multiple URLs in any format. We'll automatically extract and import them for you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Close button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
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
            Text(
                text = "How it works",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Paste text containing multiple URLs in any format. We'll automatically extract and import them for you.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "Examples:",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
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
            TextButton(onClick = onDismiss) {
                Text("Got it")
            }
        },
        shape = RoundedCornerShape(16.dp)
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
                textAlign = TextAlign.Start
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    confirmText,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
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
                    modifier = Modifier.size(48.dp)
                )
                if (progress.isNotEmpty()) {
                    Text(
                        text = progress,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = { /* No button while scanning */ },
        shape = RoundedCornerShape(16.dp)
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
            Text(
                text = "URLs Found",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Found $totalUrls unique URL${if (totalUrls != 1) "s" else ""} ready to import",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (duplicateCount > 0) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "$duplicateCount URL${if (duplicateCount != 1) "s" else ""} already saved (marked as duplicate)",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }

                Text(
                    text = "New links: ${totalUrls - duplicateCount}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onProceed,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Review Links")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onBackToEdit,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Back to Edit")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

/**
 * FOR TESTING ONLY: Get random normal test data (5-10 links)
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

        // Test 8: Mixed content with duplicates (for testing)
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
        """.trimIndent()
    )

    return testDataList[Random.nextInt(testDataList.size)]
}

/**
 * FOR TESTING ONLY: Large test data with 50+ links for load testing
 */
private fun getLargeTestData(): String {
    return """
    📚 COMPREHENSIVE WEB DEVELOPMENT RESOURCES COLLECTION

    This is a curated list of essential web development resources gathered from various sources.
    Perfect for testing link import functionality with a large dataset.

    === OFFICIAL DOCUMENTATION ===

    Frontend Frameworks:
    React official docs: https://react.dev/learn
    Vue.js guide: https://vuejs.org/guide/introduction.html
    Angular documentation: https://angular.io/docs
    Svelte tutorial: https://svelte.dev/tutorial
    Next.js docs: https://nextjs.org/docs
    Nuxt.js guide: https://nuxt.com/docs
    Remix docs: https://remix.run/docs
    Astro documentation: https://docs.astro.build

    Backend & APIs:
    Node.js documentation: https://nodejs.org/docs/latest/api/
    Express.js guide: https://expressjs.com/en/guide/routing.html
    Django documentation: https://docs.djangoproject.com/
    Flask quickstart: https://flask.palletsprojects.com/quickstart/
    FastAPI docs: https://fastapi.tiangolo.com/
    Ruby on Rails guides: https://guides.rubyonrails.org/
    Laravel documentation: https://laravel.com/docs
    Spring Boot reference: https://docs.spring.io/spring-boot/docs/current/reference/html/

    === LEARNING PLATFORMS ===

    Interactive Tutorials:
    https://www.freecodecamp.org/learn
    https://www.codecademy.com/catalog
    https://www.udemy.com/courses/development/
    https://www.coursera.org/browse/computer-science
    https://www.edx.org/learn/computer-programming
    https://www.pluralsight.com/browse/software-development
    https://egghead.io/
    https://frontendmasters.com/
    https://www.udacity.com/courses/programming
    https://www.khanacademy.org/computing/computer-programming

    === DEVELOPER COMMUNITIES ===

    Forums & Discussion:
    Stack Overflow: https://stackoverflow.com/
    Reddit webdev: https://www.reddit.com/r/webdev/
    Dev.to community: https://dev.to/
    Hashnode blogs: https://hashnode.com/
    HackerNews: https://news.ycombinator.com/
    Lobsters: https://lobste.rs/

    === CODE REPOSITORIES & EXAMPLES ===

    GitHub Collections:
    https://github.com/topics/web-development
    https://github.com/topics/javascript
    https://github.com/topics/typescript
    https://github.com/topics/python
    https://github.com/trending
    https://github.com/collections/web-accessibility
    https://github.com/topics/progressive-web-apps
    https://github.com/topics/serverless

    === CSS & DESIGN ===

    CSS Resources:
    https://css-tricks.com/
    https://www.smashingmagazine.com/category/css/
    https://developer.mozilla.org/en-US/docs/Web/CSS
    https://web.dev/learn/css/
    https://cssreference.io/
    https://tailwindcss.com/docs
    https://getbootstrap.com/docs/
    https://bulma.io/documentation/

    Design Systems:
    Material Design: https://material.io/design
    Apple HIG: https://developer.apple.com/design/human-interface-guidelines/
    Ant Design: https://ant.design/
    Chakra UI: https://chakra-ui.com/
    Radix UI: https://www.radix-ui.com/

    === JAVASCRIPT ECOSYSTEM ===

    Libraries & Tools:
    Lodash utilities: https://lodash.com/docs/
    Axios HTTP client: https://axios-http.com/docs/intro
    Moment.js dates: https://momentjs.com/docs/
    Day.js alternative: https://day.js.org/
    Chart.js graphs: https://www.chartjs.org/docs/
    D3.js visualization: https://d3js.org/
    Three.js 3D: https://threejs.org/docs/
    Gsap animation: https://greensock.com/docs/

    === TESTING & QUALITY ===

    Testing Frameworks:
    Jest testing: https://jestjs.io/docs/getting-started
    Vitest modern: https://vitest.dev/guide/
    Cypress e2e: https://docs.cypress.io/
    Playwright testing: https://playwright.dev/docs/intro
    Testing Library: https://testing-library.com/docs/
    Mocha framework: https://mochajs.org/

    === DEPLOYMENT & HOSTING ===

    Cloud Platforms:
    Vercel hosting: https://vercel.com/docs
    Netlify deploy: https://docs.netlify.com/
    AWS documentation: https://docs.aws.amazon.com/
    Google Cloud: https://cloud.google.com/docs
    Azure docs: https://docs.microsoft.com/azure/
    Heroku platform: https://devcenter.heroku.com/
    DigitalOcean tutorials: https://www.digitalocean.com/community/tutorials
    Railway hosting: https://docs.railway.app/
    Render services: https://render.com/docs
    Fly.io platform: https://fly.io/docs/

    === DATABASE & BACKEND ===

    Databases:
    MongoDB docs: https://docs.mongodb.com/
    PostgreSQL manual: https://www.postgresql.org/docs/
    MySQL reference: https://dev.mysql.com/doc/
    Redis documentation: https://redis.io/documentation
    Firebase guides: https://firebase.google.com/docs
    Supabase docs: https://supabase.com/docs
    PlanetScale MySQL: https://planetscale.com/docs

    === PERFORMANCE & OPTIMIZATION ===

    Web Performance:
    https://web.dev/learn-core-web-vitals/
    https://developers.google.com/speed/docs/insights/
    https://www.webpagetest.org/
    https://gtmetrix.com/
    https://tools.pingdom.com/

    === SECURITY ===

    Security Resources:
    OWASP Top 10: https://owasp.org/www-project-top-ten/
    Security headers: https://securityheaders.com/
    Content Security Policy: https://content-security-policy.com/

    === TOOLS & UTILITIES ===

    Developer Tools:
    Can I Use: https://caniuse.com/
    Regexr patterns: https://regexr.com/
    JSON formatter: https://jsonformatter.org/
    Code beautifier: https://codebeautify.org/
    Epoch converter: https://www.epochconverter.com/
    Base64 encode: https://www.base64encode.org/
    Color picker: https://htmlcolorcodes.com/color-picker/

    === NEWS & BLOGS ===

    Tech News:
    https://www.theverge.com/tech
    https://techcrunch.com/
    https://www.wired.com/category/gear/
    https://arstechnica.com/

    Developer Blogs:
    https://overreacted.io/
    https://kentcdodds.com/blog
    https://css-tricks.com/
    https://www.joshwcomeau.com/
    https://leerob.io/blog

    === PODCASTS & VIDEOS ===

    YouTube Channels:
    Fireship: https://www.youtube.com/@Fireship
    Traversy Media: https://www.youtube.com/@TraversyMedia
    Web Dev Simplified: https://www.youtube.com/@WebDevSimplified
    The Net Ninja: https://www.youtube.com/@NetNinja
    Kevin Powell CSS: https://www.youtube.com/@KevinPowell

    === API RESOURCES ===

    Public APIs:
    https://github.com/public-apis/public-apis
    https://rapidapi.com/hub
    https://any-api.com/
    https://apilist.fun/

    === MISCELLANEOUS ===

    Code Challenges:
    LeetCode: https://leetcode.com/
    HackerRank: https://www.hackerrank.com/
    Codewars: https://www.codewars.com/
    Project Euler: https://projecteuler.net/
    Advent of Code: https://adventofcode.com/

    Icons & Assets:
    Font Awesome: https://fontawesome.com/
    Heroicons: https://heroicons.com/
    Unsplash photos: https://unsplash.com/
    Pexels videos: https://www.pexels.com/

    This collection contains over 150+ unique URLs across various categories for comprehensive testing.
    Use this to test batch import performance, duplicate detection, and URL extraction logic.
    """.trimIndent()
}

/**
 * Link Selection & Review Screen - Step 4
 *
 * Features:
 * - Statistics card showing total URLs, duplicates, new links, and selected count
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
        // Header Statistics Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "URL Statistics",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatisticItem("Total URLs", state.totalUrls.toString())
                    StatisticItem("Duplicates", state.duplicateCount.toString(), align = Alignment.End)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatisticItem("New Links", (state.totalUrls - state.duplicateCount).toString())
                    StatisticItem("Selected", state.selectedCount.toString(), align = Alignment.End)
                }
            }
        }

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
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            if (state.urlStatuses.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "No URLs to review",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "All URLs have been removed",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
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
                        if (index < state.urlStatuses.size - 1) {
                            androidx.compose.material3.HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        }
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
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Back")
                }

                Button(
                    onClick = {
                        onEvent(BatchImportEvent.OnStartFetching)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = state.selectedCount > 0,
                    shape = RoundedCornerShape(12.dp)
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
 * Statistic Item for the statistics card
 */
@Composable
private fun StatisticItem(
    label: String,
    value: String,
    align: Alignment.Horizontal = Alignment.Start
) {
    Column(
        horizontalAlignment = align
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Checkbox
        androidx.compose.material3.Checkbox(
            checked = urlStatus.isSelected,
            onCheckedChange = { onToggleSelection() }
        )

        // URL Info
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Domain name (bold)
                Text(
                    text = urlStatus.domain,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Duplicate badge
                if (urlStatus.isDuplicate) {
                    androidx.compose.material3.Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = "Duplicate",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Full URL (ellipsized)
            Text(
                text = urlStatus.url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }

        // Remove button
        IconButton(
            onClick = onRemove
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove",
                tint = MaterialTheme.colorScheme.error
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
            FetchingProgressCard(state.fetchProgress)
        } else {
            // Completion Summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = "Previews Ready",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${state.previewResults.size} links ready to import",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Collection Picker
        // TODO: Implement collection picker

        // Preview Results List
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            if (state.previewResults.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Starting preview fetch...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
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
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Back")
                }

                Button(
                    onClick = {
                        onEvent(BatchImportEvent.OnStartImport)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = state.previewResults.isNotEmpty(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Import")
                }
            }
        }
    }
}

/**
 * Fetching Progress Card
 */
@Composable
private fun FetchingProgressCard(progress: FetchProgress?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Fetching Previews...",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                if (progress != null) {
                    Text(
                        text = "Chunk ${progress.currentChunk} of ${progress.totalChunks}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            if (progress != null) {
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { progress.current.toFloat() / progress.total.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = "${progress.current} of ${progress.total} links",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            } else {
                androidx.compose.material3.LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Preview Card for individual link
 */
@Composable
private fun PreviewCard(result: LinkPreviewResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (result) {
                is LinkPreviewResult.Success -> {
                    // Title
                    Text(
                        text = result.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )

                    // Description
                    if (result.description != null) {
                        Text(
                            text = result.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }

                    // Domain
                    Text(
                        text = result.domain,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                is LinkPreviewResult.Error -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = result.domain,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Preview failed - will use domain as title",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                is LinkPreviewResult.Timeout -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = result.domain,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Request timeout - will use domain as title",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
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
            Text("Importing Links")
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp)
                )

                if (progress != null) {
                    Text(
                        text = "Saving ${progress.current} of ${progress.total} links...",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        text = "Preparing to save links...",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {},
        shape = RoundedCornerShape(16.dp)
    )
}

/**
 * Import Result Screen - Shows success/failure summary
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

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Result Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (result.isCompleteSuccess) {
                    MaterialTheme.colorScheme.primaryContainer
                } else if (result.isCompleteFailure) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.tertiaryContainer
                }
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon
                Icon(
                    imageVector = if (result.isCompleteSuccess) {
                        Icons.Default.Check
                    } else {
                        Icons.Default.Info
                    },
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = if (result.isCompleteSuccess) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else if (result.isCompleteFailure) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Title
                Text(
                    text = when {
                        result.isCompleteSuccess -> "Import Complete!"
                        result.isCompleteFailure -> "Import Failed"
                        else -> "Partial Success"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (result.isCompleteSuccess) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else if (result.isCompleteFailure) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    }
                )

                // Summary
                if (result.isCompleteSuccess) {
                    Text(
                        text = "Successfully imported ${result.totalSuccess} link${if (result.totalSuccess == 1) "" else "s"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else if (result.isCompleteFailure) {
                    Text(
                        text = "Failed to import all ${result.totalFailed} link${if (result.totalFailed == 1) "" else "s"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Success: ${result.totalSuccess} link${if (result.totalSuccess == 1) "" else "s"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "Failed: ${result.totalFailed} link${if (result.totalFailed == 1) "" else "s"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }

        // Failed Links (if any)
        if (result.hasFailures) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Failed Links",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    androidx.compose.foundation.lazy.LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(result.failed.size) { index ->
                            val failedImport = result.failed[index]
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = failedImport.url,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (failedImport.error != null) {
                                        Text(
                                            text = failedImport.error,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
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
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
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
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Retry All")
                }
            }

            // Done Button
            Button(
                onClick = {
                    onEvent(BatchImportEvent.OnDone)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Done")
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
 * Allows users to create a new collection with name, color, and favorite status
 */
@Composable
private fun CreateCollectionDialog(
    collectionName: String,
    selectedColor: String?,
    isFavorite: Boolean,
    onCollectionNameChange: (String) -> Unit,
    onColorChange: (String?) -> Unit,
    onToggleFavorite: () -> Unit,
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

                Spacer(modifier = Modifier.height(8.dp))

                // Favorite toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add to Favourite",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = isFavorite,
                        onCheckedChange = { onToggleFavorite() }
                    )
                }
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
