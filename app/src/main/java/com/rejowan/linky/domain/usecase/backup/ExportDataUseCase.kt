package com.rejowan.linky.domain.usecase.backup

import android.net.Uri
import com.rejowan.linky.data.export.ExportManager
import com.rejowan.linky.data.export.ExportSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Use case for exporting app data to JSON
 */
class ExportDataUseCase(
    private val exportManager: ExportManager
) {
    /**
     * Export data to the specified URI
     * @param uri The destination file URI
     * @param includeSnapshots Whether to include snapshots in the export (can be large)
     * @return Flow of export state updates
     */
    operator fun invoke(
        uri: Uri,
        includeSnapshots: Boolean = false
    ): Flow<ExportState> = flow {
        emit(ExportState.Preparing)

        var lastProgress = 0
        val result = exportManager.exportToUri(
            uri = uri,
            includeSnapshots = includeSnapshots,
            onProgress = { progress ->
                if (progress > lastProgress) {
                    lastProgress = progress
                }
            }
        )

        result.fold(
            onSuccess = { summary ->
                emit(ExportState.Success(uri, summary))
            },
            onFailure = { error ->
                emit(ExportState.Error(error.message ?: "Export failed"))
            }
        )
    }
}

/**
 * Represents the state of an export operation
 */
sealed class ExportState {
    data object Preparing : ExportState()
    data class Progress(val percent: Int) : ExportState()
    data class Success(val uri: Uri, val summary: ExportSummary) : ExportState()
    data class Error(val message: String) : ExportState()
}
