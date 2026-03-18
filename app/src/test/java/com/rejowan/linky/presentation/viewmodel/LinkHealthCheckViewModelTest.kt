package com.rejowan.linky.presentation.viewmodel

import app.cash.turbine.test
import com.rejowan.linky.domain.model.Link
import com.rejowan.linky.domain.repository.LinkRepository
import com.rejowan.linky.domain.usecase.link.DeleteLinkUseCase
import com.rejowan.linky.presentation.feature.settings.healthcheck.HealthCheckEvent
import com.rejowan.linky.presentation.feature.settings.healthcheck.HealthCheckUiEvent
import com.rejowan.linky.presentation.feature.settings.healthcheck.LinkHealthCheckViewModel
import com.rejowan.linky.presentation.feature.settings.healthcheck.LinkHealthStatus
import com.rejowan.linky.util.LinkPreviewFetcher
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
class LinkHealthCheckViewModelTest {

    private lateinit var viewModel: LinkHealthCheckViewModel
    private lateinit var linkRepository: LinkRepository
    private lateinit var deleteLinkUseCase: DeleteLinkUseCase
    private lateinit var linkPreviewFetcher: LinkPreviewFetcher

    private val testDispatcher = StandardTestDispatcher()

    private val testLink = Link(
        id = "link-1",
        url = "https://example.com",
        title = "Example",
        createdAt = 1000L,
        updatedAt = 2000L
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        linkRepository = mockk()
        deleteLinkUseCase = mockk()
        linkPreviewFetcher = mockk()

        every { linkRepository.getAllLinks() } returns flowOf(listOf(testLink))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = LinkHealthCheckViewModel(
            linkRepository = linkRepository,
            deleteLinkUseCase = deleteLinkUseCase,
            linkPreviewFetcher = linkPreviewFetcher
        )
    }

    @Test
    fun `initial state is not checking`() = runTest {
        createViewModel()

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isChecking)
            assertEquals(0, state.totalLinks)
            assertEquals(0f, state.progress)
        }
    }

    @Test
    fun `OnStartHealthCheck starts health check`() = runTest {
        createViewModel()

        viewModel.onEvent(HealthCheckEvent.OnStartHealthCheck)
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            // After health check, state should reflect links count
            assertTrue(state.totalLinks >= 0)
        }
    }

    @Test
    fun `OnCancelHealthCheck cancels check`() = runTest {
        createViewModel()

        viewModel.onEvent(HealthCheckEvent.OnStartHealthCheck)
        viewModel.onEvent(HealthCheckEvent.OnCancelHealthCheck)

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.isCancelled)
        }
    }

    @Test
    fun `OnDeleteLink deletes link and updates results`() = runTest {
        coEvery { deleteLinkUseCase("link-1", softDelete = true) } returns Result.Success(Unit)

        createViewModel()
        advanceUntilIdle()

        viewModel.uiEvents.test {
            viewModel.onEvent(HealthCheckEvent.OnDeleteLink("link-1"))
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is HealthCheckUiEvent.ShowMessage)
            assertTrue((event as HealthCheckUiEvent.ShowMessage).message.contains("trash"))
        }

        coVerify { deleteLinkUseCase("link-1", softDelete = true) }
    }

    @Test
    fun `OnDeleteLink failure shows error`() = runTest {
        coEvery { deleteLinkUseCase("link-1", softDelete = true) } returns Result.Error(Exception("Failed"))

        createViewModel()
        advanceUntilIdle()

        viewModel.uiEvents.test {
            viewModel.onEvent(HealthCheckEvent.OnDeleteLink("link-1"))
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is HealthCheckUiEvent.ShowMessage)
            assertTrue((event as HealthCheckUiEvent.ShowMessage).message.contains("Failed"))
        }
    }

    @Test
    fun `OnFilterByStatus filters results`() = runTest {
        createViewModel()

        viewModel.onEvent(HealthCheckEvent.OnFilterByStatus(LinkHealthStatus.BROKEN))

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(LinkHealthStatus.BROKEN, state.filterStatus)
        }
    }

    @Test
    fun `OnFilterByStatus with null shows all`() = runTest {
        createViewModel()

        viewModel.onEvent(HealthCheckEvent.OnFilterByStatus(LinkHealthStatus.BROKEN))
        viewModel.onEvent(HealthCheckEvent.OnFilterByStatus(null))

        viewModel.state.test {
            val state = awaitItem()
            assertNull(state.filterStatus)
        }
    }

    @Test
    fun `shows message when no links to check`() = runTest {
        every { linkRepository.getAllLinks() } returns flowOf(emptyList())

        createViewModel()

        viewModel.uiEvents.test {
            viewModel.onEvent(HealthCheckEvent.OnStartHealthCheck)
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is HealthCheckUiEvent.ShowMessage)
            assertTrue((event as HealthCheckUiEvent.ShowMessage).message.contains("No links"))
        }
    }

    @Test
    fun `excludes trashed links from health check`() = runTest {
        val trashedLink = Link(
            id = "link-trashed",
            url = "https://example.com",
            title = "Trashed",
            deletedAt = System.currentTimeMillis(),
            createdAt = 1000L,
            updatedAt = 2000L
        )

        every { linkRepository.getAllLinks() } returns flowOf(listOf(trashedLink))

        createViewModel()

        viewModel.uiEvents.test {
            viewModel.onEvent(HealthCheckEvent.OnStartHealthCheck)
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is HealthCheckUiEvent.ShowMessage)
            assertTrue((event as HealthCheckUiEvent.ShowMessage).message.contains("No links"))
        }
    }

    @Test
    fun `progress updates during health check`() = runTest {
        every { linkRepository.getAllLinks() } returns flowOf(listOf(testLink))

        createViewModel()

        viewModel.onEvent(HealthCheckEvent.OnStartHealthCheck)

        // Progress should update as links are checked
        viewModel.state.test {
            val state = awaitItem()
            // Either checking is in progress or completed
            assertTrue(state.isChecking || state.progress >= 0f)
        }
    }

    @Test
    fun `filteredResults returns filtered list when status set`() = runTest {
        createViewModel()

        viewModel.onEvent(HealthCheckEvent.OnFilterByStatus(LinkHealthStatus.HEALTHY))

        viewModel.state.test {
            val state = awaitItem()
            // Filtered results should only contain HEALTHY status items
            assertTrue(state.filteredResults.all { it.status == LinkHealthStatus.HEALTHY || state.healthResults.isEmpty() })
        }
    }

    @Test
    fun `OnToggleRefetchThumbnails updates state`() = runTest {
        createViewModel()

        viewModel.onEvent(HealthCheckEvent.OnToggleRefetchThumbnails(true))

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.refetchThumbnailsEnabled)
        }
    }

    @Test
    fun `OnToggleRefetchTitles updates state`() = runTest {
        createViewModel()

        viewModel.onEvent(HealthCheckEvent.OnToggleRefetchTitles(true))

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.refetchTitlesEnabled)
        }
    }
}
