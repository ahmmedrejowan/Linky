package com.rejowan.linky.presentation.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.rejowan.linky.domain.model.Link
import com.rejowan.linky.domain.repository.CollectionRepository
import com.rejowan.linky.domain.repository.LinkRepository
import com.rejowan.linky.domain.repository.TagRepository
import com.rejowan.linky.domain.usecase.link.DeleteLinkUseCase
import com.rejowan.linky.domain.usecase.link.GetAllLinksUseCase
import com.rejowan.linky.domain.usecase.link.GetArchivedLinksUseCase
import com.rejowan.linky.domain.usecase.link.GetFavoriteLinksUseCase
import com.rejowan.linky.domain.usecase.link.GetTrashedLinksUseCase
import com.rejowan.linky.domain.usecase.link.RestoreLinkUseCase
import com.rejowan.linky.domain.usecase.link.ToggleArchiveUseCase
import com.rejowan.linky.domain.usecase.link.ToggleFavoriteUseCase
import com.rejowan.linky.presentation.feature.home.FilterType
import com.rejowan.linky.presentation.feature.home.HomeEvent
import com.rejowan.linky.presentation.feature.home.HomeUiEvent
import com.rejowan.linky.presentation.feature.home.HomeViewModel
import com.rejowan.linky.presentation.feature.home.SortType
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class HomeViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getAllLinksUseCase: GetAllLinksUseCase
    private lateinit var getFavoriteLinksUseCase: GetFavoriteLinksUseCase
    private lateinit var getArchivedLinksUseCase: GetArchivedLinksUseCase
    private lateinit var getTrashedLinksUseCase: GetTrashedLinksUseCase
    private lateinit var toggleFavoriteUseCase: ToggleFavoriteUseCase
    private lateinit var toggleArchiveUseCase: ToggleArchiveUseCase
    private lateinit var deleteLinkUseCase: DeleteLinkUseCase
    private lateinit var restoreLinkUseCase: RestoreLinkUseCase
    private lateinit var linkRepository: LinkRepository
    private lateinit var collectionRepository: CollectionRepository
    private lateinit var tagRepository: TagRepository

    private lateinit var viewModel: HomeViewModel

    private val testLink = Link(
        id = "test-id-1",
        title = "Test Link",
        url = "https://example.com",
        createdAt = 1000L,
        updatedAt = 2000L
    )

    private val testLinks = listOf(
        testLink,
        Link(id = "test-id-2", title = "Second Link", url = "https://second.com", createdAt = 2000L, updatedAt = 3000L),
        Link(id = "test-id-3", title = "Third Link", url = "https://third.com", isFavorite = true, createdAt = 3000L, updatedAt = 4000L)
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        getAllLinksUseCase = mockk()
        getFavoriteLinksUseCase = mockk()
        getArchivedLinksUseCase = mockk()
        getTrashedLinksUseCase = mockk()
        toggleFavoriteUseCase = mockk()
        toggleArchiveUseCase = mockk()
        deleteLinkUseCase = mockk()
        restoreLinkUseCase = mockk()
        linkRepository = mockk(relaxed = true)
        collectionRepository = mockk(relaxed = true)
        tagRepository = mockk(relaxed = true)

        // Default mock behavior
        every { getAllLinksUseCase() } returns flowOf(testLinks)
        every { getFavoriteLinksUseCase() } returns flowOf(testLinks.filter { it.isFavorite })
        every { getArchivedLinksUseCase() } returns flowOf(emptyList())
        every { getTrashedLinksUseCase() } returns flowOf(emptyList())
        every { linkRepository.getAllLinksCount() } returns flowOf(3)
        every { linkRepository.getFavoriteLinksCount() } returns flowOf(1)
        every { linkRepository.getArchivedLinksCount() } returns flowOf(0)
        every { collectionRepository.getCollectionsWithLinkCount() } returns flowOf(emptyList())
        every { tagRepository.getTagsWithLinkCount() } returns flowOf(emptyList())
        coEvery { linkRepository.getAllActiveUrls() } returns emptyList()
        coEvery { linkRepository.existsByUrl(any()) } returns false
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): HomeViewModel {
        return HomeViewModel(
            getAllLinksUseCase = getAllLinksUseCase,
            getFavoriteLinksUseCase = getFavoriteLinksUseCase,
            getArchivedLinksUseCase = getArchivedLinksUseCase,
            getTrashedLinksUseCase = getTrashedLinksUseCase,
            toggleFavoriteUseCase = toggleFavoriteUseCase,
            toggleArchiveUseCase = toggleArchiveUseCase,
            deleteLinkUseCase = deleteLinkUseCase,
            restoreLinkUseCase = restoreLinkUseCase,
            linkRepository = linkRepository,
            collectionRepository = collectionRepository,
            tagRepository = tagRepository
        )
    }

    // ============ Initial State Tests ============

    @Test
    fun `initial state has default values`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(FilterType.ALL, state.filterType)
        assertEquals(SortType.DATE_ADDED_DESC, state.sortType)
        assertFalse(state.isLoading)
        assertFalse(state.isSelectionMode)
        assertTrue(state.selectedLinkIds.isEmpty())
    }

    @Test
    fun `initial state loads links`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        // Favorites are sorted to top, so we expect favorites first
        assertTrue(state.links.isNotEmpty())
    }

    // ============ Filter Type Tests ============

    @Test
    fun `OnFilterTypeChange updates filter type`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(HomeEvent.OnFilterTypeChange(FilterType.FAVORITES))
        advanceUntilIdle()

        assertEquals(FilterType.FAVORITES, viewModel.state.value.filterType)
    }

    @Test
    fun `changing to FAVORITES filter shows only favorite links`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(HomeEvent.OnFilterTypeChange(FilterType.FAVORITES))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.links.all { it.isFavorite })
    }

    @Test
    fun `changing to ARCHIVED filter calls archived use case`() = runTest {
        val archivedLinks = listOf(testLink.copy(isArchived = true))
        every { getArchivedLinksUseCase() } returns flowOf(archivedLinks)

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(HomeEvent.OnFilterTypeChange(FilterType.ARCHIVED))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1, state.links.size)
        assertTrue(state.links[0].isArchived)
    }

    @Test
    fun `changing to TRASH filter calls trashed use case`() = runTest {
        val trashedLinks = listOf(testLink.copy(deletedAt = System.currentTimeMillis()))
        every { getTrashedLinksUseCase() } returns flowOf(trashedLinks)

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(HomeEvent.OnFilterTypeChange(FilterType.TRASH))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1, state.links.size)
        assertTrue(state.links[0].isDeleted)
    }

    // ============ Sort Type Tests ============

    @Test
    fun `OnSortTypeChange updates sort type`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(HomeEvent.OnSortTypeChange(SortType.TITLE_ASC))
        advanceUntilIdle()

        assertEquals(SortType.TITLE_ASC, viewModel.state.value.sortType)
    }

    @Test
    fun `sorting by title ascending orders links correctly`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(HomeEvent.OnSortTypeChange(SortType.TITLE_ASC))
        advanceUntilIdle()

        val links = viewModel.state.value.links
        // Favorites should still be at top within ALL filter, then sorted by title
        val nonFavorites = links.filter { !it.isFavorite }
        for (i in 0 until nonFavorites.size - 1) {
            assertTrue(nonFavorites[i].title.lowercase() <= nonFavorites[i + 1].title.lowercase())
        }
    }

    // ============ Toggle Favorite Tests ============

    @Test
    fun `OnToggleFavorite calls toggle favorite use case`() = runTest {
        coEvery { toggleFavoriteUseCase(any(), any()) } returns Result.Success(Unit)

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(HomeEvent.OnToggleFavorite("test-id-1", true))
        advanceUntilIdle()

        coVerify { toggleFavoriteUseCase("test-id-1", true) }
    }

    @Test
    fun `OnToggleFavorite emits success event`() = runTest {
        coEvery { toggleFavoriteUseCase(any(), any()) } returns Result.Success(Unit)

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.uiEvents.test {
            viewModel.onEvent(HomeEvent.OnToggleFavorite("test-id-1", true))
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is HomeUiEvent.ShowFavoriteToggled)
            assertEquals("test-id-1", (event as HomeUiEvent.ShowFavoriteToggled).linkId)
            assertTrue(event.isFavorite)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `OnToggleFavorite silent mode does not emit event`() = runTest {
        coEvery { toggleFavoriteUseCase(any(), any()) } returns Result.Success(Unit)

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.uiEvents.test {
            viewModel.onEvent(HomeEvent.OnToggleFavorite("test-id-1", true, silent = true))
            advanceUntilIdle()

            // Should not receive any events
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `OnToggleFavorite emits error event on failure`() = runTest {
        coEvery { toggleFavoriteUseCase(any(), any()) } returns Result.Error(RuntimeException("Test error"))

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.uiEvents.test {
            viewModel.onEvent(HomeEvent.OnToggleFavorite("test-id-1", true))
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is HomeUiEvent.ShowError)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ============ Archive Link Tests ============

    @Test
    fun `OnArchiveLink calls toggle archive use case`() = runTest {
        coEvery { toggleArchiveUseCase(any(), any()) } returns Result.Success(Unit)

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(HomeEvent.OnArchiveLink("test-id-1", true))
        advanceUntilIdle()

        coVerify { toggleArchiveUseCase("test-id-1", true) }
    }

    @Test
    fun `OnArchiveLink emits success event`() = runTest {
        coEvery { toggleArchiveUseCase(any(), any()) } returns Result.Success(Unit)

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.uiEvents.test {
            viewModel.onEvent(HomeEvent.OnArchiveLink("test-id-1", true))
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is HomeUiEvent.ShowArchiveToggled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ============ Delete Link Tests ============

    @Test
    fun `OnDeleteLink calls delete use case with soft delete`() = runTest {
        coEvery { deleteLinkUseCase(any(), softDelete = true) } returns Result.Success(Unit)

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(HomeEvent.OnDeleteLink("test-id-1"))
        advanceUntilIdle()

        coVerify { deleteLinkUseCase("test-id-1", softDelete = true) }
    }

    @Test
    fun `OnDeleteLink emits trashed event on success`() = runTest {
        coEvery { deleteLinkUseCase(any(), softDelete = true) } returns Result.Success(Unit)

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.uiEvents.test {
            viewModel.onEvent(HomeEvent.OnDeleteLink("test-id-1"))
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is HomeUiEvent.ShowLinkTrashed)
            assertEquals("test-id-1", (event as HomeUiEvent.ShowLinkTrashed).linkId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ============ Restore Link Tests ============

    @Test
    fun `OnRestoreLink calls restore use case`() = runTest {
        coEvery { restoreLinkUseCase(any()) } returns Result.Success(Unit)

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(HomeEvent.OnRestoreLink("test-id-1"))
        advanceUntilIdle()

        coVerify { restoreLinkUseCase("test-id-1") }
    }

    // ============ Clipboard URL Tests ============

    @Test
    fun `OnClipboardUrlDetected shows prompt for valid new URL`() = runTest {
        coEvery { linkRepository.existsByUrl(any()) } returns false

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(HomeEvent.OnClipboardUrlDetected("https://newurl.com"))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.showClipboardPrompt)
        assertEquals("https://newurl.com", state.clipboardUrl)
    }

    @Test
    fun `OnClipboardUrlDetected does not show prompt for existing URL`() = runTest {
        coEvery { linkRepository.existsByUrl("https://example.com") } returns true

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(HomeEvent.OnClipboardUrlDetected("https://example.com"))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.showClipboardPrompt)
    }

    @Test
    fun `OnClipboardUrlDetected does not show prompt for invalid URL`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(HomeEvent.OnClipboardUrlDetected("not-a-valid-url"))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.showClipboardPrompt)
    }

    @Test
    fun `OnDismissClipboardPrompt hides prompt and clears URL`() = runTest {
        coEvery { linkRepository.existsByUrl(any()) } returns false

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(HomeEvent.OnClipboardUrlDetected("https://newurl.com"))
        advanceUntilIdle()

        viewModel.onEvent(HomeEvent.OnDismissClipboardPrompt)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.showClipboardPrompt)
        assertEquals(null, state.clipboardUrl)
    }

    // ============ Selection Mode Tests ============

    @Test
    fun `OnEnterSelectionMode enables selection mode`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(HomeEvent.OnEnterSelectionMode)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isSelectionMode)
        assertTrue(viewModel.state.value.selectedLinkIds.isEmpty())
    }

    @Test
    fun `OnExitSelectionMode disables selection mode and clears selection`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(HomeEvent.OnEnterSelectionMode)
        viewModel.onEvent(HomeEvent.OnToggleLinkSelection("test-id-1"))
        advanceUntilIdle()

        viewModel.onEvent(HomeEvent.OnExitSelectionMode)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isSelectionMode)
        assertTrue(viewModel.state.value.selectedLinkIds.isEmpty())
    }

    @Test
    fun `OnToggleLinkSelection adds link to selection`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(HomeEvent.OnEnterSelectionMode)
        viewModel.onEvent(HomeEvent.OnToggleLinkSelection("test-id-1"))
        advanceUntilIdle()

        assertTrue(viewModel.state.value.selectedLinkIds.contains("test-id-1"))
    }

    @Test
    fun `OnToggleLinkSelection removes link from selection if already selected`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(HomeEvent.OnEnterSelectionMode)
        viewModel.onEvent(HomeEvent.OnToggleLinkSelection("test-id-1"))
        viewModel.onEvent(HomeEvent.OnToggleLinkSelection("test-id-1"))
        advanceUntilIdle()

        assertFalse(viewModel.state.value.selectedLinkIds.contains("test-id-1"))
    }

    @Test
    fun `OnSelectAll selects all links`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(HomeEvent.OnEnterSelectionMode)
        viewModel.onEvent(HomeEvent.OnSelectAll)
        advanceUntilIdle()

        assertEquals(viewModel.state.value.links.size, viewModel.state.value.selectedLinkIds.size)
    }

    @Test
    fun `OnDeselectAll clears selection`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(HomeEvent.OnEnterSelectionMode)
        viewModel.onEvent(HomeEvent.OnSelectAll)
        viewModel.onEvent(HomeEvent.OnDeselectAll)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.selectedLinkIds.isEmpty())
    }

    // ============ Bulk Operations Tests ============

    @Test
    fun `OnBulkDelete deletes selected links and exits selection mode`() = runTest {
        coEvery { deleteLinkUseCase(any(), softDelete = true) } returns Result.Success(Unit)

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(HomeEvent.OnEnterSelectionMode)
        viewModel.onEvent(HomeEvent.OnToggleLinkSelection("test-id-1"))
        viewModel.onEvent(HomeEvent.OnToggleLinkSelection("test-id-2"))
        advanceUntilIdle()

        viewModel.uiEvents.test {
            viewModel.onEvent(HomeEvent.OnBulkDelete)
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is HomeUiEvent.ShowBulkOperationResult)
            assertTrue((event as HomeUiEvent.ShowBulkOperationResult).message.contains("links moved to trash"))
            cancelAndIgnoreRemainingEvents()
        }

        assertFalse(viewModel.state.value.isSelectionMode)
    }

    @Test
    fun `OnBulkArchive archives selected links`() = runTest {
        coEvery { toggleArchiveUseCase(any(), any()) } returns Result.Success(Unit)

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(HomeEvent.OnEnterSelectionMode)
        viewModel.onEvent(HomeEvent.OnToggleLinkSelection("test-id-1"))
        advanceUntilIdle()

        viewModel.uiEvents.test {
            viewModel.onEvent(HomeEvent.OnBulkArchive)
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is HomeUiEvent.ShowBulkOperationResult)
            assertTrue((event as HomeUiEvent.ShowBulkOperationResult).message.contains("archived"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `OnBulkFavorite favorites selected links`() = runTest {
        coEvery { toggleFavoriteUseCase(any(), any()) } returns Result.Success(Unit)

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(HomeEvent.OnEnterSelectionMode)
        viewModel.onEvent(HomeEvent.OnToggleLinkSelection("test-id-1"))
        advanceUntilIdle()

        viewModel.uiEvents.test {
            viewModel.onEvent(HomeEvent.OnBulkFavorite)
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is HomeUiEvent.ShowBulkOperationResult)
            assertTrue((event as HomeUiEvent.ShowBulkOperationResult).message.contains("favorites"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ============ Advanced Filter Sheet Tests ============

    @Test
    fun `OnShowAdvancedFilterSheet shows sheet`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(HomeEvent.OnShowAdvancedFilterSheet)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.showAdvancedFilterSheet)
    }

    @Test
    fun `OnDismissAdvancedFilterSheet hides sheet`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(HomeEvent.OnShowAdvancedFilterSheet)
        viewModel.onEvent(HomeEvent.OnDismissAdvancedFilterSheet)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.showAdvancedFilterSheet)
    }

    // ============ Count Updates Tests ============

    @Test
    fun `observes link counts from repository`() = runTest {
        every { linkRepository.getAllLinksCount() } returns flowOf(10)
        every { linkRepository.getFavoriteLinksCount() } returns flowOf(5)
        every { linkRepository.getArchivedLinksCount() } returns flowOf(2)

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(10, state.allLinksCount)
        assertEquals(5, state.favoriteLinksCount)
        assertEquals(2, state.archivedLinksCount)
    }
}
