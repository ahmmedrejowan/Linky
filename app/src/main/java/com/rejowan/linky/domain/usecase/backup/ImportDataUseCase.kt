package com.rejowan.linky.domain.usecase.backup

import android.net.Uri
import com.rejowan.linky.data.export.ImportConflictStrategy
import com.rejowan.linky.data.export.ImportManager
import com.rejowan.linky.data.export.ImportPreview
import com.rejowan.linky.data.export.ImportSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Use case for importing app data from JSON
 */
class ImportDataUseCase(
    private val importManager: ImportManager
) {
    /**
     * Preview an import file before actually importing
     * @param uri The source file URI
     * @return Result containing the preview or error
     */
    suspend fun preview(uri: Uri): Result<ImportPreview> {
        return importManager.previewImport(uri)
    }

    /**
     * Import data from the specified URI
     * @param uri The source file URI
     * @param conflictStrategy How to handle duplicate items
     * @return Flow of import state updates
     */
    operator fun invoke(
        uri: Uri,
        conflictStrategy: ImportConflictStrategy = ImportConflictStrategy.SKIP
    ): Flow<ImportState> = flow {
        emit(ImportState.Validating)

        // First, validate the file
        val previewResult = importManager.previewImport(uri)
        if (previewResult.isFailure) {
            emit(ImportState.Error(previewResult.exceptionOrNull()?.message ?: "Invalid file"))
            return@flow
        }

        val preview = previewResult.getOrThrow()
        emit(ImportState.Preview(preview))

        // Proceed with import
        emit(ImportState.Importing)

        var lastProgress = 0
        val result = importManager.importFromUri(
            uri = uri,
            conflictStrategy = conflictStrategy,
            onProgress = { progress ->
                if (progress > lastProgress) {
                    lastProgress = progress
                }
            }
        )

        result.fold(
            onSuccess = { summary ->
                emit(ImportState.Success(summary))
            },
            onFailure = { error ->
                emit(ImportState.Error(error.message ?: "Import failed"))
            }
        )
    }
}

/**
 * Represents the state of an import operation
 */
sealed class ImportState {
    data object Validating : ImportState()
    data class Preview(val preview: ImportPreview) : ImportState()
    data object Importing : ImportState()
    data class Progress(val percent: Int) : ImportState()
    data class Success(val summary: ImportSummary) : ImportState()
    data class Error(val message: String) : ImportState()
}
