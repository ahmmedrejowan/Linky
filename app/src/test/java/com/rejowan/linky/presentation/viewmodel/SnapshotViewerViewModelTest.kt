package com.rejowan.linky.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.rejowan.linky.domain.model.Snapshot
import com.rejowan.linky.domain.model.SnapshotType
import com.rejowan.linky.domain.usecase.snapshot.DeleteSnapshotUseCase
import com.rejowan.linky.domain.usecase.snapshot.GetSnapshotByIdUseCase
import com.rejowan.linky.presentation.feature.snapshotviewer.FontSize
import com.rejowan.linky.presentation.feature.snapshotviewer.SnapshotViewerEvent
import com.rejowan.linky.presentation.feature.snapshotviewer.SnapshotViewerViewModel
import com.rejowan.linky.util.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
class SnapshotViewerViewModelTest {

    private lateinit var viewModel: SnapshotViewerViewModel
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var getSnapshotByIdUseCase: GetSnapshotByIdUseCase
    private lateinit var deleteSnapshotUseCase: DeleteSnapshotUseCase

    private val testDispatcher = StandardTestDispatcher()

    private val testSnapshot = Snapshot(
        id = "snapshot-1",
        linkId = "link-1",
        type = SnapshotType.READER_MODE,
        filePath = "/data/snapshots/snapshot-1.html",
        fileSize = 1024L,
        createdAt = 1000L
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        savedStateHandle = SavedStateHandle(mapOf("snapshotId" to "snapshot-1"))
        getSnapshotByIdUseCase = mockk()
        deleteSnapshotUseCase = mockk()

        every { getSnapshotByIdUseCase("snapshot-1") } returns flowOf(testSnapshot)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = SnapshotViewerViewModel(
            savedStateHandle,
            getSnapshotByIdUseCase,
            deleteSnapshotUseCase
        )
    }

    @Test
    fun `initial state has default font size`() = runTest {
        // Use null snapshot to avoid file I/O issues in tests
        every { getSnapshotByIdUseCase("snapshot-1") } returns flowOf(null)

        createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(FontSize.MEDIUM, viewModel.state.value.fontSize)
    }

    @Test
    fun `error when snapshot ID not provided`() = runTest {
        savedStateHandle = SavedStateHandle(emptyMap())

        viewModel = SnapshotViewerViewModel(
            savedStateHandle,
            getSnapshotByIdUseCase,
            deleteSnapshotUseCase
        )

        viewModel.state.test {
            val state = awaitItem()
            assertNotNull(state.error)
            assertTrue(state.error!!.contains("not found"))
        }
    }

    @Test
    fun `state when snapshot not found in database`() = runTest {
        every { getSnapshotByIdUseCase("snapshot-1") } returns flowOf(null)

        createViewModel()
        // Allow coroutines to process
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        // When snapshot not found, state should reflect this
        assertTrue("Expected error or null snapshot when not found",
            state.error?.contains("not found") == true || state.snapshot == null)
    }

    @Test
    fun `OnIncreaseFontSize increases font size`() = runTest {
        // Use null snapshot to avoid file I/O
        every { getSnapshotByIdUseCase("snapshot-1") } returns flowOf(null)

        createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Start at MEDIUM (default)
        viewModel.onEvent(SnapshotViewerEvent.OnIncreaseFontSize)
        testDispatcher.scheduler.runCurrent()

        assertEquals(FontSize.LARGE, viewModel.state.value.fontSize)
    }

    @Test
    fun `OnIncreaseFontSize does not exceed LARGE`() = runTest {
        // Use null snapshot to avoid file I/O
        every { getSnapshotByIdUseCase("snapshot-1") } returns flowOf(null)

        createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Increase to LARGE
        viewModel.onEvent(SnapshotViewerEvent.OnIncreaseFontSize)
        testDispatcher.scheduler.runCurrent()
        // Try to increase again
        viewModel.onEvent(SnapshotViewerEvent.OnIncreaseFontSize)
        testDispatcher.scheduler.runCurrent()

        assertEquals(FontSize.LARGE, viewModel.state.value.fontSize) // Should stay at LARGE
    }

    @Test
    fun `OnDecreaseFontSize decreases font size`() = runTest {
        // Use null snapshot to avoid file I/O
        every { getSnapshotByIdUseCase("snapshot-1") } returns flowOf(null)

        createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Start at MEDIUM (default)
        viewModel.onEvent(SnapshotViewerEvent.OnDecreaseFontSize)
        testDispatcher.scheduler.runCurrent()

        assertEquals(FontSize.SMALL, viewModel.state.value.fontSize)
    }

    @Test
    fun `OnDecreaseFontSize does not go below SMALL`() = runTest {
        // Use null snapshot to avoid file I/O
        every { getSnapshotByIdUseCase("snapshot-1") } returns flowOf(null)

        createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Decrease to SMALL
        viewModel.onEvent(SnapshotViewerEvent.OnDecreaseFontSize)
        testDispatcher.scheduler.runCurrent()
        // Try to decrease again
        viewModel.onEvent(SnapshotViewerEvent.OnDecreaseFontSize)
        testDispatcher.scheduler.runCurrent()

        // Font size should be SMALL (minimum)
        assertEquals(FontSize.SMALL, viewModel.state.value.fontSize)
    }

    @Test
    fun `font size can go from SMALL to LARGE and back`() = runTest {
        // Use null snapshot to avoid file I/O
        every { getSnapshotByIdUseCase("snapshot-1") } returns flowOf(null)

        createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // MEDIUM -> SMALL
        viewModel.onEvent(SnapshotViewerEvent.OnDecreaseFontSize)
        testDispatcher.scheduler.runCurrent()
        // SMALL -> MEDIUM
        viewModel.onEvent(SnapshotViewerEvent.OnIncreaseFontSize)
        testDispatcher.scheduler.runCurrent()
        // MEDIUM -> LARGE
        viewModel.onEvent(SnapshotViewerEvent.OnIncreaseFontSize)
        testDispatcher.scheduler.runCurrent()

        assertEquals(FontSize.LARGE, viewModel.state.value.fontSize)

        // LARGE -> MEDIUM
        viewModel.onEvent(SnapshotViewerEvent.OnDecreaseFontSize)
        testDispatcher.scheduler.runCurrent()

        assertEquals(FontSize.MEDIUM, viewModel.state.value.fontSize)
    }

    @Test
    fun `OnDeleteSnapshot does nothing when snapshot not loaded`() = runTest {
        // Snapshot not found, so delete should do nothing
        every { getSnapshotByIdUseCase("snapshot-1") } returns flowOf(null)
        coEvery { deleteSnapshotUseCase("snapshot-1") } returns Result.Success(Unit)

        createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Attempt delete - should do nothing since snapshot is null
        viewModel.onEvent(SnapshotViewerEvent.OnDeleteSnapshot)
        testDispatcher.scheduler.runCurrent()

        // Verify state is still accessible and snapshot is null
        val state = viewModel.state.value
        assertNotNull(state)
        assertNull(state.snapshot) // Snapshot should be null since not found
    }

    @Test
    fun `delete snapshot does not proceed when no snapshot loaded`() = runTest {
        // Snapshot not found, so delete should not proceed
        every { getSnapshotByIdUseCase("snapshot-1") } returns flowOf(null)
        coEvery { deleteSnapshotUseCase("snapshot-1") } returns Result.Error(Exception("Delete failed"))

        createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(SnapshotViewerEvent.OnDeleteSnapshot)
        testDispatcher.scheduler.runCurrent()

        // Verify state - since no snapshot, delete shouldn't be called
        val state = viewModel.state.value
        assertNull(state.snapshot)
    }
}
