package com.rejowan.linky.presentation.viewmodel

import app.cash.turbine.test
import com.rejowan.linky.domain.model.Link
import com.rejowan.linky.domain.repository.LinkRepository
import com.rejowan.linky.domain.usecase.link.DeleteLinkUseCase
import com.rejowan.linky.presentation.feature.settings.duplicates.DuplicateDetectionViewModel
import com.rejowan.linky.presentation.feature.settings.duplicates.DuplicateEvent
import com.rejowan.linky.presentation.feature.settings.duplicates.DuplicateUiEvent
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
class DuplicateDetectionViewModelTest {

    private lateinit var viewModel: DuplicateDetectionViewModel
    private lateinit var linkRepository: LinkRepository
    private lateinit var deleteLinkUseCase: DeleteLinkUseCase

    private val testDispatcher = StandardTestDispatcher()

    private val testLink1 = Link(
        id = "link-1",
        url = "https://example.com",
        title = "Example 1",
        createdAt = 1000L,
        updatedAt = 2000L
    )

    private val testLink2 = Link(
        id = "link-2",
        url = "https://example.com", // Same URL - duplicate
        title = "Example 2",
        createdAt = 2000L,
        updatedAt = 3000L
    )

    private val testLink3 = Link(
        id = "link-3",
        url = "https://other.com",
        title = "Other",
        createdAt = 3000L,
        updatedAt = 4000L
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        linkRepository = mockk()
        deleteLinkUseCase = mockk()

        every { linkRepository.getAllLinks() } returns flowOf(listOf(testLink1, testLink2, testLink3))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = DuplicateDetectionViewModel(
            linkRepository,
            deleteLinkUseCase
        )
    }

    @Test
    fun `initial state has not scanned`() = runTest {
        createViewModel()

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isScanning)
            assertFalse(state.hasScanned)
            assertEquals(0, state.duplicateGroups.size)
        }
    }

    @Test
    fun `OnScan finds duplicates`() = runTest {
        createViewModel()

        viewModel.onEvent(DuplicateEvent.OnScan)
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isScanning)
            assertTrue(state.hasScanned)
            assertEquals(1, state.duplicateGroups.size) // One group of duplicates
            assertEquals(1, state.totalDuplicatesFound) // 2 links, 1 is duplicate
        }
    }

    @Test
    fun `no duplicates found shows message`() = runTest {
        every { linkRepository.getAllLinks() } returns flowOf(listOf(testLink1, testLink3))

        createViewModel()

        viewModel.uiEvents.test {
            viewModel.onEvent(DuplicateEvent.OnScan)
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is DuplicateUiEvent.ShowMessage)
            assertTrue((event as DuplicateUiEvent.ShowMessage).message.contains("No duplicates"))
        }
    }

    @Test
    fun `OnScan rescans for duplicates`() = runTest {
        createViewModel()

        viewModel.onEvent(DuplicateEvent.OnScan)
        advanceUntilIdle()

        viewModel.onEvent(DuplicateEvent.OnScan)
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isScanning)
            assertEquals(1, state.duplicateGroups.size)
        }
    }

    @Test
    fun `OnDeleteLink deletes link`() = runTest {
        coEvery { deleteLinkUseCase("link-2", softDelete = true) } returns Result.Success(Unit)

        createViewModel()

        viewModel.onEvent(DuplicateEvent.OnScan)
        advanceUntilIdle()

        viewModel.onEvent(DuplicateEvent.OnDeleteLink("link-2"))
        advanceUntilIdle()

        coVerify { deleteLinkUseCase("link-2", softDelete = true) }
    }

    @Test
    fun `OnDeleteAllInGroup deletes all except first`() = runTest {
        coEvery { deleteLinkUseCase(any(), softDelete = true) } returns Result.Success(Unit)

        createViewModel()

        viewModel.onEvent(DuplicateEvent.OnScan)
        advanceUntilIdle()

        viewModel.onEvent(DuplicateEvent.OnDeleteAllInGroup(0, keepFirst = true))
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isDeleting)
        }
    }

    @Test
    fun `OnDeleteAllDuplicates deletes all duplicates keeping oldest`() = runTest {
        coEvery { deleteLinkUseCase(any(), softDelete = true) } returns Result.Success(Unit)

        createViewModel()

        viewModel.onEvent(DuplicateEvent.OnScan)
        advanceUntilIdle()

        viewModel.onEvent(DuplicateEvent.OnDeleteAllDuplicates)
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isDeleting)
        }
    }

    @Test
    fun `OnExpandGroup toggles group expansion`() = runTest {
        createViewModel()

        viewModel.onEvent(DuplicateEvent.OnScan)
        advanceUntilIdle()

        viewModel.onEvent(DuplicateEvent.OnExpandGroup(0))

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.expandedGroups.contains(0))
        }

        viewModel.onEvent(DuplicateEvent.OnExpandGroup(0))

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.expandedGroups.contains(0))
        }
    }

    @Test
    fun `normalizes URLs correctly for duplicate detection`() = runTest {
        val linkWithWww = Link(
            id = "link-www",
            url = "https://www.example.com",
            title = "With WWW",
            createdAt = 1000L,
            updatedAt = 2000L
        )

        val linkWithoutWww = Link(
            id = "link-no-www",
            url = "https://example.com",
            title = "Without WWW",
            createdAt = 2000L,
            updatedAt = 3000L
        )

        every { linkRepository.getAllLinks() } returns flowOf(listOf(linkWithWww, linkWithoutWww))

        createViewModel()

        viewModel.onEvent(DuplicateEvent.OnScan)
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(1, state.duplicateGroups.size) // Should detect as duplicates
        }
    }

    @Test
    fun `excludes trashed links from duplicate scan`() = runTest {
        val trashedLink = Link(
            id = "link-trashed",
            url = "https://example.com",
            title = "Trashed",
            deletedAt = System.currentTimeMillis(),
            createdAt = 1000L,
            updatedAt = 2000L
        )

        every { linkRepository.getAllLinks() } returns flowOf(listOf(testLink1, trashedLink))

        createViewModel()

        viewModel.onEvent(DuplicateEvent.OnScan)
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(0, state.duplicateGroups.size) // No duplicates (trashed excluded)
        }
    }
}
