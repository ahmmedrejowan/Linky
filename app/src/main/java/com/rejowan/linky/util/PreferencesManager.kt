package com.rejowan.linky.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages user preferences using SharedPreferences
 */
class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    /**
     * Check if the preview fetch suggestion card should be shown
     */
    fun shouldShowPreviewFetchSuggestion(): Boolean {
        return prefs.getBoolean(KEY_SHOW_PREVIEW_FETCH_SUGGESTION, true)
    }

    /**
     * Mark the preview fetch suggestion card as dismissed
     */
    fun dismissPreviewFetchSuggestion() {
        prefs.edit().putBoolean(KEY_SHOW_PREVIEW_FETCH_SUGGESTION, false).apply()
    }

    companion object {
        private const val PREFS_NAME = "linky_preferences"
        private const val KEY_SHOW_PREVIEW_FETCH_SUGGESTION = "show_preview_fetch_suggestion"
    }
}
