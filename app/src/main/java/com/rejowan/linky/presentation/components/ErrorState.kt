package com.rejowan.linky.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * ErrorState component - Displays error messages with retry option
 *
 * @param icon The icon to display
 * @param title The error title
 * @param message The error message
 * @param onRetryClick Optional retry button callback
 * @param modifier Modifier for styling
 */
@Composable
fun ErrorState(
    icon: ImageVector = Icons.Outlined.Error,
    title: String = "Something Went Wrong",
    message: String,
    modifier: Modifier = Modifier,
    onRetryClick: (() -> Unit)? = null,
    onDismissClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Error Icon
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Title
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Error Message
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons
        if (onRetryClick != null) {
            Button(onClick = onRetryClick) {
                Text(text = "Retry")
            }
        }

        if (onDismissClick != null) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onDismissClick) {
                Text(text = "Dismiss")
            }
        }
    }
}

/**
 * Predefined error states for common scenarios
 */
object ErrorStates {
    @Composable
    fun NetworkError(
        onRetryClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        ErrorState(
            icon = Icons.Outlined.WifiOff,
            title = "No Internet Connection",
            message = "Please check your network connection and try again",
            onRetryClick = onRetryClick,
            modifier = modifier
        )
    }

    @Composable
    fun GenericError(
        errorMessage: String,
        onRetryClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        ErrorState(
            icon = Icons.Outlined.Error,
            title = "Error",
            message = errorMessage,
            onRetryClick = onRetryClick,
            modifier = modifier
        )
    }

    @Composable
    fun LoadError(
        onRetryClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        ErrorState(
            icon = Icons.Outlined.Error,
            title = "Failed to Load",
            message = "Something went wrong while loading data. Please try again",
            onRetryClick = onRetryClick,
            modifier = modifier
        )
    }
}
