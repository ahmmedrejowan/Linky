package com.rejowan.linky.presentation.feature.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rejowan.linky.BuildConfig
import com.rejowan.linky.data.export.ImportConflictStrategy
import com.rejowan.linky.data.local.preferences.ThemePreferences
import com.rejowan.linky.data.update.GithubRelease
import com.rejowan.linky.data.update.UpdateCheckInterval
import com.rejowan.linky.data.update.UpdateState
import com.rejowan.linky.domain.repository.CollectionRepository
import com.rejowan.linky.domain.repository.LinkRepository
import com.rejowan.linky.domain.repository.SnapshotRepository
import com.rejowan.linky.domain.repository.UpdateRepository
import com.rejowan.linky.domain.usecase.backup.ExportDataUseCase
import com.rejowan.linky.domain.usecase.backup.ExportState
import com.rejowan.linky.domain.usecase.backup.ImportDataUseCase
import com.rejowan.linky.domain.usecase.backup.ImportState
import com.rejowan.linky.util.ApkDownloadManager
import com.rejowan.linky.util.ErrorHandler
import com.rejowan.linky.util.FileStorageManager
import com.rejowan.linky.util.SettingsOperation
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import kotlin.math.roundToInt

class SettingsViewModel(
    private val linkRepository: LinkRepository,
    private val collectionRepository: CollectionRepository,
    private val snapshotRepository: SnapshotRepository,
    private val themePreferences: ThemePreferences,
    private val fileStorageManager: FileStorageManager,
    private val exportDataUseCase: ExportDataUseCase,
    private val importDataUseCase: ImportDataUseCase,
    private val updateRepository: UpdateRepository,
    private val apkDownloadManager: ApkDownloadManager
) : ViewModel() {

    companion object {
        private const val TAG = "UpdateChecker"
        private const val GITHUB_OWNER = "ahmmedrejowan"
        private const val GITHUB_REPO = "Linky"
    }

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val _downloadState = MutableStateFlow<ApkDownloadManager.DownloadState>(ApkDownloadManager.DownloadState.Idle)
    val downloadState: StateFlow<ApkDownloadManager.DownloadState> = _downloadState.asStateFlow()

    private val _lastCheckTime = MutableStateFlow(0L)
    val lastCheckTime: StateFlow<Long> = _lastCheckTime.asStateFlow()

    private var downloadJob: Job? = null
    private var pendingInstallFile: File? = null

    private val _hasPendingApk = MutableStateFlow(false)
    val hasPendingApk: StateFlow<Boolean> = _hasPendingApk.asStateFlow()

    private val _pendingApkVersion = MutableStateFlow<String?>(null)
    val pendingApkVersion: StateFlow<String?> = _pendingApkVersion.asStateFlow()

    init {
        loadSettings()
        observeTheme()
        observeTrashedLinksCount()
        loadLastCheckTime()
        checkForUpdatesIfNeeded()
        checkPendingApk()
    }

    private fun checkPendingApk() {
        val currentVersion = BuildConfig.VERSION_NAME
        _hasPendingApk.value = apkDownloadManager.hasPendingApk(currentVersion)
        _pendingApkVersion.value = if (_hasPendingApk.value) {
            apkDownloadManager.getPendingApkVersion()
        } else {
            null
        }
        Timber.tag(TAG).d("Pending APK: ${_hasPendingApk.value}, version: ${_pendingApkVersion.value}, current: $currentVersion")
    }

    private fun loadLastCheckTime() {
        viewModelScope.launch {
            _lastCheckTime.value = updateRepository.getLastCheckTime()
        }
    }

    /**
     * Automatically checks for updates if the configured interval has passed.
     * Called on ViewModel initialization.
     */
    private fun checkForUpdatesIfNeeded() {
        viewModelScope.launch {
            val lastCheck = updateRepository.getLastCheckTime()
            val interval = _state.value.updateCheckInterval

            // Skip if auto-check is disabled
            if (interval == UpdateCheckInterval.NEVER) {
                Timber.d("Auto update check disabled")
                return@launch
            }

            val intervalMillis = interval.days * 24 * 60 * 60 * 1000L
            val timeSinceLastCheck = System.currentTimeMillis() - lastCheck

            if (timeSinceLastCheck >= intervalMillis) {
                Timber.d("Auto-checking for updates (last check: ${timeSinceLastCheck / 1000 / 60 / 60}h ago)")
                checkForUpdates()
            } else {
                val hoursUntilNextCheck = (intervalMillis - timeSinceLastCheck) / 1000 / 60 / 60
                Timber.d("Next auto-check in ${hoursUntilNextCheck}h")
            }
        }
    }

    /**
     * Check for app updates via GitHub releases
     */
    fun checkForUpdates() {
        viewModelScope.launch {
            _updateState.value = UpdateState.Checking
            Timber.d("Checking for updates...")

            val currentVersion = BuildConfig.VERSION_NAME
            val result = updateRepository.checkForUpdate(
                owner = GITHUB_OWNER,
                repo = GITHUB_REPO,
                currentVersion = currentVersion
            )

            result.fold(
                onSuccess = { release ->
                    if (release != null) {
                        // Check if this version should be skipped
                        if (updateRepository.shouldSkipVersion(release.version)) {
                            Timber.d("Version ${release.version} is skipped")
                            _updateState.value = UpdateState.UpToDate
                        } else {
                            Timber.d("Update available: ${release.version}")
                            _updateState.value = UpdateState.Available(
                                release = release,
                                currentVersion = currentVersion
                            )
                        }
                    } else {
                        Timber.d("App is up to date")
                        _updateState.value = UpdateState.UpToDate
                    }
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to check for updates")
                    _updateState.value = UpdateState.Error(
                        error.message ?: "Unknown error occurred"
                    )
                }
            )

            // Update last check time
            val now = System.currentTimeMillis()
            updateRepository.setLastCheckTime(now)
            _lastCheckTime.value = now
        }
    }

    fun skipVersion(version: String) {
        viewModelScope.launch {
            updateRepository.skipVersion(version)
            _updateState.value = UpdateState.Idle
        }
    }

    /**
     * Dismiss the update dialog
     */
    fun dismissUpdateDialog() {
        _updateState.value = UpdateState.Idle
    }

    fun getApkDownloadUrl(release: GithubRelease): String? {
        return release.assets.firstOrNull { it.isApk }?.downloadUrl
    }

    /**
     * Starts downloading the APK for the given release.
     */
    fun startDownload(release: GithubRelease) {
        val apkAsset = release.assets.firstOrNull { it.isApk }
        if (apkAsset == null) {
            Timber.tag(TAG).e("No APK asset found in release")
            _downloadState.value = ApkDownloadManager.DownloadState.Failed("No APK available")
            return
        }

        Timber.tag(TAG).d("Starting download: ${apkAsset.name}, version: ${release.version}")

        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            apkDownloadManager.downloadApk(apkAsset.downloadUrl, apkAsset.name, release.version)
                .collect { state ->
                    _downloadState.value = state
                    Timber.tag(TAG).d("Download state: $state")

                    if (state is ApkDownloadManager.DownloadState.Completed) {
                        pendingInstallFile = state.file
                    }
                }
        }
    }

    /**
     * Cancels the ongoing download.
     */
    fun cancelDownload() {
        Timber.tag(TAG).d("Cancelling download")
        downloadJob?.cancel()
        downloadJob = null
        _downloadState.value = ApkDownloadManager.DownloadState.Cancelled
    }

    /**
     * Resets the download state to idle.
     */
    fun resetDownloadState() {
        _downloadState.value = ApkDownloadManager.DownloadState.Idle
        pendingInstallFile = null
    }

    /**
     * Checks if the app has permission to install APKs.
     */
    fun canInstallApks(): Boolean {
        return apkDownloadManager.canInstallApks()
    }

    /**
     * Installs the downloaded APK.
     */
    fun installDownloadedApk(): Boolean {
        val file = pendingInstallFile ?: run {
            Timber.tag(TAG).e("No pending install file")
            return false
        }
        return apkDownloadManager.installApk(file)
    }

    /**
     * Installs APK from a specific file.
     */
    fun installApk(file: File): Boolean {
        return apkDownloadManager.installApk(file)
    }

    /**
     * Installs the pending APK if one exists.
     */
    fun installPendingApk(): Boolean {
        val file = apkDownloadManager.getPendingApk() ?: run {
            Timber.tag(TAG).e("No pending APK found")
            return false
        }
        return apkDownloadManager.installApk(file)
    }

    /**
     * Clears the pending APK (deletes downloaded file).
     */
    fun clearPendingApk() {
        apkDownloadManager.cleanupOldDownloads()
        checkPendingApk()
    }

    /**
     * Refreshes pending APK state (call on resume).
     */
    fun refreshPendingApkState() {
        checkPendingApk()
    }

    /**
     * Opens the system settings to enable install from unknown sources.
     */
    fun openInstallPermissionSettings(): android.content.Intent? {
        return apkDownloadManager.getInstallPermissionIntent()
    }

    /**
     * Set update check interval
     */
    fun setUpdateCheckInterval(interval: UpdateCheckInterval) {
        _state.update { it.copy(updateCheckInterval = interval) }
        // TODO: Persist to preferences
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
