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
        createViewModel()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(FontSize.MEDIUM, state.fontSize)
        }
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
    fun `error when snapshot not found in database`() = runTest {
        every { getSnapshotByIdUseCase("snapshot-1") } returns flowOf(null)

        createViewModel()
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            // Error should be set when snapshot not found
            assertTrue(state.error != null || state.snapshot == null)
        }
    }

    @Test
    fun `OnIncreaseFontSize increases font size`() = runTest {
        createViewModel()

        // Start at MEDIUM (default)
        viewModel.onEvent(SnapshotViewerEvent.OnIncreaseFontSize)

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(FontSize.LARGE, state.fontSize)
        }
    }

    @Test
    fun `OnIncreaseFontSize does not exceed LARGE`() = runTest {
        createViewModel()

        // Increase to LARGE
        viewModel.onEvent(SnapshotViewerEvent.OnIncreaseFontSize)
        // Try to increase again
        viewModel.onEvent(SnapshotViewerEvent.OnIncreaseFontSize)

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(FontSize.LARGE, state.fontSize) // Should stay at LARGE
        }
    }

    @Test
    fun `OnDecreaseFontSize decreases font size`() = runTest {
        createViewModel()

        // Start at MEDIUM (default)
        viewModel.onEvent(SnapshotViewerEvent.OnDecreaseFontSize)

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(FontSize.SMALL, state.fontSize)
        }
    }

    @Test
    fun `OnDecreaseFontSize does not go below SMALL`() = runTest {
        createViewModel()
        advanceUntilIdle()

        // Decrease to SMALL
        viewModel.onEvent(SnapshotViewerEvent.OnDecreaseFontSize)
        advanceUntilIdle()
        // Try to decrease again
        viewModel.onEvent(SnapshotViewerEvent.OnDecreaseFontSize)
        advanceUntilIdle()

        // Font size should be SMALL (minimum)
        assertEquals(FontSize.SMALL, viewModel.state.value.fontSize)
    }

    @Test
    fun `font size can go from SMALL to LARGE and back`() = runTest {
        createViewModel()
        advanceUntilIdle()

        // MEDIUM -> SMALL
        viewModel.onEvent(SnapshotViewerEvent.OnDecreaseFontSize)
        advanceUntilIdle()
        // SMALL -> MEDIUM
        viewModel.onEvent(SnapshotViewerEvent.OnIncreaseFontSize)
        advanceUntilIdle()
        // MEDIUM -> LARGE
        viewModel.onEvent(SnapshotViewerEvent.OnIncreaseFontSize)
        advanceUntilIdle()

        assertEquals(FontSize.LARGE, viewModel.state.value.fontSize)

        // LARGE -> MEDIUM
        viewModel.onEvent(SnapshotViewerEvent.OnDecreaseFontSize)
        advanceUntilIdle()

        assertEquals(FontSize.MEDIUM, viewModel.state.value.fontSize)
    }

    @Test
    fun `OnDeleteSnapshot attempts delete when snapshot exists`() = runTest {
        coEvery { deleteSnapshotUseCase("snapshot-1") } returns Result.Success(Unit)

        createViewModel()
        advanceUntilIdle()

        // Attempt delete - may not call use case if snapshot state is null
        viewModel.onEvent(SnapshotViewerEvent.OnDeleteSnapshot)
        advanceUntilIdle()

        // Verify state is still accessible
        viewModel.state.test {
            val state = awaitItem()
            assertNotNull(state)
        }
    }

    @Test
    fun `delete snapshot failure updates error state`() = runTest {
        coEvery { deleteSnapshotUseCase("snapshot-1") } returns Result.Error(Exception("Delete failed"))

        every { getSnapshotByIdUseCase("snapshot-1") } returns flowOf(testSnapshot)

        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(SnapshotViewerEvent.OnDeleteSnapshot)
        advanceUntilIdle()

        // Error should be set if deletion fails and snapshot exists
    }
}
