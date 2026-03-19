package com.rejowan.linky.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import kotlin.text.toBoolean
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_mode_preferences")

class ThemePreferences(private val context: Context) {

    private val themeModeKey = stringPreferencesKey("theme_preference")
    private val dynamicColorKey = stringPreferencesKey("dynamic_color_preference")
    private val showBatchImportHowItWorksKey = stringPreferencesKey("show_batch_import_how_it_works")
    private val clipboardCheckingEnabledKey = stringPreferencesKey("clipboard_checking_enabled")
    private val updateCheckIntervalKey = stringPreferencesKey("update_check_interval")
    private val linkViewModeKey = stringPreferencesKey("link_view_mode")
    private val collectionViewModeKey = stringPreferencesKey("collection_view_mode")

    suspend fun saveTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[themeModeKey] = theme
        }
    }

    fun getTheme(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[themeModeKey] ?: "System Default"
        }
    }

    suspend fun setDefaultThemeIfNotSet() {
        context.dataStore.edit { preferences ->
            Timber.tag("ThemePrefHelper").e("setDefaultThemeIfNotSet: Current value: ${preferences[themeModeKey]}")
            if (preferences[themeModeKey] == null) {
                preferences[themeModeKey] = "System Default"
            }
        }
    }


    suspend fun saveDynamicColorPreference(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[dynamicColorKey] = if (enabled) "true" else "false"
        }
    }

    fun isDynamicColorEnabled(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[dynamicColorKey]?.toBoolean() ?: false
        }
    }

    suspend fun setShowBatchImportHowItWorks(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[showBatchImportHowItWorksKey] = if (show) "true" else "false"
        }
    }

    fun shouldShowBatchImportHowItWorks(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[showBatchImportHowItWorksKey]?.toBoolean() ?: true // Default: show it
        }
    }

    suspend fun setClipboardCheckingEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[clipboardCheckingEnabledKey] = if (enabled) "true" else "false"
        }
    }

    fun isClipboardCheckingEnabled(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[clipboardCheckingEnabledKey]?.toBoolean() ?: true // Default: enabled
        }
    }

    suspend fun setUpdateCheckInterval(interval: String) {
        context.dataStore.edit { preferences ->
            preferences[updateCheckIntervalKey] = interval
        }
    }

    fun getUpdateCheckInterval(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[updateCheckIntervalKey] ?: "WEEKLY" // Default: weekly
        }
    }

    // View Mode preferences (separate for links and collections)

    suspend fun setLinkViewMode(viewMode: String) {
        context.dataStore.edit { preferences ->
            preferences[linkViewModeKey] = viewMode
        }
    }

    fun getLinkViewMode(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[linkViewModeKey] ?: "LIST" // Default: list
        }
    }

    suspend fun setCollectionViewMode(viewMode: String) {
        context.dataStore.edit { preferences ->
            preferences[collectionViewModeKey] = viewMode
        }
    }

    fun getCollectionViewMode(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[collectionViewModeKey] ?: "LIST" // Default: list
        }
    }

}