package com.rejowan.linky.presentation.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rejowan.linky.data.local.preferences.ThemePreferences
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * App Features Screen - Control app behavior and features
 *
 * Features:
 * - Toggle clipboard auto-checking
 * - Future: Additional feature controls can be added here
 *
 * @param onNavigateBack Callback to navigate back
 * @param modifier Modifier for styling
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppFeaturesScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val themePreferences = remember { ThemePreferences(context) }
    val coroutineScope = rememberCoroutineScope()

    // Collect clipboard checking preference
    val isClipboardCheckingEnabled by themePreferences.isClipboardCheckingEnabled()
        .collectAsState(initial = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "App Features",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 22.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Clipboard Checking Toggle
            item {
                FeatureToggleCard(
                    icon = Icons.Filled.ContentPaste,
                    title = "Auto Clipboard Checking",
                    description = "Automatically detect URLs copied to clipboard when app resumes",
                    checked = isClipboardCheckingEnabled,
                    onCheckedChange = { enabled ->
                        Timber.tag("AppFeatures").d("Toggle changed to: $enabled")
                        coroutineScope.launch {
                            themePreferences.setClipboardCheckingEnabled(enabled)
                            Timber.tag("AppFeatures").d("Preference saved: $enabled")
                        }
                    }
                )
            }

            // Future feature toggles can be added here
            // Example:
            // item {
            //     FeatureToggleCard(
            //         icon = Icons.Filled.Notifications,
            //         title = "Link Notifications",
            //         description = "Get notified about important link updates",
            //         checked = isNotificationsEnabled,
            //         onCheckedChange = { enabled -> ... }
            //     )
            // }
        }
    }
}

/**
 * Feature toggle card component
 * Card with toggle switch for enabling/disabling features
 */
@Composable
private fun FeatureToggleCard(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            // Title and Description
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Toggle Switch
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}
