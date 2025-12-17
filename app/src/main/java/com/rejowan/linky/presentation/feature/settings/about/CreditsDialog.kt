package com.rejowan.linky.presentation.feature.settings.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CreditsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Credits",
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Open Source Libraries",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                SingleCredit(
                    name = "Jetpack Compose",
                    license = "Apache 2.0",
                    url = "https://developer.android.com/jetpack/compose",
                    onViewClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
                    }
                )

                SingleCredit(
                    name = "Room Database",
                    license = "Apache 2.0",
                    url = "https://developer.android.com/training/data-storage/room",
                    onViewClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
                    }
                )

                SingleCredit(
                    name = "Koin",
                    version = "4.0.0",
                    license = "Apache 2.0",
                    url = "https://insert-koin.io/",
                    onViewClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
                    }
                )

                SingleCredit(
                    name = "Coil",
                    version = "2.5.0",
                    license = "Apache 2.0",
                    url = "https://coil-kt.github.io/coil/",
                    onViewClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
                    }
                )

                SingleCredit(
                    name = "Jsoup",
                    version = "1.17.2",
                    license = "MIT",
                    url = "https://jsoup.org/",
                    onViewClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
                    }
                )

                SingleCredit(
                    name = "Readability4J",
                    version = "1.0.8",
                    license = "Apache 2.0",
                    url = "https://github.com/nicoly/readability4j",
                    onViewClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
                    }
                )

                SingleCredit(
                    name = "Glance AppWidget",
                    license = "Apache 2.0",
                    url = "https://developer.android.com/jetpack/compose/glance",
                    onViewClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
                    }
                )

                SingleCredit(
                    name = "Timber",
                    version = "5.0.1",
                    license = "Apache 2.0",
                    url = "https://github.com/JakeWharton/timber",
                    onViewClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
                    }
                )

                SingleCredit(
                    name = "Material Icons Extended",
                    license = "Apache 2.0",
                    url = "https://fonts.google.com/icons",
                    onViewClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = "Design & Assets",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Material Design 3 by Google",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "Material Icons by Google",
                    style = MaterialTheme.typography.bodyMedium
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
private fun SingleCredit(
    name: String,
    license: String,
    url: String,
    onViewClick: (String) -> Unit,
    version: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewClick(url) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (version != null) "$name v$version" else name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = license,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = "View",
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}
