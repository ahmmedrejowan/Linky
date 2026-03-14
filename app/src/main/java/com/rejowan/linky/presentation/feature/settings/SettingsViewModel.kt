package com.rejowan.linky.presentation.feature.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rejowan.linky.BuildConfig
import com.rejowan.linky.data.export.ImportConflictStrategy
import com.rejowan.linky.data.local.preferences.ThemePreferences
import com.rejowan.linky.domain.repository.CollectionRepository
import com.rejowan.linky.domain.repository.LinkRepository
import com.rejowan.linky.domain.repository.SnapshotRepository
import com.rejowan.linky.domain.usecase.backup.ExportDataUseCase
import com.rejowan.linky.domain.usecase.backup.ExportState
import com.rejowan.linky.domain.usecase.backup.ImportDataUseCase
import com.rejowan.linky.domain.usecase.backup.ImportState
import com.rejowan.linky.util.ErrorHandler
import com.rejowan.linky.util.FileStorageManager
import com.rejowan.linky.util.SettingsOperation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.roundToInt

class SettingsViewModel(
    private val linkRepository: LinkRepository,
    private val collectionRepository: CollectionRepository,
    private val snapshotRepository: SnapshotRepository,
    private val themePreferences: ThemePreferences,
    private val fileStorageManager: FileStorageManager,
    private val exportDataUseCase: ExportDataUseCase,
    private val importDataUseCase: ImportDataUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        loadSettings()
        observeTheme()
        observeTrashedLinksCount()
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.OnThemeChange -> {
                changeTheme(event.theme)
            }
            is SettingsEvent.OnClearCache -> {
                clearCache()
            }
            is SettingsEvent.OnRefresh -> {
                loadSettings()
            }
            is SettingsEvent.OnExportData -> {
                exportData(event.uri, event.includeSnapshots)
            }
            is SettingsEvent.OnImportData -> {
                importData(event.uri, event.conflictStrategy)
            }
            is SettingsEvent.OnPreviewImport -> {
                previewImport(event.uri)
            }
            is SettingsEvent.OnDismissExportResult -> {
                _state.update { it.copy(exportState = ExportUiState.Idle) }
            }
            is SettingsEvent.OnDismissImportResult -> {
                _state.update { it.copy(importState = ImportUiState.Idle) }
            }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val totalLinks = linkRepository.countLinks()
                val totalCollections = collectionRepository.countCollections()

                // Calculate total storage: snapshots from DB + actual file storage
                val snapshotStorageBytes = snapshotRepository.getTotalStorageUsed()
                val fileStorageBytes = fileStorageManager.getTotalStorageUsed()
                val totalStorageBytes = snapshotStorageBytes + fileStorageBytes
                val storageMB = (totalStorageBytes / (1024.0 * 1024.0)).roundToInt()

                _state.update {
                    it.copy(
                        totalLinks = totalLinks,
                        totalCollections = totalCollections,
                        totalStorageUsed = "$storageMB MB",
                        appVersion = BuildConfig.VERSION_NAME,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                val errorMessage = ErrorHandler.getSettingsErrorMessage(e, SettingsOperation.LOAD_STATISTICS)
                _state.update { it.copy(isLoading = false, error = errorMessage) }
            }
        }
    }

    private fun observeTheme() {
        viewModelScope.launch {
            themePreferences.getTheme()
                .catch { e ->
                    Timber.e(e, "Failed to observe theme")
                }
                .collect { theme ->
                    _state.update { it.copy(theme = theme) }
                }
        }
    }

    private fun observeTrashedLinksCount() {
        viewModelScope.launch {
            linkRepository.getTrashedLinks()
                .catch { e ->
                    Timber.e(e, "Failed to observe trashed links count")
                }
                .collect { trashedLinks ->
                    _state.update { it.copy(totalTrashedLinks = trashedLinks.size) }
                }
        }
    }

    private fun changeTheme(theme: String) {
        viewModelScope.launch {
            try {
                themePreferences.saveTheme(theme)
                Timber.d("Theme changed to: $theme")
            } catch (e: Exception) {
                val errorMessage = ErrorHandler.getSettingsErrorMessage(e, SettingsOperation.CHANGE_THEME)
                _state.update { it.copy(error = errorMessage) }
            }
        }
    }

    private fun clearCache() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                // Clear preview images cache
                val cacheCleared = fileStorageManager.clearPreviewCache()

                if (cacheCleared) {
                    Timber.d("Cache cleared successfully")
                    _state.update { it.copy(error = null) }
                } else {
                    Timber.w("Cache clearing completed with some failures")
                    _state.update { it.copy(error = "Some files could not be deleted") }
                }

                // Refresh to show updated storage
                loadSettings()
            } catch (e: Exception) {
                val errorMessage = ErrorHandler.getSettingsErrorMessage(e, SettingsOperation.CLEAR_CACHE)
                _state.update { it.copy(isLoading = false, error = errorMessage) }
            }
        }
    }

    private fun exportData(uri: Uri, includeSnapshots: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(exportState = ExportUiState.Exporting) }

            exportDataUseCase(uri, includeSnapshots)
                .catch { e ->
                    Timber.e(e, "Export failed")
                    _state.update { it.copy(exportState = ExportUiState.Error(e.message ?: "Export failed")) }
                }
                .collect { exportState ->
                    when (exportState) {
                        is ExportState.Preparing -> {
                            _state.update { it.copy(exportState = ExportUiState.Exporting) }
                        }
                        is ExportState.Progress -> {
                            // Could update progress UI here
                        }
                        is ExportState.Success -> {
                            Timber.d("Export successful: ${exportState.summary}")
                            _state.update { it.copy(exportState = ExportUiState.Success(exportState.summary)) }
                        }
                        is ExportState.Error -> {
                            _state.update { it.copy(exportState = ExportUiState.Error(exportState.message)) }
                        }
                    }
                }
        }
    }

    private fun previewImport(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(importState = ImportUiState.Validating) }

            try {
                val result = importDataUseCase.preview(uri)
                result.fold(
                    onSuccess = { preview ->
                        _state.update { it.copy(importState = ImportUiState.Preview(preview)) }
                    },
                    onFailure = { e ->
                        _state.update { it.copy(importState = ImportUiState.Error(e.message ?: "Invalid file")) }
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Import preview failed")
                _state.update { it.copy(importState = ImportUiState.Error(e.message ?: "Failed to read file")) }
            }
        }
    }

    private fun importData(uri: Uri, conflictStrategy: ImportConflictStrategy) {
        viewModelScope.launch {
            _state.update { it.copy(importState = ImportUiState.Importing) }

            importDataUseCase(uri, conflictStrategy)
                .catch { e ->
                    Timber.e(e, "Import failed")
                    _state.update { it.copy(importState = ImportUiState.Error(e.message ?: "Import failed")) }
                }
                .collect { importState ->
                    when (importState) {
                        is ImportState.Validating -> {
                            _state.update { it.copy(importState = ImportUiState.Validating) }
                        }
                        is ImportState.Preview -> {
                            // Already in importing state, skip preview
                        }
                        is ImportState.Importing -> {
                            _state.update { it.copy(importState = ImportUiState.Importing) }
                        }
                        is ImportState.Progress -> {
                            // Could update progress UI here
                        }
                        is ImportState.Success -> {
                            Timber.d("Import successful: ${importState.summary}")
                            _state.update { it.copy(importState = ImportUiState.Success(importState.summary)) }
                            // Refresh statistics
                            loadSettings()
                        }
                        is ImportState.Error -> {
                            _state.update { it.copy(importState = ImportUiState.Error(importState.message)) }
                        }
                    }
                }
        }
    }
}

sealed class SettingsEvent {
    data class OnThemeChange(val theme: String) : SettingsEvent()
    data object OnClearCache : SettingsEvent()
    data object OnRefresh : SettingsEvent()
    data class OnExportData(val uri: Uri, val includeSnapshots: Boolean = false) : SettingsEvent()
    data class OnPreviewImport(val uri: Uri) : SettingsEvent()
    data class OnImportData(val uri: Uri, val conflictStrategy: ImportConflictStrategy = ImportConflictStrategy.SKIP) : SettingsEvent()
    data object OnDismissExportResult : SettingsEvent()
    data object OnDismissImportResult : SettingsEvent()
}
