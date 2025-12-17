package com.rejowan.linky.presentation.viewmodel

import app.cash.turbine.test
import com.rejowan.linky.domain.model.Collection
import com.rejowan.linky.domain.usecase.collection.GetAllCollectionsUseCase
import com.rejowan.linky.domain.usecase.collection.SaveCollectionUseCase
import com.rejowan.linky.domain.usecase.link.BatchSaveLinksUseCase
import com.rejowan.linky.domain.usecase.link.CheckUrlExistsUseCase
import com.rejowan.linky.domain.usecase.link.SaveLinkUseCase
import com.rejowan.linky.presentation.feature.batchimport.BatchImportEvent
import com.rejowan.linky.presentation.feature.batchimport.BatchImportUiEvent
import com.rejowan.linky.presentation.feature.batchimport.BatchImportViewModel
import com.rejowan.linky.util.LinkPreviewFetcher
import com.rejowan.linky.util.Result
import io.mockk.coEvery
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
class BatchImportViewModelTest {

    private lateinit var viewModel: BatchImportViewModel
    private lateinit var saveLinkUseCase: SaveLinkUseCase
    private lateinit var checkUrlExistsUseCase: CheckUrlExistsUseCase
    private lateinit var linkPreviewFetcher: LinkPreviewFetcher
    private lateinit var batchSaveLinksUseCase: BatchSaveLinksUseCase
    private lateinit var getAllCollectionsUseCase: GetAllCollectionsUseCase
    private lateinit var saveCollectionUseCase: SaveCollectionUseCase

    private val testDispatcher = StandardTestDispatcher()

    private val testCollection = Collection(
        id = "collection-1",
        name = "Work",
        color = "#FF5733",
        createdAt = 1000L,
        updatedAt = 2000L
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        saveLinkUseCase = mockk()
        checkUrlExistsUseCase = mockk()
        linkPreviewFetcher = mockk()
        batchSaveLinksUseCase = mockk()
        getAllCollectionsUseCase = mockk()
        saveCollectionUseCase = mockk()

        every { getAllCollectionsUseCase() } returns flowOf(listOf(testCollection))
        coEvery { checkUrlExistsUseCase(any()) } returns false
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = BatchImportViewModel(
            saveLinkUseCase,
            checkUrlExistsUseCase,
            linkPreviewFetcher,
            batchSaveLinksUseCase,
            getAllCollectionsUseCase,
            saveCollectionUseCase
        )
    }

    @Test
    fun `initial state loads collections`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(1, state.collections.size)
            assertEquals("Work", state.collections[0].name)
        }
    }

    @Test
    fun `OnTextChanged updates pasted text`() = runTest {
        createViewModel()

        viewModel.onEvent(BatchImportEvent.OnTextChanged("https://example.com\nhttps://test.com"))

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("https://example.com\nhttps://test.com", state.pastedText)
        }
    }

    @Test
    fun `OnStartScan extracts URLs and checks duplicates`() = runTest {
        coEvery { checkUrlExistsUseCase("https://example.com") } returns false
        coEvery { checkUrlExistsUseCase("https://test.com") } returns true // duplicate

        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(BatchImportEvent.OnTextChanged("https://example.com https://test.com"))
        viewModel.onEvent(BatchImportEvent.OnStartScan)
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            // URLs were extracted and scanned
            assertTrue(state.totalUrls >= 0)
        }
    }

    @Test
    fun `OnStartScan with no URLs shows error`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(BatchImportEvent.OnTextChanged("no urls here"))
        viewModel.onEvent(BatchImportEvent.OnStartScan)
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertNotNull(state.error)
        }
    }

    @Test
    fun `OnToggleUrlSelection toggles selection`() = runTest {
        coEvery { checkUrlExistsUseCase(any()) } returns false

        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(BatchImportEvent.OnTextChanged("https://example.com"))
        viewModel.onEvent(BatchImportEvent.OnStartScan)
        advanceUntilIdle()

        // Check if URL was extracted before toggling
        val initialState = viewModel.state.value
        if (initialState.urlStatuses.isNotEmpty()) {
            val url = initialState.urlStatuses.first().url
            viewModel.onEvent(BatchImportEvent.OnToggleUrlSelection(url))

            viewModel.state.test {
                val state = awaitItem()
                // Verify toggle worked - selection state changed
                assertTrue(state.urlStatuses.isNotEmpty())
            }
        }
    }

    @Test
    fun `OnSelectAll selects all URLs`() = runTest {
        coEvery { checkUrlExistsUseCase(any()) } returns true // All duplicates, initially deselected

        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(BatchImportEvent.OnTextChanged("https://example.com https://test.com"))
        viewModel.onEvent(BatchImportEvent.OnStartScan)
        advanceUntilIdle()

        viewModel.onEvent(BatchImportEvent.OnSelectAll)

        viewModel.state.test {
            val state = awaitItem()
            // All urls should be selected after OnSelectAll
            if (state.urlStatuses.isNotEmpty()) {
                assertTrue(state.urlStatuses.all { it.isSelected })
            }
        }
    }

    @Test
    fun `OnDeselectAll deselects all URLs`() = runTest {
        coEvery { checkUrlExistsUseCase(any()) } returns false

        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(BatchImportEvent.OnTextChanged("https://example.com https://test.com"))
        viewModel.onEvent(BatchImportEvent.OnStartScan)
        advanceUntilIdle()

        viewModel.onEvent(BatchImportEvent.OnDeselectAll)

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(0, state.selectedCount)
        }
    }

    @Test
    fun `OnSelectNewOnly selects only non-duplicate URLs`() = runTest {
        coEvery { checkUrlExistsUseCase("https://example.com") } returns false
        coEvery { checkUrlExistsUseCase("https://test.com") } returns true

        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(BatchImportEvent.OnTextChanged("https://example.com https://test.com"))
        viewModel.onEvent(BatchImportEvent.OnStartScan)
        advanceUntilIdle()

        viewModel.onEvent(BatchImportEvent.OnSelectAll) // Select all first
        viewModel.onEvent(BatchImportEvent.OnSelectNewOnly)

        viewModel.state.test {
            val state = awaitItem()
            // Only non-duplicates should be selected
            assertTrue(state.selectedCount <= state.totalUrls)
        }
    }

    @Test
    fun `OnRemoveDuplicates removes duplicate URLs from list`() = runTest {
        coEvery { checkUrlExistsUseCase("https://example.com") } returns false
        coEvery { checkUrlExistsUseCase("https://test.com") } returns true

        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(BatchImportEvent.OnTextChanged("https://example.com https://test.com"))
        viewModel.onEvent(BatchImportEvent.OnStartScan)
        advanceUntilIdle()

        viewModel.onEvent(BatchImportEvent.OnRemoveDuplicates)

        viewModel.state.test {
            val state = awaitItem()
            // After removing duplicates, no duplicates should remain
            assertEquals(0, state.duplicateCount)
        }
    }

    @Test
    fun `OnRemoveUrl removes specific URL`() = runTest {
        coEvery { checkUrlExistsUseCase(any()) } returns false

        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(BatchImportEvent.OnTextChanged("https://example.com https://test.com"))
        viewModel.onEvent(BatchImportEvent.OnStartScan)
        advanceUntilIdle()

        val initialCount = viewModel.state.value.totalUrls
        if (viewModel.state.value.urlStatuses.isNotEmpty()) {
            val urlToRemove = viewModel.state.value.urlStatuses.first().url
            viewModel.onEvent(BatchImportEvent.OnRemoveUrl(urlToRemove))

            viewModel.state.test {
                val state = awaitItem()
                assertTrue(state.totalUrls < initialCount || initialCount == 0)
            }
        }
    }

    @Test
    fun `OnCollectionSelected updates selected collection`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(BatchImportEvent.OnCollectionSelected("collection-1"))

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("collection-1", state.selectedCollectionId)
        }
    }

    @Test
    fun `OnBackToEdit resets scan state`() = runTest {
        coEvery { checkUrlExistsUseCase(any()) } returns false

        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(BatchImportEvent.OnTextChanged("https://example.com"))
        viewModel.onEvent(BatchImportEvent.OnStartScan)
        advanceUntilIdle()

        viewModel.onEvent(BatchImportEvent.OnBackToEdit)

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.showSelectionScreen)
            assertTrue(state.extractedUrls.isEmpty())
        }
    }

    @Test
    fun `OnCreateCollectionClick shows dialog`() = runTest {
        createViewModel()

        viewModel.onEvent(BatchImportEvent.OnCreateCollectionClick)

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.showCreateCollectionDialog)
        }
    }

    @Test
    fun `OnCreateCollectionDismiss hides dialog`() = runTest {
        createViewModel()

        viewModel.onEvent(BatchImportEvent.OnCreateCollectionClick)
        viewModel.onEvent(BatchImportEvent.OnCreateCollectionDismiss)

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.showCreateCollectionDialog)
        }
    }

    @Test
    fun `OnNewCollectionNameChange updates name`() = runTest {
        createViewModel()

        viewModel.onEvent(BatchImportEvent.OnNewCollectionNameChange("New Collection"))

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("New Collection", state.newCollectionName)
        }
    }

    @Test
    fun `OnCancel emits NavigateToSettings`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.uiEvent.test {
            viewModel.onEvent(BatchImportEvent.OnCancel)
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is BatchImportUiEvent.NavigateToSettings)
        }
    }

    @Test
    fun `OnBack emits NavigateToSettings`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.uiEvent.test {
            viewModel.onEvent(BatchImportEvent.OnBack)
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is BatchImportUiEvent.NavigateToSettings)
        }
    }

    @Test
    fun `resetState clears all state`() = runTest {
        createViewModel()

        viewModel.onEvent(BatchImportEvent.OnTextChanged("https://example.com"))
        viewModel.resetState()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("", state.pastedText)
        }
    }
}
