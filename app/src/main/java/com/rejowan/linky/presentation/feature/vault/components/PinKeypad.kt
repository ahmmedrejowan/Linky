package com.rejowan.linky.presentation.feature.vault.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * PIN indicator dots showing how many digits have been entered
 */
@Composable
fun PinIndicator(
    pinLength: Int,
    maxLength: Int,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(maxLength) { index ->
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isError -> MaterialTheme.colorScheme.error
                            index < pinLength -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.outlineVariant
                        }
                    )
            )
        }
    }
}

/**
 * Numeric keypad for PIN entry
 */
@Composable
fun PinKeypad(
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Row 1: 1, 2, 3
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            KeypadButton("1", enabled) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onDigitClick("1")
            }
            KeypadButton("2", enabled) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onDigitClick("2")
            }
            KeypadButton("3", enabled) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onDigitClick("3")
            }
        }

        // Row 2: 4, 5, 6
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            KeypadButton("4", enabled) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onDigitClick("4")
            }
            KeypadButton("5", enabled) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onDigitClick("5")
            }
            KeypadButton("6", enabled) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onDigitClick("6")
            }
        }

        // Row 3: 7, 8, 9
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            KeypadButton("7", enabled) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onDigitClick("7")
            }
            KeypadButton("8", enabled) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onDigitClick("8")
            }
            KeypadButton("9", enabled) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onDigitClick("9")
            }
        }

        // Row 4: Empty, 0, Backspace
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Empty spacer
            Spacer(modifier = Modifier.size(72.dp))

            KeypadButton("0", enabled) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onDigitClick("0")
            }

            // Backspace
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onBackspaceClick()
                },
                enabled = enabled,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace",
                    tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun KeypadButton(
    digit: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(
                if (enabled) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = digit,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Medium,
            color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
        )
    }
}
