package com.rejowan.linky.presentation.feature.settings

import com.rejowan.linky.data.export.ExportSummary
import com.rejowan.linky.data.export.ImportPreview
import com.rejowan.linky.data.export.ImportSummary
import com.rejowan.linky.data.update.UpdateCheckInterval

data class SettingsState(
    val totalLinks: Int = 0,
    val totalCollections: Int = 0,
    val totalTrashedLinks: Int = 0,
    val totalStorageUsed: String = "0 MB",
    val appVersion: String = "",
    val theme: String = "System",
    val isLoading: Boolean = false,
    val error: String? = null,

    // Export/Import states
    val exportState: ExportUiState = ExportUiState.Idle,
    val importState: ImportUiState = ImportUiState.Idle,

    // Update check
    val updateCheckInterval: UpdateCheckInterval = UpdateCheckInterval.WEEKLY
)

sealed class ExportUiState {
    data object Idle : ExportUiState()
    data object Exporting : ExportUiState()
    data class Success(val summary: ExportSummary) : ExportUiState()
    data class Error(val message: String) : ExportUiState()
}

sealed class ImportUiState {
    data object Idle : ImportUiState()
    data object Validating : ImportUiState()
    data class Preview(val preview: ImportPreview) : ImportUiState()
    data object Importing : ImportUiState()
    data class Success(val summary: ImportSummary) : ImportUiState()
    data class Error(val message: String) : ImportUiState()
}
