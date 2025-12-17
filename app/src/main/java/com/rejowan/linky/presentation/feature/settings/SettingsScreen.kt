package com.rejowan.linky.presentation.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Settings Screen - Main hub for all settings categories
 *
 * Features:
 * - Categorized settings navigation with 5 sections
 * - Tools, Preferences, Data, Security, and Information
 * - Clean Material 3 design
 *
 * @param onNavigateToAppFeatures Navigate to App Features screen
 * @param onNavigateToDataStorage Navigate to Data & Storage screen
 * @param onNavigateToAppearance Navigate to Appearance screen
 * @param onNavigateToPrivacySecurity Navigate to Privacy & Security screen
 * @param onNavigateToAbout Navigate to About screen
 * @param onNavigateToSync Navigate to Sync settings (Phase 2)
 * @param onNavigateToBatchImport Navigate to Batch Import screen
 * @param onNavigateToTagManagement Navigate to Tag Management screen
 * @param modifier Modifier for styling
 */
@Composable
fun SettingsScreen(
    onNavigateToAppFeatures: () -> Unit,
    onNavigateToDataStorage: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToPrivacySecurity: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToSync: () -> Unit,
    onNavigateToBatchImport: () -> Unit,
    onNavigateToTagManagement: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 1. Tools Section
    val toolsSection = listOf(
        SettingsCategory(
            icon = Icons.Filled.FileUpload,
            title = "Batch Import",
            description = "Import multiple links at once from text",
            onClick = onNavigateToBatchImport
        ),
        SettingsCategory(
            icon = Icons.AutoMirrored.Filled.Label,
            title = "Manage Tags",
            description = "Create, edit, and organize tags",
            onClick = onNavigateToTagManagement
        ),
        SettingsCategory(
            icon = Icons.Filled.Folder,
            title = "Link Vaults",
            description = "Secure storage for private collections",
            onClick = { /* Placeholder for Phase 2 */ },
            badge = "Soon"
        )
    )

    // 2. Preferences Section
    val preferencesSection = listOf(
        SettingsCategory(
            icon = Icons.Filled.Settings,
            title = "App Features",
            description = "Control app behavior and features",
            onClick = onNavigateToAppFeatures
        ),
        SettingsCategory(
            icon = Icons.Filled.Palette,
            title = "Appearance",
            description = "Theme, View options, Card design",
            onClick = onNavigateToAppearance
        )
    )

    // 3. Data Section
    val dataSection = listOf(
        SettingsCategory(
            icon = Icons.Filled.Cloud,
            title = "Sync",
            description = "Not logged in • Tap to configure",
            onClick = onNavigateToSync,
            badge = "Phase 2"
        ),
        SettingsCategory(
            icon = Icons.Filled.Storage,
            title = "Data & Storage",
            description = "Storage usage, Trash, Export/Import",
            onClick = onNavigateToDataStorage
        )
    )

    // 4. Security Section
    val securitySection = listOf(
        SettingsCategory(
            icon = Icons.Filled.Lock,
            title = "Privacy & Security",
            description = "Privacy policy, Data management",
            onClick = onNavigateToPrivacySecurity
        )
    )

    // 5. Information Section
    val informationSection = listOf(
        SettingsCategory(
            icon = Icons.Filled.Info,
            title = "About",
            description = "Version, Licenses, Credits, Feedback",
            onClick = onNavigateToAbout
        )
    )

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Tools Section
            item {
                SectionHeader(title = "Tools")
            }
            items(toolsSection) { category ->
                SettingsCategoryCard(category = category)
            }

            // Spacer
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 2. Preferences Section
            item {
                SectionHeader(title = "Preferences")
            }
            items(preferencesSection) { category ->
                SettingsCategoryCard(category = category)
            }

            // Spacer
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 3. Data Section
            item {
                SectionHeader(title = "Data")
            }
            items(dataSection) { category ->
                SettingsCategoryCard(category = category)
            }

            // Spacer
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 4. Security Section
            item {
                SectionHeader(title = "Security")
            }
            items(securitySection) { category ->
                SettingsCategoryCard(category = category)
            }

            // Spacer
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 5. Information Section
            item {
                SectionHeader(title = "Information")
            }
            items(informationSection) { category ->
                SettingsCategoryCard(category = category)
            }
        }
    }
}

/**
 * Settings category data class
 */
private data class SettingsCategory(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val onClick: () -> Unit,
    val badge: String? = null
)

/**
 * Section header component
 */
@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    )
}

/**
 * Settings category card component
 */
@Composable
private fun SettingsCategoryCard(
    category: SettingsCategory,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { category.onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon
            Icon(
                imageVector = category.icon,
                contentDescription = category.title,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            // Title and Description
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = category.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Badge (for Phase 2 items)
                    category.badge?.let { badge ->
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = category.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Chevron icon
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}