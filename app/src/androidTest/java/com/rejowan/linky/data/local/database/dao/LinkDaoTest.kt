package com.rejowan.linky.data.local.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.rejowan.linky.data.local.database.AppDatabase
import com.rejowan.linky.data.local.database.entity.LinkEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LinkDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var linkDao: LinkDao

    private val testLink = LinkEntity(
        id = "test-link-1",
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
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        linkDao = database.linkDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ============ Insert Tests ============

    @Test
    fun insertLink_savesLinkToDatabase() = runTest {
        linkDao.insert(testLink)

        val result = linkDao.getById(testLink.id)
        assertNotNull(result)
        assertEquals(testLink.id, result?.id)
        assertEquals(testLink.title, result?.title)
        assertEquals(testLink.url, result?.url)
    }

    @Test
    fun insertLink_replacesOnConflict() = runTest {
        linkDao.insert(testLink)

        val updatedLink = testLink.copy(title = "Updated Title")
        linkDao.insert(updatedLink)

        val result = linkDao.getById(testLink.id)
        assertEquals("Updated Title", result?.title)
    }

    @Test
    fun insertAllLinks_savesMultipleLinks() = runTest {
        val links = listOf(
            testLink,
            testLink.copy(id = "test-link-2", url = "https://second.com"),
            testLink.copy(id = "test-link-3", url = "https://third.com")
        )
        linkDao.insertAll(links)

        val count = linkDao.countLinks()
        assertEquals(3, count)
    }

    // ============ Get Tests ============

    @Test
    fun getById_returnsNullForNonExistentId() = runTest {
        val result = linkDao.getById("non-existent-id")
        assertNull(result)
    }

    @Test
    fun getByIdFlow_emitsUpdatesWhenLinkChanges() = runTest {
        linkDao.insert(testLink)

        linkDao.getByIdFlow(testLink.id).test {
            val initial = awaitItem()
            assertEquals("Test Link", initial?.title)

            // Update the link
            linkDao.update(testLink.copy(title = "Updated Title"))

            val updated = awaitItem()
            assertEquals("Updated Title", updated?.title)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getAllActiveLinks_returnsOnlyActiveLinks() = runTest {
        val activeLink = testLink
        val archivedLink = testLink.copy(id = "archived", isArchived = true)
        val deletedLink = testLink.copy(id = "deleted", deletedAt = System.currentTimeMillis())
        val hiddenLink = testLink.copy(id = "hidden", hideFromHome = true)

        linkDao.insertAll(listOf(activeLink, archivedLink, deletedLink, hiddenLink))

        linkDao.getAllActiveLinks().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals(activeLink.id, result[0].id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getFavoriteLinks_returnsOnlyFavorites() = runTest {
        val normalLink = testLink
        val favoriteLink = testLink.copy(id = "favorite", isFavorite = true)

        linkDao.insertAll(listOf(normalLink, favoriteLink))

        linkDao.getFavoriteLinks().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertTrue(result[0].isFavorite)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getArchivedLinks_returnsOnlyArchived() = runTest {
        val normalLink = testLink
        val archivedLink = testLink.copy(id = "archived", isArchived = true)

        linkDao.insertAll(listOf(normalLink, archivedLink))

        linkDao.getArchivedLinks().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertTrue(result[0].isArchived)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getTrashedLinks_returnsOnlyDeleted() = runTest {
        val normalLink = testLink
        val deletedLink = testLink.copy(id = "deleted", deletedAt = System.currentTimeMillis())

        linkDao.insertAll(listOf(normalLink, deletedLink))

        linkDao.getTrashedLinks().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertNotNull(result[0].deletedAt)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getLinksByCollection_returnsLinksInCollection() = runTest {
        val collectionId = "collection-1"
        val linkInCollection = testLink.copy(collectionId = collectionId)
        val linkOutsideCollection = testLink.copy(id = "other", collectionId = null)

        linkDao.insertAll(listOf(linkInCollection, linkOutsideCollection))

        linkDao.getLinksByCollection(collectionId).test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals(collectionId, result[0].collectionId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ============ Search Tests ============

    @Test
    fun searchLinks_findsByTitle() = runTest {
        val link1 = testLink.copy(title = "Android Development Guide")
        val link2 = testLink.copy(id = "2", title = "iOS Guide")

        linkDao.insertAll(listOf(link1, link2))

        linkDao.searchLinks("Android").test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals(link1.id, result[0].id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun searchLinks_findsByUrl() = runTest {
        val link1 = testLink.copy(url = "https://github.com/repo")
        val link2 = testLink.copy(id = "2", url = "https://gitlab.com/repo")

        linkDao.insertAll(listOf(link1, link2))

        linkDao.searchLinks("github").test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals(link1.id, result[0].id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun searchLinks_findsByNote() = runTest {
        val link1 = testLink.copy(note = "Important bookmark for later")
        val link2 = testLink.copy(id = "2", note = "Regular link")

        linkDao.insertAll(listOf(link1, link2))

        linkDao.searchLinks("Important").test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals(link1.id, result[0].id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun searchLinks_caseInsensitive() = runTest {
        val link = testLink.copy(title = "Android Guide")
        linkDao.insert(link)

        linkDao.searchLinks("android").test {
            val result = awaitItem()
            assertEquals(1, result.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ============ URL Exists Tests ============

    @Test
    fun existsByUrl_returnsTrueWhenExists() = runTest {
        linkDao.insert(testLink)

        val exists = linkDao.existsByUrl(testLink.url)
        assertTrue(exists)
    }

    @Test
    fun existsByUrl_returnsFalseWhenNotExists() = runTest {
        val exists = linkDao.existsByUrl("https://nonexistent.com")
        assertFalse(exists)
    }

    @Test
    fun existsByUrl_excludesDeletedLinks() = runTest {
        val deletedLink = testLink.copy(deletedAt = System.currentTimeMillis())
        linkDao.insert(deletedLink)

        val exists = linkDao.existsByUrl(testLink.url)
        assertFalse(exists)
    }

    // ============ Update Tests ============

    @Test
    fun updateLink_modifiesExistingLink() = runTest {
        linkDao.insert(testLink)

        val updated = testLink.copy(
            title = "Updated Title",
            description = "Updated Description"
        )
        linkDao.update(updated)

        val result = linkDao.getById(testLink.id)
        assertEquals("Updated Title", result?.title)
        assertEquals("Updated Description", result?.description)
    }

    // ============ Delete Tests ============

    @Test
    fun deleteLink_removesFromDatabase() = runTest {
        linkDao.insert(testLink)

        linkDao.delete(testLink)

        val result = linkDao.getById(testLink.id)
        assertNull(result)
    }

    @Test
    fun softDelete_setsDeletedAt() = runTest {
        linkDao.insert(testLink)

        linkDao.softDelete(testLink.id)

        val result = linkDao.getById(testLink.id)
        assertNotNull(result?.deletedAt)
    }

    @Test
    fun restore_clearsDeletedAt() = runTest {
        val deletedLink = testLink.copy(deletedAt = System.currentTimeMillis())
        linkDao.insert(deletedLink)

        linkDao.restore(testLink.id)

        val result = linkDao.getById(testLink.id)
        assertNull(result?.deletedAt)
    }

    // ============ Toggle Tests ============

    @Test
    fun toggleFavorite_updatesFavoriteStatus() = runTest {
        linkDao.insert(testLink)

        linkDao.toggleFavorite(testLink.id, true)

        val result = linkDao.getById(testLink.id)
        assertTrue(result?.isFavorite == true)
    }

    @Test
    fun toggleFavorite_canUnfavorite() = runTest {
        val favoriteLink = testLink.copy(isFavorite = true)
        linkDao.insert(favoriteLink)

        linkDao.toggleFavorite(testLink.id, false)

        val result = linkDao.getById(testLink.id)
        assertFalse(result?.isFavorite == true)
    }

    @Test
    fun toggleArchive_updatesArchiveStatus() = runTest {
        linkDao.insert(testLink)

        linkDao.toggleArchive(testLink.id, true)

        val result = linkDao.getById(testLink.id)
        assertTrue(result?.isArchived == true)
    }

    @Test
    fun toggleArchive_canUnarchive() = runTest {
        val archivedLink = testLink.copy(isArchived = true)
        linkDao.insert(archivedLink)

        linkDao.toggleArchive(testLink.id, false)

        val result = linkDao.getById(testLink.id)
        assertFalse(result?.isArchived == true)
    }

    // ============ Count Tests ============

    @Test
    fun countLinks_returnsCorrectCount() = runTest {
        linkDao.insertAll(listOf(
            testLink,
            testLink.copy(id = "2"),
            testLink.copy(id = "3")
        ))

        val count = linkDao.countLinks()
        assertEquals(3, count)
    }

    @Test
    fun countLinks_excludesDeletedLinks() = runTest {
        linkDao.insertAll(listOf(
            testLink,
            testLink.copy(id = "2", deletedAt = System.currentTimeMillis())
        ))

        val count = linkDao.countLinks()
        assertEquals(1, count)
    }

    @Test
    fun getAllLinksCount_emitsUpdates() = runTest {
        linkDao.getAllLinksCount().test {
            assertEquals(0, awaitItem())

            linkDao.insert(testLink)
            assertEquals(1, awaitItem())

            linkDao.insert(testLink.copy(id = "2"))
            assertEquals(2, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getFavoriteLinksCount_countsOnlyFavorites() = runTest {
        linkDao.insertAll(listOf(
            testLink,
            testLink.copy(id = "2", isFavorite = true),
            testLink.copy(id = "3", isFavorite = true)
        ))

        linkDao.getFavoriteLinksCount().test {
            val count = awaitItem()
            assertEquals(2, count)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getArchivedLinksCount_countsOnlyArchived() = runTest {
        linkDao.insertAll(listOf(
            testLink,
            testLink.copy(id = "2", isArchived = true),
            testLink.copy(id = "3", isArchived = true)
        ))

        linkDao.getArchivedLinksCount().test {
            val count = awaitItem()
            assertEquals(2, count)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ============ Advanced Filtering Tests ============

    @Test
    fun getAllActiveUrls_returnsUrlsForActiveLinks() = runTest {
        linkDao.insertAll(listOf(
            testLink.copy(url = "https://example1.com"),
            testLink.copy(id = "2", url = "https://example2.com"),
            testLink.copy(id = "3", url = "https://deleted.com", deletedAt = System.currentTimeMillis()),
            testLink.copy(id = "4", url = "https://archived.com", isArchived = true)
        ))

        val urls = linkDao.getAllActiveUrls()

        assertEquals(2, urls.size)
        assertTrue(urls.contains("https://example1.com"))
        assertTrue(urls.contains("https://example2.com"))
        assertFalse(urls.contains("https://deleted.com"))
        assertFalse(urls.contains("https://archived.com"))
    }

    @Test
    fun getLinksWithNotes_returnsOnlyLinksWithNotes() = runTest {
        linkDao.insertAll(listOf(
            testLink.copy(note = "Has a note"),
            testLink.copy(id = "2", note = null),
            testLink.copy(id = "3", note = "")
        ))

        linkDao.getLinksWithNotes().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Has a note", result[0].note)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getLinksWithoutNotes_returnsOnlyLinksWithoutNotes() = runTest {
        linkDao.insertAll(listOf(
            testLink.copy(note = "Has a note"),
            testLink.copy(id = "2", note = null),
            testLink.copy(id = "3", note = "")
        ))

        linkDao.getLinksWithoutNotes().test {
            val result = awaitItem()
            assertEquals(2, result.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getLinksWithPreview_returnsOnlyLinksWithPreview() = runTest {
        linkDao.insertAll(listOf(
            testLink.copy(previewImagePath = "/path/to/image"),
            testLink.copy(id = "2", previewUrl = "https://preview.com/image.png"),
            testLink.copy(id = "3", previewImagePath = null, previewUrl = null)
        ))

        linkDao.getLinksWithPreview().test {
            val result = awaitItem()
            assertEquals(2, result.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
