package com.rejowan.linky.presentation.feature.settings.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VersionLogDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Version Log",
                style = MaterialTheme.typography.titleMedium,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .height(400.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp)
            ) {
                SingleVersionLog(
                    version = "Version 1.0.0 (1) - December 2024",
                    changes = listOf(
                        "Initial release with offline-first link management.",
                        "Save, organize, and search your links with ease.",
                        "Collections and tags for better organization.",
                        "Reader mode snapshots for offline reading.",
                        "PIN-protected vault for sensitive links.",
                        "Batch import multiple URLs at once.",
                        "Link health check and duplicate detection.",
                        "Material Design 3 with light/dark themes.",
                        "Home screen widget for quick access."
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    )
}

@Composable
private fun SingleVersionLog(version: String, changes: List<String>) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(
            text = version,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        changes.forEach { change ->
            Text(
                text = "• $change",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
    }
}
