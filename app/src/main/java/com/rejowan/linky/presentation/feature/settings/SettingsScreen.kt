package com.rejowan.linky.presentation.feature.settings

import android.content.Intent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.rejowan.linky.BuildConfig
import com.rejowan.linky.R
import com.rejowan.linky.data.local.preferences.ThemePreferences
import com.rejowan.linky.data.update.UpdateState
import com.rejowan.linky.presentation.feature.settings.components.ChangelogSheet
import com.rejowan.linky.presentation.feature.settings.components.CreatorSheet
import com.rejowan.linky.presentation.feature.settings.components.LicenseSheet
import com.rejowan.linky.presentation.feature.settings.components.OpenSourceLicensesSheet
import com.rejowan.linky.presentation.feature.settings.components.PrivacyPolicySheet
import com.rejowan.linky.presentation.feature.settings.components.ThemePickerSheet
import com.rejowan.linky.presentation.feature.settings.components.UpdateAvailableSheet
import com.rejowan.linky.presentation.feature.settings.components.UpdateIntervalSheet
import com.rejowan.linky.ui.theme.SoftAccents
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private const val GITHUB_URL = "https://github.com/ahmmedrejowan/Linky"

/**
 * Redesigned Settings Screen with PDF Reader Pro style UI
 * All settings and about content is displayed via bottom sheets within this screen
 */
@Composable
fun SettingsScreen(
    onNavigateToDangerZone: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Theme preferences
    val themePreferences = remember { ThemePreferences(context) }
    val isClipboardCheckingEnabled by themePreferences.isClipboardCheckingEnabled()
        .collectAsState(initial = true)

    // Sheet states
    var showThemeSheet by remember { mutableStateOf(false) }
    var showUpdateIntervalSheet by remember { mutableStateOf(false) }
    var showChangelogSheet by remember { mutableStateOf(false) }
    var showPrivacyPolicySheet by remember { mutableStateOf(false) }
    var showOpenSourceLicensesSheet by remember { mutableStateOf(false) }
    var showLicenseSheet by remember { mutableStateOf(false) }
    var showCreatorSheet by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 100.dp)
    ) {
        // Settings Title
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        // App Header Card
        SettingsHeaderCard()

        Spacer(modifier = Modifier.height(24.dp))

        // App Settings Section
        SectionLabel(text = "APP SETTINGS", delay = 0)
        Spacer(modifier = Modifier.height(8.dp))

        // Clipboard Checking Toggle
        SettingsToggleItem(
            icon = Icons.Rounded.ContentPaste,
            title = "Clipboard Checking",
            subtitle = "Auto-detect URLs when app opens",
            accentColor = SoftAccents.Blue,
            checked = isClipboardCheckingEnabled,
            onCheckedChange = { enabled ->
                coroutineScope.launch {
                    themePreferences.setClipboardCheckingEnabled(enabled)
                }
            },
            animationDelay = 50
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Theme Picker
        SettingsOptionItem(
            icon = Icons.Rounded.Palette,
            title = "Theme",
            subtitle = when (state.theme) {
                "Light" -> "Light mode"
                "Dark" -> "Dark mode"
                else -> "System default"
            },
            accentColor = SoftAccents.Purple,
            onClick = { showThemeSheet = true },
            animationDelay = 100
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Updates Section
        SectionLabel(text = "UPDATES", delay = 150)
        Spacer(modifier = Modifier.height(8.dp))

        SettingsOptionItem(
            icon = Icons.Rounded.SystemUpdate,
            title = "Check for Updates",
            subtitle = when (updateState) {
                is UpdateState.Idle -> "Tap to check for updates"
                is UpdateState.Checking -> "Checking..."
                is UpdateState.Available -> "Update available: v${(updateState as UpdateState.Available).release.version}"
                is UpdateState.UpToDate -> "You're up to date"
                is UpdateState.Error -> "Error: ${(updateState as UpdateState.Error).message}"
            },
            accentColor = SoftAccents.Amber,
            onClick = { viewModel.checkForUpdates() },
            animationDelay = 200
        )

        // Show install option if update available
        if (updateState is UpdateState.Available) {
            Spacer(modifier = Modifier.height(8.dp))

            SettingsOptionItem(
                icon = Icons.Rounded.InstallMobile,
                title = "Download Update",
                subtitle = "v${(updateState as UpdateState.Available).release.version} is ready",
                accentColor = Color(0xFF4CAF50),
                onClick = { /* Will show update sheet */ },
                animationDelay = 210
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        SettingsOptionItem(
            icon = Icons.Rounded.Schedule,
            title = "Auto-check Interval",
            subtitle = state.updateCheckInterval.displayName,
            accentColor = SoftAccents.Blue,
            onClick = { showUpdateIntervalSheet = true },
            animationDelay = 250
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Danger Zone Section
        SectionLabel(text = "DANGER ZONE", delay = 300)
        Spacer(modifier = Modifier.height(8.dp))

        SettingsOptionItem(
            icon = Icons.Rounded.DeleteForever,
            title = "Delete Data",
            subtitle = "Clear cache, delete all data",
            accentColor = Color(0xFFE53935),
            onClick = onNavigateToDangerZone,
            animationDelay = 350
        )

        Spacer(modifier = Modifier.height(24.dp))

        // About Section
        SectionLabel(text = "ABOUT", delay = 400)
        Spacer(modifier = Modifier.height(8.dp))

        SettingsOptionItem(
            icon = Icons.Rounded.Info,
            title = "Version ${BuildConfig.VERSION_NAME}",
            subtitle = "View changelog",
            accentColor = SoftAccents.Blue,
            onClick = { showChangelogSheet = true },
            animationDelay = 450
        )

        Spacer(modifier = Modifier.height(8.dp))

        SettingsOptionItem(
            icon = Icons.Rounded.Policy,
            title = "Privacy Policy",
            subtitle = "How we handle your data",
            accentColor = SoftAccents.Teal,
            onClick = { showPrivacyPolicySheet = true },
            animationDelay = 500
        )

        Spacer(modifier = Modifier.height(8.dp))

        SettingsOptionItem(
            icon = Icons.Rounded.Gavel,
            title = "Open Source Licenses",
            subtitle = "View third-party libraries",
            accentColor = SoftAccents.Purple,
            onClick = { showOpenSourceLicensesSheet = true },
            animationDelay = 550
        )

        Spacer(modifier = Modifier.height(8.dp))

        SettingsOptionItem(
            icon = Icons.Rounded.Person,
            title = "Creator",
            subtitle = "About the developer",
            accentColor = SoftAccents.Amber,
            onClick = { showCreatorSheet = true },
            animationDelay = 600
        )

        Spacer(modifier = Modifier.height(8.dp))

        SettingsOptionItem(
            icon = Icons.Rounded.Gavel,
            title = "App License",
            subtitle = "GPL-3.0 License",
            accentColor = SoftAccents.Blue,
            onClick = { showLicenseSheet = true },
            animationDelay = 650
        )

        Spacer(modifier = Modifier.height(8.dp))

        SettingsOptionItem(
            icon = Icons.Rounded.Email,
            title = "Contact",
            subtitle = "Get in touch",
            accentColor = SoftAccents.Teal,
            onClick = {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = "mailto:kmrejowan@gmail.com".toUri()
                    putExtra(Intent.EXTRA_SUBJECT, "Linky Feedback")
                }
                context.startActivity(intent)
            },
            animationDelay = 700
        )

        Spacer(modifier = Modifier.height(8.dp))

        SettingsOptionItem(
            icon = Icons.Rounded.Code,
            title = "Source Code",
            subtitle = "View on GitHub",
            accentColor = SoftAccents.Purple,
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, GITHUB_URL.toUri())
                context.startActivity(intent)
            },
            animationDelay = 700
        )
    }

    // Bottom Sheets
    if (showThemeSheet) {
        ThemePickerSheet(
            currentTheme = state.theme,
            onSelect = { theme ->
                viewModel.onEvent(SettingsEvent.OnThemeChange(theme))
                showThemeSheet = false
            },
            onDismiss = { showThemeSheet = false }
        )
    }

    if (showUpdateIntervalSheet) {
        UpdateIntervalSheet(
            currentInterval = state.updateCheckInterval,
            onSelect = { interval ->
                viewModel.setUpdateCheckInterval(interval)
                showUpdateIntervalSheet = false
            },
            onDismiss = { showUpdateIntervalSheet = false }
        )
    }

    if (showChangelogSheet) {
        ChangelogSheet(onDismiss = { showChangelogSheet = false })
    }

    if (showPrivacyPolicySheet) {
        PrivacyPolicySheet(onDismiss = { showPrivacyPolicySheet = false })
    }

    if (showOpenSourceLicensesSheet) {
        OpenSourceLicensesSheet(onDismiss = { showOpenSourceLicensesSheet = false })
    }

    if (showLicenseSheet) {
        LicenseSheet(onDismiss = { showLicenseSheet = false })
    }

    if (showCreatorSheet) {
        CreatorSheet(onDismiss = { showCreatorSheet = false })
    }

    // Update available sheet
    if (updateState is UpdateState.Available) {
        val availableState = updateState as UpdateState.Available
        UpdateAvailableSheet(
            release = availableState.release,
            currentVersion = availableState.currentVersion,
            onDismiss = { viewModel.dismissUpdateDialog() },
            onDownload = {
                // TODO: Implement download
                viewModel.dismissUpdateDialog()
            }
        )
    }
}

@Composable
private fun SectionLabel(
    text: String,
    delay: Int,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(delay.toLong())
        isVisible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.9f,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "section scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(200),
        label = "section alpha"
    )

    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = MaterialTheme.typography.labelMedium.letterSpacing * 1.5f
        ),
        color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
        modifier = modifier
            .scale(scale)
            .padding(start = 4.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsHeaderCard() {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.95f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "header scale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App logo
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "App logo",
                modifier = Modifier.size(56.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Linky",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Link Management Made Simple",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Version ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // License badge
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = SoftAccents.Teal.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "Apache 2.0",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = SoftAccents.Teal,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsOptionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit,
    animationDelay: Int,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(animationDelay.toLong())
        isVisible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.95f,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "item scale"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = accentColor),
                onClick = onClick
            ),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accentColor.copy(alpha = 0.12f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(20.dp),
                    tint = accentColor
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    animationDelay: Int,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(animationDelay.toLong())
        isVisible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.95f,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "item scale"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = accentColor),
                onClick = { onCheckedChange(!checked) }
            ),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accentColor.copy(alpha = 0.12f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(20.dp),
                    tint = accentColor
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = accentColor,
                    checkedTrackColor = accentColor.copy(alpha = 0.3f)
                )
            )
        }
    }
}
