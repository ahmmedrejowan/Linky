package com.rejowan.linky.domain.usecase.snapshot

import com.rejowan.linky.domain.model.Snapshot
import com.rejowan.linky.domain.model.SnapshotType
import com.rejowan.linky.domain.repository.SnapshotRepository
import com.rejowan.linky.util.FileStorageManager
import com.rejowan.linky.util.ReaderContent
import com.rejowan.linky.util.ReaderModeParser
import com.rejowan.linky.util.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SnapshotUseCasesTest {

    private lateinit var snapshotRepository: SnapshotRepository
    private lateinit var fileStorageManager: FileStorageManager
    private lateinit var readerModeParser: ReaderModeParser

    private lateinit var getSnapshotsForLinkUseCase: GetSnapshotsForLinkUseCase
    private lateinit var getSnapshotByIdUseCase: GetSnapshotByIdUseCase
    private lateinit var saveSnapshotUseCase: SaveSnapshotUseCase
    private lateinit var deleteSnapshotUseCase: DeleteSnapshotUseCase
    private lateinit var captureSnapshotUseCase: CaptureSnapshotUseCase

    private val testSnapshot = Snapshot(
        id = "snapshot-1",
        linkId = "link-1",
        type = SnapshotType.READER_MODE,
        filePath = "/path/to/snapshot.html",
        fileSize = 1024L,
        createdAt = 1000L,
        title = "Test Article",
        author = "Test Author",
        excerpt = "This is a test excerpt",
        wordCount = 500,
        estimatedReadTime = 3
    )

    private val testSnapshot2 = Snapshot(
        id = "snapshot-2",
        linkId = "link-1",
        type = SnapshotType.PDF,
        filePath = "/path/to/snapshot.pdf",
        fileSize = 2048L,
        createdAt = 2000L
    )

    private val testReaderContent = ReaderContent(
        title = "Test Article",
        author = "Test Author",
        content = "<html>Clean content</html>",
        excerpt = "This is a test excerpt",
        wordCount = 500,
        estimatedReadTime = 3,
        siteName = "Example Site"
    )

    @Before
    fun setUp() {
        snapshotRepository = mockk()
        fileStorageManager = mockk()
        readerModeParser = mockk()

        getSnapshotsForLinkUseCase = GetSnapshotsForLinkUseCase(snapshotRepository)
        getSnapshotByIdUseCase = GetSnapshotByIdUseCase(snapshotRepository)
        saveSnapshotUseCase = SaveSnapshotUseCase(snapshotRepository)
        deleteSnapshotUseCase = DeleteSnapshotUseCase(snapshotRepository, fileStorageManager)
        captureSnapshotUseCase = CaptureSnapshotUseCase(readerModeParser, fileStorageManager, saveSnapshotUseCase)
    }

    // GetSnapshotsForLinkUseCase Tests
    @Test
    fun `getSnapshotsForLink returns snapshots for link`() = runTest {
        val snapshots = listOf(testSnapshot, testSnapshot2)
        every { snapshotRepository.getSnapshotsForLink("link-1") } returns flowOf(snapshots)

        val result = getSnapshotsForLinkUseCase("link-1").first()

        assertEquals(2, result.size)
        assertEquals(testSnapshot, result[0])
        assertEquals(testSnapshot2, result[1])
    }

    @Test
    fun `getSnapshotsForLink returns empty list when no snapshots`() = runTest {
        every { snapshotRepository.getSnapshotsForLink("link-1") } returns flowOf(emptyList())

        val result = getSnapshotsForLinkUseCase("link-1").first()

        assertTrue(result.isEmpty())
    }

    // GetSnapshotByIdUseCase Tests
    @Test
    fun `getSnapshotById returns snapshot when found`() = runTest {
        every { snapshotRepository.getSnapshotById("snapshot-1") } returns flowOf(testSnapshot)

        val result = getSnapshotByIdUseCase("snapshot-1").first()

        assertNotNull(result)
        assertEquals(testSnapshot, result)
    }

    @Test
    fun `getSnapshotById returns null when not found`() = runTest {
        every { snapshotRepository.getSnapshotById("non-existent") } returns flowOf(null)

        val result = getSnapshotByIdUseCase("non-existent").first()

        assertNull(result)
    }

    @Test
    fun `getSnapshotByIdOnce returns snapshot once`() = runTest {
        coEvery { snapshotRepository.getSnapshotByIdOnce("snapshot-1") } returns testSnapshot

        val result = getSnapshotByIdUseCase.getOnce("snapshot-1")

        assertNotNull(result)
        assertEquals(testSnapshot, result)
    }

    @Test
    fun `getSnapshotByIdOnce returns null when not found`() = runTest {
        coEvery { snapshotRepository.getSnapshotByIdOnce("non-existent") } returns null

        val result = getSnapshotByIdUseCase.getOnce("non-existent")

        assertNull(result)
    }

    // SaveSnapshotUseCase Tests
    @Test
    fun `saveSnapshot succeeds`() = runTest {
        coEvery { snapshotRepository.saveSnapshot(testSnapshot) } returns Result.Success(Unit)

        val result = saveSnapshotUseCase(testSnapshot)

        assertTrue(result is Result.Success)
        coVerify { snapshotRepository.saveSnapshot(testSnapshot) }
    }

    @Test
    fun `saveSnapshot returns error when repository fails`() = runTest {
        coEvery { snapshotRepository.saveSnapshot(testSnapshot) } returns Result.Error(Exception("Save failed"))

        val result = saveSnapshotUseCase(testSnapshot)

        assertTrue(result is Result.Error)
    }

    // DeleteSnapshotUseCase Tests
    @Test
    fun `deleteSnapshot succeeds`() = runTest {
        coEvery { snapshotRepository.getSnapshotByIdOnce("snapshot-1") } returns testSnapshot
        coEvery { fileStorageManager.deleteSnapshot(testSnapshot.filePath) } returns true
        coEvery { snapshotRepository.deleteSnapshot("snapshot-1") } returns Result.Success(Unit)

        val result = deleteSnapshotUseCase("snapshot-1")

        assertTrue(result is Result.Success)
        coVerify { fileStorageManager.deleteSnapshot(testSnapshot.filePath) }
        coVerify { snapshotRepository.deleteSnapshot("snapshot-1") }
    }

    @Test
    fun `deleteSnapshot returns error when snapshot not found`() = runTest {
        coEvery { snapshotRepository.getSnapshotByIdOnce("non-existent") } returns null

        val result = deleteSnapshotUseCase("non-existent")

        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.exception.message!!.contains("not found"))
    }

    @Test
    fun `deleteSnapshot continues even if file deletion fails`() = runTest {
        coEvery { snapshotRepository.getSnapshotByIdOnce("snapshot-1") } returns testSnapshot
        coEvery { fileStorageManager.deleteSnapshot(testSnapshot.filePath) } returns false
        coEvery { snapshotRepository.deleteSnapshot("snapshot-1") } returns Result.Success(Unit)

        val result = deleteSnapshotUseCase("snapshot-1")

        assertTrue(result is Result.Success)
        coVerify { snapshotRepository.deleteSnapshot("snapshot-1") }
    }

    @Test
    fun `deleteSnapshot returns error when database deletion fails`() = runTest {
        coEvery { snapshotRepository.getSnapshotByIdOnce("snapshot-1") } returns testSnapshot
        coEvery { fileStorageManager.deleteSnapshot(testSnapshot.filePath) } returns true
        coEvery { snapshotRepository.deleteSnapshot("snapshot-1") } returns Result.Error(Exception("Delete failed"))

        val result = deleteSnapshotUseCase("snapshot-1")

        assertTrue(result is Result.Error)
    }

    @Test
    fun `deleteSnapshot handles exception`() = runTest {
        coEvery { snapshotRepository.getSnapshotByIdOnce("snapshot-1") } throws RuntimeException("Unexpected error")

        val result = deleteSnapshotUseCase("snapshot-1")

        assertTrue(result is Result.Error)
    }

    // CaptureSnapshotUseCase Tests
    @Test
    fun `captureSnapshot succeeds`() = runTest {
        val url = "https://example.com/article"
        val linkId = "link-1"
        val filePath = "/path/to/saved/snapshot.html"

        coEvery { readerModeParser.parseUrl(url) } returns Result.Success(testReaderContent)
        coEvery { fileStorageManager.saveSnapshot(any(), any(), any(), any()) } returns filePath
        coEvery { snapshotRepository.saveSnapshot(any()) } returns Result.Success(Unit)

        val result = captureSnapshotUseCase(url, linkId)

        assertTrue(result is Result.Success)
        val snapshot = (result as Result.Success).data
        assertEquals(linkId, snapshot.linkId)
        assertEquals(SnapshotType.READER_MODE, snapshot.type)
        assertEquals(filePath, snapshot.filePath)
        assertEquals(testReaderContent.title, snapshot.title)
        assertEquals(testReaderContent.author, snapshot.author)
        assertEquals(testReaderContent.wordCount, snapshot.wordCount)
    }

    @Test
    fun `captureSnapshot fails when parsing fails`() = runTest {
        val url = "https://example.com/article"
        val linkId = "link-1"

        coEvery { readerModeParser.parseUrl(url) } returns Result.Error(Exception("Parse failed"))

        val result = captureSnapshotUseCase(url, linkId)

        assertTrue(result is Result.Error)
    }

    @Test
    fun `captureSnapshot fails when file save fails`() = runTest {
        val url = "https://example.com/article"
        val linkId = "link-1"

        coEvery { readerModeParser.parseUrl(url) } returns Result.Success(testReaderContent)
        coEvery { fileStorageManager.saveSnapshot(any(), any(), any(), any()) } returns null

        val result = captureSnapshotUseCase(url, linkId)

        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.exception.message!!.contains("Failed to save snapshot"))
    }

    @Test
    fun `captureSnapshot cleans up file when database save fails`() = runTest {
        val url = "https://example.com/article"
        val linkId = "link-1"
        val filePath = "/path/to/saved/snapshot.html"

        coEvery { readerModeParser.parseUrl(url) } returns Result.Success(testReaderContent)
        coEvery { fileStorageManager.saveSnapshot(any(), any(), any(), any()) } returns filePath
        coEvery { snapshotRepository.saveSnapshot(any()) } returns Result.Error(Exception("DB save failed"))
        coEvery { fileStorageManager.deleteSnapshot(filePath) } returns true

        val result = captureSnapshotUseCase(url, linkId)

        assertTrue(result is Result.Error)
        coVerify { fileStorageManager.deleteSnapshot(filePath) }
    }

    @Test
    fun `captureSnapshot handles exception`() = runTest {
        val url = "https://example.com/article"
        val linkId = "link-1"

        coEvery { readerModeParser.parseUrl(url) } throws RuntimeException("Unexpected error")

        val result = captureSnapshotUseCase(url, linkId)

        assertTrue(result is Result.Error)
    }
}
