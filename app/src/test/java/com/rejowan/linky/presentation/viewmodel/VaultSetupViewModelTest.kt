package com.rejowan.linky.presentation.viewmodel

import app.cash.turbine.test
import com.rejowan.linky.domain.usecase.vault.SetupVaultPinUseCase
import com.rejowan.linky.presentation.feature.vault.SetupStep
import com.rejowan.linky.presentation.feature.vault.VaultSetupEvent
import com.rejowan.linky.presentation.feature.vault.VaultSetupUiEvent
import com.rejowan.linky.presentation.feature.vault.VaultSetupViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VaultSetupViewModelTest {

    private lateinit var viewModel: VaultSetupViewModel
    private lateinit var setupVaultPinUseCase: SetupVaultPinUseCase

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        setupVaultPinUseCase = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = VaultSetupViewModel(setupVaultPinUseCase)
    }

    @Test
    fun `initial state is CREATE_PIN step with empty pin`() = runTest {
        createViewModel()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(SetupStep.CREATE_PIN, state.step)
            assertEquals("", state.pin)
            assertEquals("", state.confirmPin)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `OnDigitEntered adds digit to pin`() = runTest {
        createViewModel()

        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("1"))
        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("2"))

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("12", state.pin)
        }
    }

    @Test
    fun `auto-advances to CONFIRM_PIN when pin is complete`() = runTest {
        createViewModel()

        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("1"))
        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("2"))
        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("3"))
        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("4"))

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(SetupStep.CONFIRM_PIN, state.step)
            assertEquals("1234", state.pin)
        }
    }

    @Test
    fun `OnBackspace removes last digit in CREATE_PIN step`() = runTest {
        createViewModel()

        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("1"))
        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("2"))
        viewModel.onEvent(VaultSetupEvent.OnBackspace)

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("1", state.pin)
        }
    }

    @Test
    fun `OnBackspace in CONFIRM_PIN removes last digit`() = runTest {
        createViewModel()

        // Enter full PIN to advance to confirm step
        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("1"))
        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("2"))
        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("3"))
        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("4"))

        // Now in confirm step, enter partial
        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("1"))
        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("2"))
        viewModel.onEvent(VaultSetupEvent.OnBackspace)

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(SetupStep.CONFIRM_PIN, state.step)
            assertEquals("1", state.confirmPin)
        }
    }

    @Test
    fun `OnBackspace in CONFIRM_PIN with empty confirm goes back to CREATE_PIN`() = runTest {
        createViewModel()

        // Enter full PIN to advance to confirm step
        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("1"))
        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("2"))
        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("3"))
        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("4"))

        // Backspace with empty confirm
        viewModel.onEvent(VaultSetupEvent.OnBackspace)

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(SetupStep.CREATE_PIN, state.step)
            assertEquals("", state.pin)
        }
    }

    @Test
    fun `matching PINs emit SetupComplete`() = runTest {
        coEvery { setupVaultPinUseCase("1234") } returns kotlin.Result.success(Unit)

        createViewModel()
        advanceUntilIdle()

        // Enter PIN
        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("1"))
        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("2"))
        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("3"))
        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("4"))

        viewModel.uiEvents.test {
            // Confirm PIN
            viewModel.onEvent(VaultSetupEvent.OnDigitEntered("1"))
            viewModel.onEvent(VaultSetupEvent.OnDigitEntered("2"))
            viewModel.onEvent(VaultSetupEvent.OnDigitEntered("3"))
            viewModel.onEvent(VaultSetupEvent.OnDigitEntered("4"))
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is VaultSetupUiEvent.SetupComplete)
        }

        coVerify { setupVaultPinUseCase("1234") }
    }

    @Test
    fun `mismatched PINs show error and clear confirm`() = runTest {
        createViewModel()

        // Enter PIN
        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("1"))
        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("2"))
        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("3"))
        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("4"))

        // Enter different confirm PIN
        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("5"))
        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("6"))
        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("7"))
        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("8"))

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(SetupStep.CONFIRM_PIN, state.step)
            assertEquals("", state.confirmPin) // Cleared
            assertNotNull(state.error)
            assertTrue(state.error!!.contains("match"))
        }
    }

    @Test
    fun `OnClearError clears error`() = runTest {
        createViewModel()

        // Generate an error first
        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("1"))
        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("2"))
        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("3"))
        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("4"))
        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("5"))
        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("6"))
        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("7"))
        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("8"))

        viewModel.onEvent(VaultSetupEvent.OnClearError)

        viewModel.state.test {
            val state = awaitItem()
            assertNull(state.error)
        }
    }

    @Test
    fun `setup failure shows error`() = runTest {
        coEvery { setupVaultPinUseCase("1234") } returns kotlin.Result.failure(Exception("Setup failed"))

        createViewModel()
        advanceUntilIdle()

        // Enter PIN
        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("1"))
        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("2"))
        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("3"))
        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("4"))

        // Confirm PIN
        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("1"))
        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("2"))
        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("3"))
        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("4"))
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertNotNull(state.error)
            assertEquals("", state.confirmPin) // Cleared after error
        }
    }

    @Test
    fun `does not add digit beyond PIN_LENGTH`() = runTest {
        createViewModel()

        // Enter 5 digits in CREATE_PIN step
        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("1"))
        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("2"))
        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("3"))
        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("4"))
        // This should be ignored (already at PIN_LENGTH)
        viewModel.onEvent(VaultSetupEvent.OnDigitEntered("5"))

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("1234", state.pin)
            assertEquals(SetupStep.CONFIRM_PIN, state.step)
        }
    }
}
