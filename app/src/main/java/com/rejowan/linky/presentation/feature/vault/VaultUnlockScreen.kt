package com.rejowan.linky.presentation.feature.vault

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rejowan.linky.presentation.feature.vault.components.PinIndicator
import com.rejowan.linky.presentation.feature.vault.components.PinKeypad
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultUnlockScreen(
    onNavigateBack: () -> Unit,
    onUnlockSuccess: () -> Unit,
    onNavigateToSetup: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VaultUnlockViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Handle UI events
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                VaultUnlockUiEvent.UnlockSuccess -> onUnlockSuccess()
                VaultUnlockUiEvent.NavigateToSetup -> onNavigateToSetup()
                is VaultUnlockUiEvent.ShowError -> { /* Handled in state */ }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Unlock Vault") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top section - Icon and instructions
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.padding(16.dp),
                    tint = if (state.isLocked) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (state.isLocked) "Vault Locked" else "Enter PIN",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (state.isLocked) "Please wait before trying again"
                    else "Enter your 4-digit PIN to unlock",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // Middle section - PIN indicator
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PinIndicator(
                    pinLength = state.pin.length,
                    maxLength = VaultUnlockViewModel.PIN_LENGTH,
                    isError = state.error != null
                )

                // Error message
                state.error?.let { error ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Bottom section - Keypad
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PinKeypad(
                    onDigitClick = { digit ->
                        viewModel.onEvent(VaultUnlockEvent.OnDigitEntered(digit))
                    },
                    onBackspaceClick = {
                        viewModel.onEvent(VaultUnlockEvent.OnBackspace)
                    },
                    enabled = !state.isLoading && !state.isLocked
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
