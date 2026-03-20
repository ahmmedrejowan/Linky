package com.rejowan.linky.data.local.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.rejowan.linky.data.local.database.AppDatabase
import com.rejowan.linky.data.local.database.entity.LinkEntity
import com.rejowan.linky.data.local.database.entity.SnapshotEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SnapshotDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var snapshotDao: SnapshotDao
    private lateinit var linkDao: LinkDao

    private val testLink = LinkEntity(
        id = "link-1",
        title = "Test Link",
        url = "https://example.com",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )

    private val testSnapshot = SnapshotEntity(
        id = "snapshot-1",
        linkId = "link-1",
        type = "READER_MODE",
        filePath = "/path/to/snapshot.html",
        fileSize = 1024L,
        createdAt = System.currentTimeMillis(),
        title = "Snapshot Title",
        wordCount = 500,
        estimatedReadTime = 3
    )

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        snapshotDao = database.snapshotDao()
        linkDao = database.linkDao()

        // Insert the parent link first (required for foreign key)
        runTest {
            linkDao.insert(testLink)
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ============ Insert Tests ============

    @Test
    fun insertSnapshot_savesToDatabase() = runTest {
        snapshotDao.insert(testSnapshot)

        val result = snapshotDao.getById(testSnapshot.id)
        assertNotNull(result)
        assertEquals(testSnapshot.id, result?.id)
        assertEquals(testSnapshot.linkId, result?.linkId)
        assertEquals(testSnapshot.type, result?.type)
        assertEquals(testSnapshot.filePath, result?.filePath)
    }

    @Test
    fun insertSnapshot_replacesOnConflict() = runTest {
        snapshotDao.insert(testSnapshot)

        val updatedSnapshot = testSnapshot.copy(filePath = "/new/path/snapshot.html")
        snapshotDao.insert(updatedSnapshot)

        val result = snapshotDao.getById(testSnapshot.id)
        assertEquals("/new/path/snapshot.html", result?.filePath)
    }

    @Test
    fun insertAllSnapshots_savesMultiple() = runTest {
        val snapshots = listOf(
            testSnapshot,
            testSnapshot.copy(id = "snapshot-2"),
            testSnapshot.copy(id = "snapshot-3")
        )
        snapshotDao.insertAll(snapshots)

        val count = snapshotDao.getSnapshotCount(testLink.id)
        assertEquals(3, count)
    }

    // ============ Get Tests ============

    @Test
    fun getById_returnsNullForNonExistent() = runTest {
        val result = snapshotDao.getById("non-existent")
        assertNull(result)
    }

    @Test
    fun getByIdFlow_emitsUpdates() = runTest {
        snapshotDao.insert(testSnapshot)

        snapshotDao.getByIdFlow(testSnapshot.id).test {
            val initial = awaitItem()
            assertEquals("Snapshot Title", initial?.title)

            snapshotDao.insert(testSnapshot.copy(title = "Updated Title"))

            val updated = awaitItem()
            assertEquals("Updated Title", updated?.title)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getSnapshotsForLink_returnsSnapshotsOrderedByDate() = runTest {
        val now = System.currentTimeMillis()
        val snapshots = listOf(
            testSnapshot.copy(id = "snapshot-1", createdAt = now - 2000),
            testSnapshot.copy(id = "snapshot-2", createdAt = now - 1000),
            testSnapshot.copy(id = "snapshot-3", createdAt = now)
        )
        snapshotDao.insertAll(snapshots)

        snapshotDao.getSnapshotsForLink(testLink.id).test {
            val result = awaitItem()
            assertEquals(3, result.size)
            // Should be ordered by createdAt DESC (newest first)
            assertEquals("snapshot-3", result[0].id)
            assertEquals("snapshot-2", result[1].id)
            assertEquals("snapshot-1", result[2].id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getSnapshotsForLink_returnsOnlySnapshotsForSpecificLink() = runTest {
        // Insert another link
        val link2 = testLink.copy(id = "link-2", url = "https://other.com")
        linkDao.insert(link2)

        snapshotDao.insert(testSnapshot)
        snapshotDao.insert(testSnapshot.copy(id = "snapshot-2", linkId = "link-2"))

        snapshotDao.getSnapshotsForLink("link-1").test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("link-1", result[0].linkId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getAllSnapshotsSync_returnsAllSnapshots() = runTest {
        snapshotDao.insertAll(listOf(
            testSnapshot,
            testSnapshot.copy(id = "snapshot-2")
        ))

        val result = snapshotDao.getAllSnapshotsSync()
        assertEquals(2, result.size)
    }

    // ============ Delete Tests ============

    @Test
    fun deleteSnapshot_removesFromDatabase() = runTest {
        snapshotDao.insert(testSnapshot)

        snapshotDao.delete(testSnapshot)

        val result = snapshotDao.getById(testSnapshot.id)
        assertNull(result)
    }

    @Test
    fun deleteForLink_removesAllSnapshotsForLink() = runTest {
        snapshotDao.insertAll(listOf(
            testSnapshot,
            testSnapshot.copy(id = "snapshot-2"),
            testSnapshot.copy(id = "snapshot-3")
        ))

        snapshotDao.deleteForLink(testLink.id)

        val count = snapshotDao.getSnapshotCount(testLink.id)
        assertEquals(0, count)
    }

    @Test
    fun deleteAll_removesAllSnapshots() = runTest {
        snapshotDao.insertAll(listOf(
            testSnapshot,
            testSnapshot.copy(id = "snapshot-2")
        ))

        snapshotDao.deleteAll()

        val result = snapshotDao.getAllSnapshotsSync()
        assertEquals(0, result.size)
    }

    @Test
    fun cascadeDelete_removesSnapshotsWhenLinkDeleted() = runTest {
        snapshotDao.insert(testSnapshot)

        linkDao.delete(testLink)

        val result = snapshotDao.getById(testSnapshot.id)
        assertNull(result)
    }

    // ============ Storage Tests ============

    @Test
    fun getTotalStorageUsed_returnsSumOfFileSizes() = runTest {
        snapshotDao.insertAll(listOf(
            testSnapshot.copy(id = "snapshot-1", fileSize = 1000L),
            testSnapshot.copy(id = "snapshot-2", fileSize = 2000L),
            testSnapshot.copy(id = "snapshot-3", fileSize = 3000L)
        ))

        val totalSize = snapshotDao.getTotalStorageUsed()
        assertEquals(6000L, totalSize)
    }

    @Test
    fun getTotalStorageUsed_returnsNullWhenEmpty() = runTest {
        val totalSize = snapshotDao.getTotalStorageUsed()
        assertNull(totalSize)
    }

    // ============ Count Tests ============

    @Test
    fun getSnapshotCount_returnsCorrectCount() = runTest {
        snapshotDao.insertAll(listOf(
            testSnapshot,
            testSnapshot.copy(id = "snapshot-2"),
            testSnapshot.copy(id = "snapshot-3")
        ))

        val count = snapshotDao.getSnapshotCount(testLink.id)
        assertEquals(3, count)
    }

    @Test
    fun getSnapshotCount_returnsZeroWhenNoSnapshots() = runTest {
        val count = snapshotDao.getSnapshotCount(testLink.id)
        assertEquals(0, count)
    }
}
