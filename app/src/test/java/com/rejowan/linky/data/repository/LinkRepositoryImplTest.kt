package com.rejowan.linky.data.repository

import android.content.Context
import app.cash.turbine.test
import com.rejowan.linky.data.local.database.dao.LinkDao
import com.rejowan.linky.data.local.database.entity.LinkEntity
import com.rejowan.linky.domain.model.Link
import com.rejowan.linky.util.Result
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class LinkRepositoryImplTest {

    private lateinit var linkDao: LinkDao
    private lateinit var context: Context
    private lateinit var repository: LinkRepositoryImpl

    private val testLinkEntity = LinkEntity(
        id = "test-id-1",
        title = "Test Link",
        description = "Test Description",
        url = "https://example.com",
        note = "Test note",
        collectionId = null,
        previewImagePath = null,
        previewUrl = null,
        isFavorite = false,
        isArchived = false,
        hideFromHome = false,
        deletedAt = null,
        createdAt = 1000L,
        updatedAt = 2000L
    )

    private val testLink = Link(
        id = "test-id-1",
        title = "Test Link",
        description = "Test Description",
        url = "https://example.com",
        note = "Test note",
        collectionId = null,
        previewImagePath = null,
        previewUrl = null,
        isFavorite = false,
        isArchived = false,
        hideFromHome = false,
        deletedAt = null,
        createdAt = 1000L,
        updatedAt = 2000L
    )

    @Before
    fun setup() {
        linkDao = mockk(relaxed = true)
        context = mockk(relaxed = true)
        repository = LinkRepositoryImpl(linkDao, context)
    }

    // ============ Get Operations Tests ============

    @Test
    fun `getAllActiveLinks returns mapped domain models`() = runTest {
        val entities = listOf(testLinkEntity)
        every { linkDao.getAllActiveLinks() } returns flowOf(entities)

        repository.getAllActiveLinks().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals(testLink.id, result[0].id)
            assertEquals(testLink.title, result[0].title)
            assertEquals(testLink.url, result[0].url)
            cancelAndIgnoreRemainingEvents()
        }

        verify { linkDao.getAllActiveLinks() }
    }

    @Test
    fun `getAllActiveLinks returns empty list when no links`() = runTest {
        every { linkDao.getAllActiveLinks() } returns flowOf(emptyList())

        repository.getAllActiveLinks().test {
            val result = awaitItem()
            assertTrue(result.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getAllLinks returns all links including deleted`() = runTest {
        val deletedEntity = testLinkEntity.copy(id = "deleted-id", deletedAt = 3000L)
        every { linkDao.getAllLinks() } returns flowOf(listOf(testLinkEntity, deletedEntity))

        repository.getAllLinks().test {
            val result = awaitItem()
            assertEquals(2, result.size)
            cancelAndIgnoreRemainingEvents()
        }

        verify { linkDao.getAllLinks() }
    }

    @Test
    fun `getFavoriteLinks returns only favorite links`() = runTest {
        val favoriteEntity = testLinkEntity.copy(isFavorite = true)
        every { linkDao.getFavoriteLinks() } returns flowOf(listOf(favoriteEntity))

        repository.getFavoriteLinks().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertTrue(result[0].isFavorite)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getArchivedLinks returns only archived links`() = runTest {
        val archivedEntity = testLinkEntity.copy(isArchived = true)
        every { linkDao.getArchivedLinks() } returns flowOf(listOf(archivedEntity))

        repository.getArchivedLinks().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertTrue(result[0].isArchived)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getTrashedLinks returns only deleted links`() = runTest {
        val trashedEntity = testLinkEntity.copy(deletedAt = 3000L)
        every { linkDao.getTrashedLinks() } returns flowOf(listOf(trashedEntity))

        repository.getTrashedLinks().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertTrue(result[0].isDeleted)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getLinksByCollection returns links for specific collection`() = runTest {
        val collectionId = "collection-123"
        val entityWithCollection = testLinkEntity.copy(collectionId = collectionId)
        every { linkDao.getLinksByCollection(collectionId) } returns flowOf(listOf(entityWithCollection))

        repository.getLinksByCollection(collectionId).test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals(collectionId, result[0].collectionId)
            cancelAndIgnoreRemainingEvents()
        }

        verify { linkDao.getLinksByCollection(collectionId) }
    }

    @Test
    fun `searchLinks returns matching links`() = runTest {
        val query = "test"
        every { linkDao.searchLinks(query) } returns flowOf(listOf(testLinkEntity))

        repository.searchLinks(query).test {
            val result = awaitItem()
            assertEquals(1, result.size)
            cancelAndIgnoreRemainingEvents()
        }

        verify { linkDao.searchLinks(query) }
    }

    @Test
    fun `getLinkById returns link when exists`() = runTest {
        every { linkDao.getByIdFlow("test-id-1") } returns flowOf(testLinkEntity)

        repository.getLinkById("test-id-1").test {
            val result = awaitItem()
            assertEquals(testLink.id, result?.id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getLinkById returns null when not exists`() = runTest {
        every { linkDao.getByIdFlow("non-existent") } returns flowOf(null)

        repository.getLinkById("non-existent").test {
            val result = awaitItem()
            assertNull(result)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getLinkByIdOnce returns link when exists`() = runTest {
        coEvery { linkDao.getById("test-id-1") } returns testLinkEntity

        val result = repository.getLinkByIdOnce("test-id-1")
        assertEquals(testLink.id, result?.id)

        coVerify { linkDao.getById("test-id-1") }
    }

    @Test
    fun `getLinkByIdOnce returns null when not exists`() = runTest {
        coEvery { linkDao.getById("non-existent") } returns null

        val result = repository.getLinkByIdOnce("non-existent")
        assertNull(result)
    }

    @Test
    fun `existsByUrl returns true when url exists`() = runTest {
        coEvery { linkDao.existsByUrl("https://example.com") } returns true

        val result = repository.existsByUrl("https://example.com")
        assertTrue(result)
    }

    @Test
    fun `existsByUrl returns false when url not exists`() = runTest {
        coEvery { linkDao.existsByUrl("https://nonexistent.com") } returns false

        val result = repository.existsByUrl("https://nonexistent.com")
        assertFalse(result)
    }

    // ============ Create/Update Operations Tests ============

    @Test
    fun `saveLink returns Success when insert succeeds`() = runTest {
        coEvery { linkDao.insert(any()) } just Runs

        val result = repository.saveLink(testLink)

        assertTrue(result is Result.Success)
        coVerify { linkDao.insert(any()) }
    }

    @Test
    fun `saveLink returns Error when insert fails`() = runTest {
        coEvery { linkDao.insert(any()) } throws RuntimeException("Database error")

        val result = repository.saveLink(testLink)

        assertTrue(result is Result.Error)
    }

    @Test
    fun `updateLink returns Success when update succeeds`() = runTest {
        coEvery { linkDao.update(any()) } just Runs

        val result = repository.updateLink(testLink)

        assertTrue(result is Result.Success)
        coVerify { linkDao.update(any()) }
    }

    @Test
    fun `updateLink returns Error when update fails`() = runTest {
        coEvery { linkDao.update(any()) } throws RuntimeException("Database error")

        val result = repository.updateLink(testLink)

        assertTrue(result is Result.Error)
    }

    // ============ Delete Operations Tests ============

    @Test
    fun `deleteLink returns Success when link exists and delete succeeds`() = runTest {
        coEvery { linkDao.getById("test-id-1") } returns testLinkEntity
        coEvery { linkDao.delete(any()) } just Runs

        val result = repository.deleteLink("test-id-1")

        assertTrue(result is Result.Success)
        coVerify { linkDao.delete(testLinkEntity) }
    }

    @Test
    fun `deleteLink returns Error when link not found`() = runTest {
        coEvery { linkDao.getById("non-existent") } returns null

        val result = repository.deleteLink("non-existent")

        assertTrue(result is Result.Error)
        assertEquals("Link not found", (result as Result.Error).exception.message)
    }

    @Test
    fun `deleteLink returns Error when delete fails`() = runTest {
        coEvery { linkDao.getById("test-id-1") } returns testLinkEntity
        coEvery { linkDao.delete(any()) } throws RuntimeException("Delete failed")

        val result = repository.deleteLink("test-id-1")

        assertTrue(result is Result.Error)
    }

    @Test
    fun `softDeleteLink returns Success when succeeds`() = runTest {
        coEvery { linkDao.softDelete(any(), any()) } just Runs

        val result = repository.softDeleteLink("test-id-1")

        assertTrue(result is Result.Success)
        coVerify { linkDao.softDelete("test-id-1", any()) }
    }

    @Test
    fun `softDeleteLink returns Error when fails`() = runTest {
        coEvery { linkDao.softDelete(any(), any()) } throws RuntimeException("Soft delete failed")

        val result = repository.softDeleteLink("test-id-1")

        assertTrue(result is Result.Error)
    }

    @Test
    fun `restoreLink returns Success when succeeds`() = runTest {
        coEvery { linkDao.restore(any()) } just Runs

        val result = repository.restoreLink("test-id-1")

        assertTrue(result is Result.Success)
        coVerify { linkDao.restore("test-id-1") }
    }

    @Test
    fun `restoreLink returns Error when fails`() = runTest {
        coEvery { linkDao.restore(any()) } throws RuntimeException("Restore failed")

        val result = repository.restoreLink("test-id-1")

        assertTrue(result is Result.Error)
    }

    @Test
    fun `deleteAllLinks returns Success when succeeds`() = runTest {
        coEvery { linkDao.deleteAll() } just Runs

        val result = repository.deleteAllLinks()

        assertTrue(result is Result.Success)
        coVerify { linkDao.deleteAll() }
    }

    @Test
    fun `deleteAllLinks returns Error when fails`() = runTest {
        coEvery { linkDao.deleteAll() } throws RuntimeException("Delete all failed")

        val result = repository.deleteAllLinks()

        assertTrue(result is Result.Error)
    }

    // ============ Toggle Operations Tests ============

    @Test
    fun `toggleFavorite returns Success when succeeds`() = runTest {
        coEvery { linkDao.toggleFavorite(any(), any(), any()) } just Runs

        val result = repository.toggleFavorite("test-id-1", true)

        assertTrue(result is Result.Success)
        coVerify { linkDao.toggleFavorite("test-id-1", true, any()) }
    }

    @Test
    fun `toggleFavorite returns Error when fails`() = runTest {
        coEvery { linkDao.toggleFavorite(any(), any(), any()) } throws RuntimeException("Toggle failed")

        val result = repository.toggleFavorite("test-id-1", true)

        assertTrue(result is Result.Error)
    }

    @Test
    fun `toggleArchive returns Success when succeeds`() = runTest {
        coEvery { linkDao.toggleArchive(any(), any(), any()) } just Runs

        val result = repository.toggleArchive("test-id-1", true)

        assertTrue(result is Result.Success)
        coVerify { linkDao.toggleArchive("test-id-1", true, any()) }
    }

    @Test
    fun `toggleArchive returns Error when fails`() = runTest {
        coEvery { linkDao.toggleArchive(any(), any(), any()) } throws RuntimeException("Toggle failed")

        val result = repository.toggleArchive("test-id-1", true)

        assertTrue(result is Result.Error)
    }

    // ============ Count Operations Tests ============

    @Test
    fun `countLinks returns correct count`() = runTest {
        coEvery { linkDao.countLinks() } returns 5

        val result = repository.countLinks()

        assertEquals(5, result)
    }

    @Test
    fun `getAllLinksCount emits count updates`() = runTest {
        every { linkDao.getAllLinksCount() } returns flowOf(10)

        repository.getAllLinksCount().test {
            assertEquals(10, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getFavoriteLinksCount emits count updates`() = runTest {
        every { linkDao.getFavoriteLinksCount() } returns flowOf(3)

        repository.getFavoriteLinksCount().test {
            assertEquals(3, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getArchivedLinksCount emits count updates`() = runTest {
        every { linkDao.getArchivedLinksCount() } returns flowOf(2)

        repository.getArchivedLinksCount().test {
            assertEquals(2, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ============ Advanced Filtering Tests ============

    @Test
    fun `getAllActiveUrls returns list of urls`() = runTest {
        val urls = listOf("https://example1.com", "https://example2.com")
        coEvery { linkDao.getAllActiveUrls() } returns urls

        val result = repository.getAllActiveUrls()

        assertEquals(2, result.size)
        assertEquals("https://example1.com", result[0])
    }
}
