package com.rejowan.linky.presentation.viewmodel

import app.cash.turbine.test
import com.rejowan.linky.data.local.preferences.ThemePreferences
import com.rejowan.linky.domain.repository.CollectionRepository
import com.rejowan.linky.domain.repository.LinkRepository
import com.rejowan.linky.domain.repository.SnapshotRepository
import com.rejowan.linky.domain.usecase.backup.ExportDataUseCase
import com.rejowan.linky.domain.usecase.backup.ImportDataUseCase
import com.rejowan.linky.presentation.feature.settings.SettingsEvent
import com.rejowan.linky.presentation.feature.settings.SettingsViewModel
import com.rejowan.linky.util.FileStorageManager
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
class SettingsViewModelTest {

    private lateinit var viewModel: SettingsViewModel
    private lateinit var linkRepository: LinkRepository
    private lateinit var collectionRepository: CollectionRepository
    private lateinit var snapshotRepository: SnapshotRepository
    private lateinit var themePreferences: ThemePreferences
    private lateinit var fileStorageManager: FileStorageManager
    private lateinit var exportDataUseCase: ExportDataUseCase
    private lateinit var importDataUseCase: ImportDataUseCase

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        linkRepository = mockk()
        collectionRepository = mockk()
        snapshotRepository = mockk()
        themePreferences = mockk()
        fileStorageManager = mockk()
        exportDataUseCase = mockk()
        importDataUseCase = mockk()

        coEvery { linkRepository.countLinks() } returns 10
        coEvery { collectionRepository.countCollections() } returns 5
        coEvery { snapshotRepository.getTotalStorageUsed() } returns 1024L * 1024L
        coEvery { fileStorageManager.getTotalStorageUsed() } returns 1024L * 1024L
        every { themePreferences.getTheme() } returns flowOf("System")
        every { linkRepository.getTrashedLinks() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = SettingsViewModel(
            linkRepository = linkRepository,
            collectionRepository = collectionRepository,
            snapshotRepository = snapshotRepository,
            themePreferences = themePreferences,
            fileStorageManager = fileStorageManager,
            exportDataUseCase = exportDataUseCase,
            importDataUseCase = importDataUseCase
        )
    }

    @Test
    fun `initial state loads statistics`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(10, state.totalLinks)
            assertEquals(5, state.totalCollections)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `OnThemeChange changes theme`() = runTest {
        coEvery { themePreferences.saveTheme("Dark") } returns Unit

        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(SettingsEvent.OnThemeChange("Dark"))
        advanceUntilIdle()

        coVerify { themePreferences.saveTheme("Dark") }
    }

    @Test
    fun `OnClearCache clears cache and refreshes`() = runTest {
        coEvery { fileStorageManager.clearPreviewCache() } returns true

        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(SettingsEvent.OnClearCache)
        advanceUntilIdle()

        coVerify { fileStorageManager.clearPreviewCache() }
    }

    @Test
    fun `OnClearCache with failure returns to non-loading state`() = runTest {
        coEvery { fileStorageManager.clearPreviewCache() } returns false

        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(SettingsEvent.OnClearCache)
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            // After clearing, loading should be false
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `OnRefresh reloads settings`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(SettingsEvent.OnRefresh)
        advanceUntilIdle()

        coVerify(atLeast = 2) { linkRepository.countLinks() }
    }

    @Test
    fun `observes theme changes`() = runTest {
        every { themePreferences.getTheme() } returns flowOf("Dark")

        createViewModel()
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("Dark", state.theme)
        }
    }

    @Test
    fun `observes trashed links count`() = runTest {
        val trashedLinks = listOf(
            mockk<com.rejowan.linky.domain.model.Link>(relaxed = true),
            mockk<com.rejowan.linky.domain.model.Link>(relaxed = true)
        )
        every { linkRepository.getTrashedLinks() } returns flowOf(trashedLinks)

        createViewModel()
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(2, state.totalTrashedLinks)
        }
    }

    @Test
    fun `handles error when loading statistics fails`() = runTest {
        coEvery { linkRepository.countLinks() } throws RuntimeException("Database error")

        createViewModel()
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertNotNull(state.error)
            assertFalse(state.isLoading)
        }
    }
}
