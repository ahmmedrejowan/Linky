package com.rejowan.linky.data.repository

import app.cash.turbine.test
import com.rejowan.linky.data.local.database.dao.SnapshotDao
import com.rejowan.linky.data.local.database.entity.SnapshotEntity
import com.rejowan.linky.domain.model.Snapshot
import com.rejowan.linky.domain.model.SnapshotType
import com.rejowan.linky.util.Result
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SnapshotRepositoryImplTest {

    private lateinit var snapshotDao: SnapshotDao
    private lateinit var repository: SnapshotRepositoryImpl
    private val testDispatcher = StandardTestDispatcher()

    private val testSnapshotEntity = SnapshotEntity(
        id = "snapshot-1",
        linkId = "link-1",
        type = "READER_MODE",
        filePath = "/path/to/file",
        fileSize = 1024L,
        createdAt = 1000L,
        title = "Test Title",
        author = "Test Author",
        excerpt = "Test excerpt",
        wordCount = 500,
        estimatedReadTime = 3
    )

    private val testSnapshot = Snapshot(
        id = "snapshot-1",
        linkId = "link-1",
        type = SnapshotType.READER_MODE,
        filePath = "/path/to/file",
        fileSize = 1024L,
        createdAt = 1000L,
        title = "Test Title",
        author = "Test Author",
        excerpt = "Test excerpt",
        wordCount = 500,
        estimatedReadTime = 3
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        snapshotDao = mockk(relaxed = true)
        repository = SnapshotRepositoryImpl(snapshotDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ============ Get Operations Tests ============

    @Test
    fun `getSnapshotsForLink returns mapped domain models`() = runTest {
        every { snapshotDao.getSnapshotsForLink("link-1") } returns flowOf(listOf(testSnapshotEntity))

        repository.getSnapshotsForLink("link-1").test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals(testSnapshot.id, result[0].id)
            assertEquals(testSnapshot.linkId, result[0].linkId)
            cancelAndIgnoreRemainingEvents()
        }

        verify { snapshotDao.getSnapshotsForLink("link-1") }
    }

    @Test
    fun `getSnapshotsForLink returns empty list when no snapshots`() = runTest {
        every { snapshotDao.getSnapshotsForLink("link-1") } returns flowOf(emptyList())

        repository.getSnapshotsForLink("link-1").test {
            val result = awaitItem()
            assertTrue(result.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getSnapshotById returns snapshot when exists`() = runTest {
        every { snapshotDao.getByIdFlow("snapshot-1") } returns flowOf(testSnapshotEntity)

        repository.getSnapshotById("snapshot-1").test {
            val result = awaitItem()
            assertEquals(testSnapshot.id, result?.id)
            assertEquals(testSnapshot.linkId, result?.linkId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getSnapshotById returns null when not exists`() = runTest {
        every { snapshotDao.getByIdFlow("non-existent") } returns flowOf(null)

        repository.getSnapshotById("non-existent").test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getSnapshotByIdOnce returns snapshot when exists`() = runTest {
        coEvery { snapshotDao.getById("snapshot-1") } returns testSnapshotEntity

        val result = repository.getSnapshotByIdOnce("snapshot-1")

        assertEquals(testSnapshot.id, result?.id)
        coVerify { snapshotDao.getById("snapshot-1") }
    }

    @Test
    fun `getSnapshotByIdOnce returns null when not exists`() = runTest {
        coEvery { snapshotDao.getById("non-existent") } returns null

        val result = repository.getSnapshotByIdOnce("non-existent")

        assertNull(result)
    }

    @Test
    fun `getSnapshotCount returns correct count`() = runTest {
        coEvery { snapshotDao.getSnapshotCount("link-1") } returns 5

        val result = repository.getSnapshotCount("link-1")

        assertEquals(5, result)
    }

    @Test
    fun `getTotalStorageUsed returns total bytes`() = runTest {
        coEvery { snapshotDao.getTotalStorageUsed() } returns 1024000L

        val result = repository.getTotalStorageUsed()

        assertEquals(1024000L, result)
    }

    @Test
    fun `getTotalStorageUsed returns 0 when null`() = runTest {
        coEvery { snapshotDao.getTotalStorageUsed() } returns null

        val result = repository.getTotalStorageUsed()

        assertEquals(0L, result)
    }

    // ============ Save Operations Tests ============

    @Test
    fun `saveSnapshot returns Success when insert succeeds`() = runTest {
        coEvery { snapshotDao.insert(any()) } just Runs

        val result = repository.saveSnapshot(testSnapshot)

        assertTrue(result is Result.Success)
        coVerify { snapshotDao.insert(any()) }
    }

    @Test
    fun `saveSnapshot returns Error when insert fails`() = runTest {
        coEvery { snapshotDao.insert(any()) } throws RuntimeException("Database error")

        val result = repository.saveSnapshot(testSnapshot)

        assertTrue(result is Result.Error)
    }

    // ============ Delete Operations Tests ============

    @Test
    fun `deleteSnapshot returns Success when snapshot exists`() = runTest {
        coEvery { snapshotDao.getById("snapshot-1") } returns testSnapshotEntity
        coEvery { snapshotDao.delete(any()) } just Runs

        val result = repository.deleteSnapshot("snapshot-1")

        assertTrue(result is Result.Success)
        coVerify { snapshotDao.delete(testSnapshotEntity) }
    }

    @Test
    fun `deleteSnapshot returns Error when snapshot not found`() = runTest {
        coEvery { snapshotDao.getById("non-existent") } returns null

        val result = repository.deleteSnapshot("non-existent")

        assertTrue(result is Result.Error)
        assertEquals("Snapshot not found", (result as Result.Error).exception.message)
    }

    @Test
    fun `deleteSnapshot returns Error when delete fails`() = runTest {
        coEvery { snapshotDao.getById("snapshot-1") } returns testSnapshotEntity
        coEvery { snapshotDao.delete(any()) } throws RuntimeException("Delete failed")

        val result = repository.deleteSnapshot("snapshot-1")

        assertTrue(result is Result.Error)
    }

    @Test
    fun `deleteSnapshotsForLink returns Success when delete succeeds`() = runTest {
        coEvery { snapshotDao.deleteForLink(any()) } just Runs

        val result = repository.deleteSnapshotsForLink("link-1")

        assertTrue(result is Result.Success)
        coVerify { snapshotDao.deleteForLink("link-1") }
    }

    @Test
    fun `deleteSnapshotsForLink returns Error when delete fails`() = runTest {
        coEvery { snapshotDao.deleteForLink(any()) } throws RuntimeException("Delete failed")

        val result = repository.deleteSnapshotsForLink("link-1")

        assertTrue(result is Result.Error)
    }

    @Test
    fun `deleteAllSnapshots returns Success when succeeds`() = runTest {
        coEvery { snapshotDao.deleteAll() } just Runs

        val result = repository.deleteAllSnapshots()

        assertTrue(result is Result.Success)
        coVerify { snapshotDao.deleteAll() }
    }

    @Test
    fun `deleteAllSnapshots returns Error when fails`() = runTest {
        coEvery { snapshotDao.deleteAll() } throws RuntimeException("Delete all failed")

        val result = repository.deleteAllSnapshots()

        assertTrue(result is Result.Error)
    }
}
