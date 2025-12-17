package com.rejowan.linky.presentation.feature.settings.about

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PrivacyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Privacy Policy",
                style = MaterialTheme.typography.titleMedium,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .height(400.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PrivacySection(
                    title = "Data Collection",
                    content = "Linky is designed with privacy as a core principle. The app operates entirely offline and does not collect, transmit, or share any personal data with external servers."
                )

                PrivacySection(
                    title = "Local Storage",
                    content = "All your links, collections, tags, and reader mode snapshots are stored locally on your device using an encrypted database. Your data never leaves your device unless you explicitly export it."
                )

                PrivacySection(
                    title = "Network Access",
                    content = "The app only accesses the internet to:\n\n• Fetch link metadata (title, description, favicon) when you save a new link\n• Capture reader mode content for offline reading\n• Check link health status when requested\n\nNo analytics, tracking, or telemetry data is collected."
                )

                PrivacySection(
                    title = "Vault Protection",
                    content = "Links stored in the vault are protected with a PIN that you set. The PIN is stored securely on your device and is never transmitted anywhere."
                )

                PrivacySection(
                    title = "Permissions",
                    content = "Linky requests only the permissions necessary for its features:\n\n• Internet: To fetch link metadata and content\n• Storage: To save reader mode snapshots locally"
                )

                PrivacySection(
                    title = "Open Source",
                    content = "Linky is open source software. You can review the complete source code on GitHub to verify these privacy practices."
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
private fun PrivacySection(
    title: String,
    content: String
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
