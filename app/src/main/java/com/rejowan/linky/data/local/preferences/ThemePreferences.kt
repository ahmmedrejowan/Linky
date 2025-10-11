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



}