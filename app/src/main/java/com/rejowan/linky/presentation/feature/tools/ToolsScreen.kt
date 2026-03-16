package com.rejowan.linky.presentation.feature.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rejowan.linky.ui.theme.SoftAccents

/**
 * Tools Screen - Central hub for all app tools and utilities
 *
 * Features:
 * - Quick Tools: Batch Import, Vault, Recycle Bin
 * - Data Management: Duplicate Detection, Link Health Check
 * - Import/Export: Data backup and restore
 */
@Composable
fun ToolsScreen(
    onNavigateToBatchImport: () -> Unit,
    onNavigateToVault: () -> Unit,
    onNavigateToTrash: () -> Unit,
    onNavigateToDataStorage: () -> Unit,
    onNavigateToDuplicateDetection: () -> Unit,
    onNavigateToLinkHealthCheck: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Header
        ToolsHeader()

        // Tools List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ToolCard(
                    icon = Icons.Default.FileUpload,
                    title = "Batch Import",
                    description = "Import multiple links at once from text",
                    accentColor = SoftAccents.Blue,
                    onClick = onNavigateToBatchImport
                )
            }

            item {
                ToolCard(
                    icon = Icons.Default.Security,
                    title = "Vault",
                    description = "Secure storage for private links",
                    accentColor = SoftAccents.Purple,
                    onClick = onNavigateToVault
                )
            }

            item {
                ToolCard(
                    icon = Icons.Default.Delete,
                    title = "Recycle Bin",
                    description = "View and restore deleted links",
                    accentColor = SoftAccents.Pink,
                    onClick = onNavigateToTrash
                )
            }

            item {
                ToolCard(
                    icon = Icons.Default.ImportExport,
                    title = "Import/Export",
                    description = "Backup and restore your data",
                    accentColor = SoftAccents.Teal,
                    onClick = onNavigateToDataStorage
                )
            }

            // Data Management Section
            item {
                SectionHeader(title = "Data Management")
            }

            item {
                ToolCard(
                    icon = Icons.Default.FindReplace,
                    title = "Duplicate Detection",
                    description = "Find and remove duplicate links",
                    accentColor = SoftAccents.Amber,
                    onClick = onNavigateToDuplicateDetection
                )
            }

            item {
                ToolCard(
                    icon = Icons.Default.HealthAndSafety,
                    title = "Link Health Check",
                    description = "Verify links are still accessible",
                    accentColor = SoftAccents.Teal,
                    onClick = onNavigateToLinkHealthCheck
                )
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun ToolsHeader(
    modifier: Modifier = Modifier
) {
    Text(
        text = "Tools",
        style = MaterialTheme.typography.headlineMedium.copy(
            fontWeight = FontWeight.Bold
        ),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    )
}

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.SemiBold
        ),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    )
}

@Composable
private fun ToolCard(
    icon: ImageVector,
    title: String,
    description: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon with accent background
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Title and Description
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Chevron
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
