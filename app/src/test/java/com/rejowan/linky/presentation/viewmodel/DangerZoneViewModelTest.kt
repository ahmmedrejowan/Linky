package com.rejowan.linky.presentation.viewmodel

import app.cash.turbine.test
import com.rejowan.linky.domain.repository.CollectionRepository
import com.rejowan.linky.domain.repository.LinkRepository
import com.rejowan.linky.domain.repository.SnapshotRepository
import com.rejowan.linky.presentation.feature.settings.dangerzone.DangerZoneViewModel
import com.rejowan.linky.presentation.feature.settings.dangerzone.OperationType
import com.rejowan.linky.util.FileStorageManager
import com.rejowan.linky.util.Result
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DangerZoneViewModelTest {

    private lateinit var viewModel: DangerZoneViewModel
    private lateinit var linkRepository: LinkRepository
    private lateinit var collectionRepository: CollectionRepository
    private lateinit var snapshotRepository: SnapshotRepository
    private lateinit var fileStorageManager: FileStorageManager

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        linkRepository = mockk(relaxed = true)
        collectionRepository = mockk(relaxed = true)
        snapshotRepository = mockk(relaxed = true)
        fileStorageManager = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = DangerZoneViewModel(
            linkRepository = linkRepository,
            collectionRepository = collectionRepository,
            snapshotRepository = snapshotRepository,
            fileStorageManager = fileStorageManager
        )
    }

    @Test
    fun `initial state is correct`() = runTest {
        createViewModel()

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isProcessing)
            assertNull(state.operationType)
            assertNull(state.resultMessage)
            assertFalse(state.isSuccess)
        }
    }

    // ============ Clear Cache Tests ============

    @Test
    fun `clearCache success updates state correctly`() = runTest {
        coEvery { fileStorageManager.clearPreviewCache() } returns true

        createViewModel()

        viewModel.clearCache()
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isProcessing)
            assertNull(state.operationType)
            assertEquals("Cache cleared successfully", state.resultMessage)
            assertTrue(state.isSuccess)
        }
    }

    @Test
    fun `clearCache partial failure updates state correctly`() = runTest {
        coEvery { fileStorageManager.clearPreviewCache() } returns false

        createViewModel()

        viewModel.clearCache()
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isProcessing)
            assertEquals("Some files could not be deleted", state.resultMessage)
            assertFalse(state.isSuccess)
        }
    }

    @Test
    fun `clearCache exception updates state with error`() = runTest {
        coEvery { fileStorageManager.clearPreviewCache() } throws RuntimeException("IO Error")

        createViewModel()

        viewModel.clearCache()
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isProcessing)
            assertTrue(state.resultMessage?.contains("Failed to clear cache") == true)
            assertFalse(state.isSuccess)
        }
    }

    // ============ Delete All Snapshots Tests ============

    @Test
    fun `deleteAllSnapshots success updates state correctly`() = runTest {
        coEvery { snapshotRepository.deleteAllSnapshots() } returns Result.Success(Unit)

        createViewModel()

        viewModel.deleteAllSnapshots()
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isProcessing)
            assertEquals("All snapshots deleted successfully", state.resultMessage)
            assertTrue(state.isSuccess)
        }

        coVerify { snapshotRepository.deleteAllSnapshots() }
    }

    @Test
    fun `deleteAllSnapshots exception updates state with error`() = runTest {
        coEvery { snapshotRepository.deleteAllSnapshots() } throws RuntimeException("Database error")

        createViewModel()

        viewModel.deleteAllSnapshots()
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isProcessing)
            assertTrue(state.resultMessage?.contains("Failed to delete snapshots") == true)
            assertFalse(state.isSuccess)
        }
    }

    // ============ Delete All Data Tests ============

    @Test
    fun `deleteAllData success updates state correctly`() = runTest {
        coEvery { snapshotRepository.deleteAllSnapshots() } returns Result.Success(Unit)
        coEvery { linkRepository.deleteAllLinks() } returns Result.Success(Unit)
        coEvery { collectionRepository.deleteAllCollections() } returns Result.Success(Unit)
        coEvery { fileStorageManager.clearPreviewCache() } returns true

        createViewModel()

        viewModel.deleteAllData()
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isProcessing)
            assertEquals("All data deleted successfully", state.resultMessage)
            assertTrue(state.isSuccess)
        }

        coVerify {
            snapshotRepository.deleteAllSnapshots()
            linkRepository.deleteAllLinks()
            collectionRepository.deleteAllCollections()
            fileStorageManager.clearPreviewCache()
        }
    }

    @Test
    fun `deleteAllData exception updates state with error`() = runTest {
        coEvery { snapshotRepository.deleteAllSnapshots() } throws RuntimeException("Database error")

        createViewModel()

        viewModel.deleteAllData()
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isProcessing)
            assertTrue(state.resultMessage?.contains("Failed to delete all data") == true)
            assertFalse(state.isSuccess)
        }
    }

    @Test
    fun `deleteAllData calls repositories in correct order`() = runTest {
        val callOrder = mutableListOf<String>()

        coEvery { snapshotRepository.deleteAllSnapshots() } coAnswers {
            callOrder.add("snapshots")
            Result.Success(Unit)
        }
        coEvery { linkRepository.deleteAllLinks() } coAnswers {
            callOrder.add("links")
            Result.Success(Unit)
        }
        coEvery { collectionRepository.deleteAllCollections() } coAnswers {
            callOrder.add("collections")
            Result.Success(Unit)
        }
        coEvery { fileStorageManager.clearPreviewCache() } coAnswers {
            callOrder.add("cache")
            true
        }

        createViewModel()

        viewModel.deleteAllData()
        advanceUntilIdle()

        assertEquals(listOf("snapshots", "links", "collections", "cache"), callOrder)
    }

    // ============ Clear Result Message Tests ============

    @Test
    fun `clearResultMessage clears the message`() = runTest {
        coEvery { fileStorageManager.clearPreviewCache() } returns true

        createViewModel()

        viewModel.clearCache()
        advanceUntilIdle()

        viewModel.clearResultMessage()

        viewModel.state.test {
            val state = awaitItem()
            assertNull(state.resultMessage)
        }
    }

    // ============ Processing State Tests ============

    @Test
    fun `clearCache sets processing state with correct operation type`() = runTest {
        coEvery { fileStorageManager.clearPreviewCache() } coAnswers {
            // Verify state during processing
            assertEquals(true, viewModel.state.value.isProcessing)
            assertEquals(OperationType.CLEAR_CACHE, viewModel.state.value.operationType)
            true
        }

        createViewModel()

        viewModel.clearCache()
        advanceUntilIdle()
    }

    @Test
    fun `deleteAllSnapshots sets processing state with correct operation type`() = runTest {
        coEvery { snapshotRepository.deleteAllSnapshots() } coAnswers {
            assertEquals(true, viewModel.state.value.isProcessing)
            assertEquals(OperationType.DELETE_SNAPSHOTS, viewModel.state.value.operationType)
            Result.Success(Unit)
        }

        createViewModel()

        viewModel.deleteAllSnapshots()
        advanceUntilIdle()
    }

    @Test
    fun `deleteAllData sets processing state with correct operation type`() = runTest {
        coEvery { snapshotRepository.deleteAllSnapshots() } coAnswers {
            assertEquals(true, viewModel.state.value.isProcessing)
            assertEquals(OperationType.DELETE_ALL, viewModel.state.value.operationType)
            Result.Success(Unit)
        }
        coEvery { linkRepository.deleteAllLinks() } returns Result.Success(Unit)
        coEvery { collectionRepository.deleteAllCollections() } returns Result.Success(Unit)
        coEvery { fileStorageManager.clearPreviewCache() } returns true

        createViewModel()

        viewModel.deleteAllData()
        advanceUntilIdle()
    }
}
