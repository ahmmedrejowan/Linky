package com.rejowan.linky.presentation.feature.settings.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.rejowan.licensy.LicenseContent
import com.rejowan.licensy.Licenses
import com.rejowan.licensy.compose.LicensyList
import com.rejowan.linky.data.update.UpdateCheckInterval
import com.rejowan.linky.ui.theme.SoftAccents

/**
 * Update Interval Picker Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateIntervalSheet(
    currentInterval: UpdateCheckInterval,
    onSelect: (UpdateCheckInterval) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SoftAccents.Blue.copy(alpha = 0.12f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(20.dp),
                        tint = SoftAccents.Blue
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Auto-check Interval",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "How often to check for updates",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Options
            UpdateCheckInterval.entries.forEach { interval ->
                IntervalOption(
                    interval = interval,
                    isSelected = interval == currentInterval,
                    onClick = { onSelect(interval) }
                )
                if (interval != UpdateCheckInterval.entries.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun IntervalOption(
    interval: UpdateCheckInterval,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) {
            SoftAccents.Blue.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = interval.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) SoftAccents.Blue else MaterialTheme.colorScheme.onSurface
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = "Selected",
                    tint = SoftAccents.Blue,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Changelog Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Changelog",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            ChangelogVersion(
                version = "1.0.0",
                date = "March 2026",
                changes = listOf(
                    "Initial release",
                    "Save and organize links",
                    "Collections support",
                    "Vault for private links",
                    "Batch import",
                    "Dark and light themes"
                )
            )
        }
    }
}

@Composable
private fun ChangelogVersion(
    version: String,
    date: String,
    changes: List<String>
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = SoftAccents.Purple.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "v$version",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = SoftAccents.Purple,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        changes.forEach { change ->
            Row(
                modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
            ) {
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = change,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Privacy Policy Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicySheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Privacy Policy",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            PolicySection(
                title = "Data Collection",
                content = "Linky does not collect any personal data. All your links, collections, and settings are stored locally on your device."
            )

            PolicySection(
                title = "Internet Access",
                content = "The app requires internet access only to fetch link previews and metadata. No data is sent to our servers."
            )

            PolicySection(
                title = "Third-Party Services",
                content = "The app does not use any third-party analytics or tracking services."
            )

            PolicySection(
                title = "Vault Security",
                content = "Links stored in the Vault are encrypted using AES encryption. The encryption key is derived from your PIN and stored securely in Android Keystore."
            )

            PolicySection(
                title = "Updates",
                content = "Update checks are performed via GitHub API. No personal information is transmitted during this process."
            )
        }
    }
}

@Composable
private fun PolicySection(
    title: String,
    content: String
) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * License Sheet (GPL-3.0)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicenseSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "GNU General Public License v3.0",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Text(
                    text = "Copyright ${java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)} K M Rejowan Ahmmed\n\n" +
                            "This program is free software: you can redistribute it and/or modify " +
                            "it under the terms of the GNU General Public License as published by " +
                            "the Free Software Foundation, either version 3 of the License, or " +
                            "(at your option) any later version.\n\n" +
                            "This program is distributed in the hope that it will be useful, " +
                            "but WITHOUT ANY WARRANTY; without even the implied warranty of " +
                            "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Key Terms",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LicenseTermItem("Freedom to use the software for any purpose")
                    LicenseTermItem("Freedom to study how the software works")
                    LicenseTermItem("Freedom to distribute copies")
                    LicenseTermItem("Freedom to modify and improve")
                    LicenseTermItem("Derivative works must also be GPL-3.0")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            "https://www.gnu.org/licenses/gpl-3.0.en.html".toUri()
                        )
                        context.startActivity(intent)
                    },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = "View Full GPL-3.0 License",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun LicenseTermItem(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

/**
 * Theme Picker Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemePickerSheet(
    currentTheme: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SoftAccents.Purple.copy(alpha = 0.12f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Palette,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(20.dp),
                        tint = SoftAccents.Purple
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Choose Theme",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Select your preferred appearance",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Theme options - "System Default" must match ThemePreferences storage value
            val themes = listOf(
                Triple("System Default", "Follow device settings", Icons.Rounded.PhoneAndroid),
                Triple("Light", "Always light mode", Icons.Rounded.LightMode),
                Triple("Dark", "Always dark mode", Icons.Rounded.DarkMode)
            )

            themes.forEach { (theme, description, icon) ->
                ThemeOption(
                    theme = theme,
                    description = description,
                    icon = icon,
                    isSelected = currentTheme == theme,
                    onClick = { onSelect(theme) }
                )
                if (theme != themes.last().first) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ThemeOption(
    theme: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) {
            SoftAccents.Purple.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (isSelected) SoftAccents.Purple else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = theme,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) SoftAccents.Purple else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = "Selected",
                    tint = SoftAccents.Purple,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Creator Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "About Creator",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Text(
                text = "K M Rejowan Ahmmed",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Android Developer",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            Text(
                text = "Passionate about creating beautiful and functional Android applications. Linky is built with love using Kotlin and Jetpack Compose.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    CreatorLinkItem(
                        icon = Icons.Rounded.Language,
                        label = "Website",
                        value = "rejowan.com",
                        accentColor = SoftAccents.Blue,
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, "https://rejowan.com".toUri())
                            context.startActivity(intent)
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    CreatorLinkItem(
                        icon = Icons.Rounded.Email,
                        label = "Email",
                        value = "kmrejowan@gmail.com",
                        accentColor = SoftAccents.Amber,
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = "mailto:kmrejowan@gmail.com".toUri()
                            }
                            context.startActivity(intent)
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    CreatorLinkItem(
                        icon = Icons.Rounded.Code,
                        label = "GitHub",
                        value = "github.com/ahmmedrejowan",
                        accentColor = SoftAccents.Purple,
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, "https://github.com/ahmmedrejowan".toUri())
                            context.startActivity(intent)
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    CreatorLinkItem(
                        icon = Icons.Rounded.Work,
                        label = "LinkedIn",
                        value = "linkedin.com/in/ahmmedrejowan",
                        accentColor = SoftAccents.Teal,
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, "https://linkedin.com/in/ahmmedrejowan".toUri())
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CreatorLinkItem(
    icon: ImageVector,
    label: String,
    value: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = accentColor.copy(alpha = 0.12f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(8.dp)
                    .size(18.dp),
                tint = accentColor
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

/**
 * Open Source Licenses Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenSourceLicensesSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val licenses = remember {
        listOf(
            LicenseContent(
                title = "Jetpack Compose",
                author = "Google",
                license = Licenses.APACHE_2_0,
                url = "https://developer.android.com/jetpack/compose"
            ),
            LicenseContent(
                title = "Koin",
                author = "Kotzilla",
                license = Licenses.APACHE_2_0,
                url = "https://insert-koin.io/"
            ),
            LicenseContent(
                title = "Room Database",
                author = "Google",
                license = Licenses.APACHE_2_0,
                url = "https://developer.android.com/training/data-storage/room"
            ),
            LicenseContent(
                title = "Material Components",
                author = "Google",
                license = Licenses.APACHE_2_0,
                url = "https://github.com/material-components/material-components-android"
            ),
            LicenseContent(
                title = "Kotlin Coroutines",
                author = "JetBrains",
                license = Licenses.APACHE_2_0,
                url = "https://github.com/Kotlin/kotlinx.coroutines"
            ),
            LicenseContent(
                title = "AndroidX Libraries",
                author = "Google",
                license = Licenses.APACHE_2_0,
                url = "https://developer.android.com/jetpack/androidx"
            ),
            LicenseContent(
                title = "DataStore",
                author = "Google",
                license = Licenses.APACHE_2_0,
                url = "https://developer.android.com/topic/libraries/architecture/datastore"
            ),
            LicenseContent(
                title = "Coil",
                author = "Coil Contributors",
                license = Licenses.APACHE_2_0,
                url = "https://coil-kt.github.io/coil/"
            ),
            LicenseContent(
                title = "Timber",
                author = "Jake Wharton",
                license = Licenses.APACHE_2_0,
                url = "https://github.com/JakeWharton/timber"
            ),
            LicenseContent(
                title = "Kotlinx Serialization",
                author = "JetBrains",
                license = Licenses.APACHE_2_0,
                url = "https://github.com/Kotlin/kotlinx.serialization"
            ),
            LicenseContent(
                title = "Navigation Compose",
                author = "Google",
                license = Licenses.APACHE_2_0,
                url = "https://developer.android.com/jetpack/compose/navigation"
            ),
            LicenseContent(
                title = "Jsoup",
                author = "Jonathan Hedley",
                license = Licenses.MIT,
                url = "https://jsoup.org/"
            ),
            LicenseContent(
                title = "AndroidX Security Crypto",
                author = "Google",
                license = Licenses.APACHE_2_0,
                url = "https://developer.android.com/jetpack/androidx/releases/security"
            ),
            LicenseContent(
                title = "Glance",
                author = "Google",
                license = Licenses.APACHE_2_0,
                url = "https://developer.android.com/jetpack/compose/glance"
            ),
            LicenseContent(
                title = "Licensy",
                author = "K M Rejowan Ahmmed",
                license = Licenses.APACHE_2_0,
                url = "https://github.com/ahmmedrejowan/Licensy"
            )
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Open Source Licenses",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            LicensyList(licenses = licenses)
        }
    }
}
