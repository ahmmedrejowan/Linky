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
fun VaultSetupScreen(
    onNavigateBack: () -> Unit,
    onSetupComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VaultSetupViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Handle UI events
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                VaultSetupUiEvent.SetupComplete -> onSetupComplete()
                is VaultSetupUiEvent.ShowError -> { /* Handled in state */ }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Setup Vault") },
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
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = when (state.step) {
                        SetupStep.CREATE_PIN -> "Create a PIN"
                        SetupStep.CONFIRM_PIN -> "Confirm your PIN"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = when (state.step) {
                        SetupStep.CREATE_PIN -> "Enter a 4-digit PIN to secure your vault"
                        SetupStep.CONFIRM_PIN -> "Enter your PIN again to confirm"
                    },
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
                    pinLength = when (state.step) {
                        SetupStep.CREATE_PIN -> state.pin.length
                        SetupStep.CONFIRM_PIN -> state.confirmPin.length
                    },
                    maxLength = VaultSetupViewModel.PIN_LENGTH,
                    isError = state.error != null
                )

                // Error message
                if (state.error != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = state.error!!,
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
                        viewModel.onEvent(VaultSetupEvent.OnDigitEntered(digit))
                    },
                    onBackspaceClick = {
                        viewModel.onEvent(VaultSetupEvent.OnBackspace)
                    },
                    enabled = !state.isLoading
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
