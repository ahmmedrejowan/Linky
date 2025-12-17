package com.rejowan.linky.domain.usecase.link

import app.cash.turbine.test
import com.rejowan.linky.domain.model.Link
import com.rejowan.linky.domain.repository.LinkRepository
import com.rejowan.linky.util.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LinkUseCasesTest {

    private lateinit var linkRepository: LinkRepository

    private val testLink = Link(
        id = "link-1",
        title = "Test Link",
        url = "https://example.com",
        description = "Test description",
        note = "Test note"
    )

    @Before
    fun setup() {
        linkRepository = mockk(relaxed = true)
    }

    // ============ GetAllLinksUseCase Tests ============

    @Test
    fun `GetAllLinksUseCase returns all active links`() = runTest {
        val links = listOf(testLink)
        every { linkRepository.getAllActiveLinks() } returns flowOf(links)

        val useCase = GetAllLinksUseCase(linkRepository)

        useCase().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals(testLink.id, result[0].id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ============ GetFavoriteLinksUseCase Tests ============

    @Test
    fun `GetFavoriteLinksUseCase returns favorite links`() = runTest {
        val favoriteLink = testLink.copy(isFavorite = true)
        every { linkRepository.getFavoriteLinks() } returns flowOf(listOf(favoriteLink))

        val useCase = GetFavoriteLinksUseCase(linkRepository)

        useCase().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertTrue(result[0].isFavorite)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ============ GetArchivedLinksUseCase Tests ============

    @Test
    fun `GetArchivedLinksUseCase returns archived links`() = runTest {
        val archivedLink = testLink.copy(isArchived = true)
        every { linkRepository.getArchivedLinks() } returns flowOf(listOf(archivedLink))

        val useCase = GetArchivedLinksUseCase(linkRepository)

        useCase().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertTrue(result[0].isArchived)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ============ GetTrashedLinksUseCase Tests ============

    @Test
    fun `GetTrashedLinksUseCase returns trashed links`() = runTest {
        val trashedLink = testLink.copy(deletedAt = System.currentTimeMillis())
        every { linkRepository.getTrashedLinks() } returns flowOf(listOf(trashedLink))

        val useCase = GetTrashedLinksUseCase(linkRepository)

        useCase().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertTrue(result[0].isDeleted)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ============ GetLinkByIdUseCase Tests ============

    @Test
    fun `GetLinkByIdUseCase returns link when exists`() = runTest {
        every { linkRepository.getLinkById("link-1") } returns flowOf(testLink)

        val useCase = GetLinkByIdUseCase(linkRepository)

        useCase("link-1").test {
            val result = awaitItem()
            assertEquals(testLink.id, result?.id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GetLinkByIdUseCase returns null when not exists`() = runTest {
        every { linkRepository.getLinkById("non-existent") } returns flowOf(null)

        val useCase = GetLinkByIdUseCase(linkRepository)

        useCase("non-existent").test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ============ GetLinksByCollectionUseCase Tests ============

    @Test
    fun `GetLinksByCollectionUseCase returns links for collection`() = runTest {
        val linkInCollection = testLink.copy(collectionId = "collection-1")
        every { linkRepository.getLinksByCollection("collection-1") } returns flowOf(listOf(linkInCollection))

        val useCase = GetLinksByCollectionUseCase(linkRepository)

        useCase("collection-1").test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("collection-1", result[0].collectionId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ============ SearchLinksUseCase Tests ============

    @Test
    fun `SearchLinksUseCase returns matching links`() = runTest {
        every { linkRepository.searchLinks("example") } returns flowOf(listOf(testLink))

        val useCase = SearchLinksUseCase(linkRepository)

        useCase("example").test {
            val result = awaitItem()
            assertEquals(1, result.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ============ SaveLinkUseCase Tests ============

    @Test
    fun `SaveLinkUseCase saves valid link`() = runTest {
        coEvery { linkRepository.saveLink(any()) } returns Result.Success(Unit)

        val useCase = SaveLinkUseCase(linkRepository)

        val result = useCase(testLink)

        assertTrue(result is Result.Success)
        coVerify { linkRepository.saveLink(any()) }
    }

    @Test
    fun `SaveLinkUseCase returns error for invalid URL`() = runTest {
        val useCase = SaveLinkUseCase(linkRepository)

        val invalidLink = testLink.copy(url = "invalid-url")
        val result = useCase(invalidLink)

        assertTrue(result is Result.Error)
    }

    @Test
    fun `SaveLinkUseCase returns error for empty title`() = runTest {
        val useCase = SaveLinkUseCase(linkRepository)

        val invalidLink = testLink.copy(title = "")
        val result = useCase(invalidLink)

        assertTrue(result is Result.Error)
    }

    @Test
    fun `SaveLinkUseCase sets hideFromHome to false when no collection`() = runTest {
        coEvery { linkRepository.saveLink(any()) } returns Result.Success(Unit)

        val useCase = SaveLinkUseCase(linkRepository)

        val linkWithHideFromHome = testLink.copy(hideFromHome = true, collectionId = null)
        val result = useCase(linkWithHideFromHome)

        assertTrue(result is Result.Success)
        coVerify { linkRepository.saveLink(match { !it.hideFromHome }) }
    }

    // ============ UpdateLinkUseCase Tests ============

    @Test
    fun `UpdateLinkUseCase updates valid link`() = runTest {
        coEvery { linkRepository.updateLink(any()) } returns Result.Success(Unit)

        val useCase = UpdateLinkUseCase(linkRepository)

        val result = useCase(testLink)

        assertTrue(result is Result.Success)
        coVerify { linkRepository.updateLink(any()) }
    }

    @Test
    fun `UpdateLinkUseCase returns error for invalid URL`() = runTest {
        val useCase = UpdateLinkUseCase(linkRepository)

        val invalidLink = testLink.copy(url = "invalid-url")
        val result = useCase(invalidLink)

        assertTrue(result is Result.Error)
    }

    // ============ DeleteLinkUseCase Tests ============

    @Test
    fun `DeleteLinkUseCase soft deletes by default`() = runTest {
        coEvery { linkRepository.softDeleteLink("link-1") } returns Result.Success(Unit)

        val useCase = DeleteLinkUseCase(linkRepository)

        val result = useCase("link-1")

        assertTrue(result is Result.Success)
        coVerify { linkRepository.softDeleteLink("link-1") }
    }

    @Test
    fun `DeleteLinkUseCase hard deletes when specified`() = runTest {
        coEvery { linkRepository.deleteLink("link-1") } returns Result.Success(Unit)

        val useCase = DeleteLinkUseCase(linkRepository)

        val result = useCase("link-1", softDelete = false)

        assertTrue(result is Result.Success)
        coVerify { linkRepository.deleteLink("link-1") }
    }

    // ============ RestoreLinkUseCase Tests ============

    @Test
    fun `RestoreLinkUseCase restores link`() = runTest {
        coEvery { linkRepository.restoreLink("link-1") } returns Result.Success(Unit)

        val useCase = RestoreLinkUseCase(linkRepository)

        val result = useCase("link-1")

        assertTrue(result is Result.Success)
        coVerify { linkRepository.restoreLink("link-1") }
    }

    // ============ ToggleFavoriteUseCase Tests ============

    @Test
    fun `ToggleFavoriteUseCase toggles favorite status`() = runTest {
        coEvery { linkRepository.toggleFavorite("link-1", true) } returns Result.Success(Unit)

        val useCase = ToggleFavoriteUseCase(linkRepository)

        val result = useCase("link-1", true)

        assertTrue(result is Result.Success)
        coVerify { linkRepository.toggleFavorite("link-1", true) }
    }

    // ============ ToggleArchiveUseCase Tests ============

    @Test
    fun `ToggleArchiveUseCase toggles archive status`() = runTest {
        coEvery { linkRepository.toggleArchive("link-1", true) } returns Result.Success(Unit)

        val useCase = ToggleArchiveUseCase(linkRepository)

        val result = useCase("link-1", true)

        assertTrue(result is Result.Success)
        coVerify { linkRepository.toggleArchive("link-1", true) }
    }

    // ============ CheckUrlExistsUseCase Tests ============

    @Test
    fun `CheckUrlExistsUseCase returns true when URL exists`() = runTest {
        coEvery { linkRepository.existsByUrl("https://example.com") } returns true

        val useCase = CheckUrlExistsUseCase(linkRepository)

        val result = useCase("https://example.com")

        assertTrue(result)
    }

    @Test
    fun `CheckUrlExistsUseCase returns false when URL not exists`() = runTest {
        coEvery { linkRepository.existsByUrl("https://nonexistent.com") } returns false

        val useCase = CheckUrlExistsUseCase(linkRepository)

        val result = useCase("https://nonexistent.com")

        assertFalse(result)
    }

    // ============ BatchSaveLinksUseCase Tests ============

    @Test
    fun `BatchSaveLinksUseCase saves all links successfully`() = runTest {
        coEvery { linkRepository.saveLink(any()) } returns Result.Success(Unit)

        val useCase = BatchSaveLinksUseCase(linkRepository)
        val links = listOf(
            testLink,
            testLink.copy(id = "link-2", url = "https://second.com")
        )

        val result = useCase(links)

        assertEquals(2, result.totalSuccess)
        assertEquals(0, result.totalFailed)
        assertTrue(result.isCompleteSuccess)
        assertFalse(result.hasFailures)
    }

    @Test
    fun `BatchSaveLinksUseCase handles partial failures`() = runTest {
        coEvery { linkRepository.saveLink(match { it.id == "link-1" }) } returns Result.Success(Unit)
        coEvery { linkRepository.saveLink(match { it.id == "link-2" }) } returns Result.Error(RuntimeException("Error"))

        val useCase = BatchSaveLinksUseCase(linkRepository)
        val links = listOf(
            testLink,
            testLink.copy(id = "link-2", url = "https://second.com")
        )

        val result = useCase(links)

        assertEquals(1, result.totalSuccess)
        assertEquals(1, result.totalFailed)
        assertFalse(result.isCompleteSuccess)
        assertTrue(result.hasFailures)
    }

    @Test
    fun `BatchSaveLinksUseCase handles all failures`() = runTest {
        coEvery { linkRepository.saveLink(any()) } returns Result.Error(RuntimeException("Error"))

        val useCase = BatchSaveLinksUseCase(linkRepository)
        val links = listOf(
            testLink,
            testLink.copy(id = "link-2", url = "https://second.com")
        )

        val result = useCase(links)

        assertEquals(0, result.totalSuccess)
        assertEquals(2, result.totalFailed)
        assertFalse(result.isCompleteSuccess)
        assertTrue(result.hasFailures)
    }
}
