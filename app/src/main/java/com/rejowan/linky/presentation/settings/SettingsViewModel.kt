package com.rejowan.linky.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rejowan.linky.data.local.preferences.ThemePreferences
import com.rejowan.linky.domain.repository.FolderRepository
import com.rejowan.linky.domain.repository.LinkRepository
import com.rejowan.linky.domain.repository.SnapshotRepository
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
    private val folderRepository: FolderRepository,
    private val snapshotRepository: SnapshotRepository,
    private val themePreferences: ThemePreferences
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        loadSettings()
        observeTheme()
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
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val totalLinks = linkRepository.countLinks()
                val totalFolders = folderRepository.countFolders()
                val storageBytes = snapshotRepository.getTotalStorageUsed()
                val storageMB = (storageBytes / (1024.0 * 1024.0)).roundToInt()

                _state.update {
                    it.copy(
                        totalLinks = totalLinks,
                        totalFolders = totalFolders,
                        totalStorageUsed = "$storageMB MB",
                        appVersion = "1.0.0", // TODO: Get from BuildConfig
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load settings")
                _state.update { it.copy(isLoading = false, error = e.message) }
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

    private fun changeTheme(theme: String) {
        viewModelScope.launch {
            themePreferences.setTheme(theme)
        }
    }

    private fun clearCache() {
        viewModelScope.launch {
            try {
                // TODO: Implement cache clearing
                // This will clear preview images and temporary files
                Timber.d("Cache cleared")
                loadSettings() // Refresh to show updated storage
            } catch (e: Exception) {
                Timber.e(e, "Failed to clear cache")
                _state.update { it.copy(error = e.message) }
            }
        }
    }
}

sealed class SettingsEvent {
    data class OnThemeChange(val theme: String) : SettingsEvent()
    data object OnClearCache : SettingsEvent()
    data object OnRefresh : SettingsEvent()
}
