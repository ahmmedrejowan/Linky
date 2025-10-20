package com.rejowan.linky.domain.usecase.snapshot

import com.rejowan.linky.domain.model.Snapshot
import com.rejowan.linky.domain.model.SnapshotType
import com.rejowan.linky.util.FileStorageManager
import com.rejowan.linky.util.ReaderModeParser
import com.rejowan.linky.util.Result
import timber.log.Timber
import java.util.UUID

/**
 * Use case to capture a reader mode snapshot of a webpage
 * Orchestrates parsing, file storage, and database persistence
 */
class CaptureSnapshotUseCase(
    private val readerModeParser: ReaderModeParser,
    private val fileStorageManager: FileStorageManager,
    private val saveSnapshotUseCase: SaveSnapshotUseCase
) {
    /**
     * Captures a reader mode snapshot for the given URL
     * @param url The URL to capture
     * @param linkId The ID of the link this snapshot belongs to
     * @return Result with the created Snapshot or error
     */
    suspend operator fun invoke(url: String, linkId: String): Result<Snapshot> {
        try {
            Timber.d("CaptureSnapshotUseCase: Starting snapshot capture for URL: $url")

            // Step 1: Parse URL with Readability4J
            Timber.d("CaptureSnapshotUseCase: Parsing webpage content...")
            val parseResult = readerModeParser.parseUrl(url)

            when (parseResult) {
                is Result.Error -> {
                    Timber.e(parseResult.exception, "CaptureSnapshotUseCase: Failed to parse webpage")
                    return Result.Error(parseResult.exception)
                }
                is Result.Success -> {
                    val readerContent = parseResult.data
                    Timber.d("CaptureSnapshotUseCase: Parsing successful")
                    Timber.d("CaptureSnapshotUseCase: Title: ${readerContent.title}")
                    Timber.d("CaptureSnapshotUseCase: Word count: ${readerContent.wordCount}")

                    // Step 2: Save HTML content to file
                    Timber.d("CaptureSnapshotUseCase: Saving content to file...")
                    val contentBytes = readerContent.content.toByteArray(Charsets.UTF_8)
                    val timestamp = System.currentTimeMillis()

                    val filePath = fileStorageManager.saveSnapshot(
                        linkId = linkId,
                        content = contentBytes,
                        type = SnapshotType.READER_MODE,
                        timestamp = timestamp
                    )

                    if (filePath == null) {
                        Timber.e("CaptureSnapshotUseCase: Failed to save snapshot file")
                        return Result.Error(Exception("Failed to save snapshot content to file"))
                    }

                    Timber.d("CaptureSnapshotUseCase: File saved at: $filePath")
                    Timber.d("CaptureSnapshotUseCase: File size: ${contentBytes.size} bytes")

                    // Step 3: Create Snapshot entity with metadata
                    val snapshot = Snapshot(
                        id = UUID.randomUUID().toString(),
                        linkId = linkId,
                        type = SnapshotType.READER_MODE,
                        filePath = filePath,
                        fileSize = contentBytes.size.toLong(),
                        createdAt = timestamp,
                        title = readerContent.title,
                        author = readerContent.author,
                        excerpt = readerContent.excerpt,
                        wordCount = readerContent.wordCount,
                        estimatedReadTime = readerContent.estimatedReadTime
                    )

                    Timber.d("CaptureSnapshotUseCase: Snapshot entity created | ID: ${snapshot.id}")

                    // Step 4: Save to database
                    Timber.d("CaptureSnapshotUseCase: Saving snapshot to database...")
                    val saveResult = saveSnapshotUseCase(snapshot)

                    return when (saveResult) {
                        is Result.Success -> {
                            Timber.d("CaptureSnapshotUseCase: Snapshot saved successfully")
                            Result.Success(snapshot)
                        }
                        is Result.Error -> {
                            Timber.e(saveResult.exception, "CaptureSnapshotUseCase: Failed to save snapshot to database")
                            // Clean up file if database save fails
                            Timber.d("CaptureSnapshotUseCase: Cleaning up file due to database save failure")
                            fileStorageManager.deleteSnapshot(filePath)
                            Result.Error(saveResult.exception)
                        }
                        is Result.Loading -> Result.Loading
                    }
                }
                is Result.Loading -> return Result.Loading
            }
        } catch (e: Exception) {
            Timber.e(e, "CaptureSnapshotUseCase: Unexpected error during snapshot capture")
            return Result.Error(e)
        }
    }
}
