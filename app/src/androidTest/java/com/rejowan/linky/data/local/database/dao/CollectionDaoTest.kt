package com.rejowan.linky.data.local.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.rejowan.linky.data.local.database.AppDatabase
import com.rejowan.linky.data.local.database.entity.CollectionEntity
import com.rejowan.linky.data.local.database.entity.LinkEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CollectionDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var collectionDao: CollectionDao
    private lateinit var linkDao: LinkDao

    private val testCollection = CollectionEntity(
        id = "collection-1",
        name = "Work",
        color = "#FF5733",
        icon = "briefcase",
        sortOrder = 0,
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
        collectionDao = database.collectionDao()
        linkDao = database.linkDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ============ Insert Tests ============

    @Test
    fun insertCollection_savesToDatabase() = runTest {
        collectionDao.insert(testCollection)

        val result = collectionDao.getById(testCollection.id)
        assertNotNull(result)
        assertEquals(testCollection.id, result?.id)
        assertEquals(testCollection.name, result?.name)
        assertEquals(testCollection.color, result?.color)
    }

    @Test
    fun insertCollection_replacesOnConflict() = runTest {
        collectionDao.insert(testCollection)

        val updatedCollection = testCollection.copy(name = "Updated Work")
        collectionDao.insert(updatedCollection)

        val result = collectionDao.getById(testCollection.id)
        assertEquals("Updated Work", result?.name)
    }

    @Test
    fun insertAllCollections_savesMultiple() = runTest {
        val collections = listOf(
            testCollection,
            testCollection.copy(id = "collection-2", name = "Personal"),
            testCollection.copy(id = "collection-3", name = "Reading")
        )
        collectionDao.insertAll(collections)

        val count = collectionDao.countCollections()
        assertEquals(3, count)
    }

    // ============ Get Tests ============

    @Test
    fun getById_returnsNullForNonExistent() = runTest {
        val result = collectionDao.getById("non-existent")
        assertNull(result)
    }

    @Test
    fun getByIdFlow_emitsUpdates() = runTest {
        collectionDao.insert(testCollection)

        collectionDao.getByIdFlow(testCollection.id).test {
            val initial = awaitItem()
            assertEquals("Work", initial?.name)

            collectionDao.update(testCollection.copy(name = "Updated Work"))

            val updated = awaitItem()
            assertEquals("Updated Work", updated?.name)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getAllCollections_returnsSortedBySortOrder() = runTest {
        val collections = listOf(
            testCollection.copy(sortOrder = 2),
            testCollection.copy(id = "collection-2", name = "Personal", sortOrder = 0),
            testCollection.copy(id = "collection-3", name = "Reading", sortOrder = 1)
        )
        collectionDao.insertAll(collections)

        collectionDao.getAllCollections().test {
            val result = awaitItem()
            assertEquals(3, result.size)
            assertEquals("Personal", result[0].name)
            assertEquals("Reading", result[1].name)
            assertEquals("Work", result[2].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getAllCollectionsSync_returnsAllCollections() = runTest {
        collectionDao.insertAll(listOf(
            testCollection,
            testCollection.copy(id = "collection-2", name = "Personal")
        ))

        val result = collectionDao.getAllCollectionsSync()
        assertEquals(2, result.size)
    }

    // ============ Update Tests ============

    @Test
    fun updateCollection_modifiesExisting() = runTest {
        collectionDao.insert(testCollection)

        val updated = testCollection.copy(
            name = "Updated Name",
            color = "#00FF00"
        )
        collectionDao.update(updated)

        val result = collectionDao.getById(testCollection.id)
        assertEquals("Updated Name", result?.name)
        assertEquals("#00FF00", result?.color)
    }

    // ============ Delete Tests ============

    @Test
    fun deleteCollection_removesFromDatabase() = runTest {
        collectionDao.insert(testCollection)

        collectionDao.delete(testCollection)

        val result = collectionDao.getById(testCollection.id)
        assertNull(result)
    }

    @Test
    fun deleteAll_removesAllCollections() = runTest {
        collectionDao.insertAll(listOf(
            testCollection,
            testCollection.copy(id = "collection-2"),
            testCollection.copy(id = "collection-3")
        ))

        collectionDao.deleteAll()

        val count = collectionDao.countCollections()
        assertEquals(0, count)
    }

    // ============ Collection with Count Tests ============

    @Test
    fun getCollectionsWithCount_returnsCorrectLinkCount() = runTest {
        collectionDao.insert(testCollection)

        // Add links to the collection
        val link1 = LinkEntity(
            id = "link-1",
            title = "Link 1",
            url = "https://example1.com",
            collectionId = testCollection.id,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val link2 = LinkEntity(
            id = "link-2",
            title = "Link 2",
            url = "https://example2.com",
            collectionId = testCollection.id,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        linkDao.insertAll(listOf(link1, link2))

        collectionDao.getCollectionsWithCount().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals(2, result[0].linkCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getCollectionsWithCount_excludesDeletedLinks() = runTest {
        collectionDao.insert(testCollection)

        val activeLink = LinkEntity(
            id = "link-1",
            title = "Active Link",
            url = "https://example.com",
            collectionId = testCollection.id,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val deletedLink = LinkEntity(
            id = "link-2",
            title = "Deleted Link",
            url = "https://deleted.com",
            collectionId = testCollection.id,
            deletedAt = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        linkDao.insertAll(listOf(activeLink, deletedLink))

        collectionDao.getCollectionsWithCount().test {
            val result = awaitItem()
            assertEquals(1, result[0].linkCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getCollectionsWithCount_excludesArchivedLinks() = runTest {
        collectionDao.insert(testCollection)

        val activeLink = LinkEntity(
            id = "link-1",
            title = "Active Link",
            url = "https://example.com",
            collectionId = testCollection.id,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val archivedLink = LinkEntity(
            id = "link-2",
            title = "Archived Link",
            url = "https://archived.com",
            collectionId = testCollection.id,
            isArchived = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        linkDao.insertAll(listOf(activeLink, archivedLink))

        collectionDao.getCollectionsWithCount().test {
            val result = awaitItem()
            assertEquals(1, result[0].linkCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ============ Search Tests ============

    @Test
    fun searchCollections_findsByName() = runTest {
        collectionDao.insertAll(listOf(
            testCollection,
            testCollection.copy(id = "collection-2", name = "Personal"),
            testCollection.copy(id = "collection-3", name = "Work Projects")
        ))

        collectionDao.searchCollections("Work").test {
            val result = awaitItem()
            assertEquals(2, result.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun searchCollections_caseInsensitive() = runTest {
        collectionDao.insert(testCollection)

        collectionDao.searchCollections("work").test {
            val result = awaitItem()
            assertEquals(1, result.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ============ Count Tests ============

    @Test
    fun countCollections_returnsCorrectCount() = runTest {
        collectionDao.insertAll(listOf(
            testCollection,
            testCollection.copy(id = "collection-2"),
            testCollection.copy(id = "collection-3")
        ))

        val count = collectionDao.countCollections()
        assertEquals(3, count)
    }

    @Test
    fun countCollections_returnsZeroWhenEmpty() = runTest {
        val count = collectionDao.countCollections()
        assertEquals(0, count)
    }
}
