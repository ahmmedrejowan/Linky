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

    /**
     * Check if archived links should be shown in collection details
     * Default is true (show archived links by default)
     */
    fun shouldShowArchivedLinks(): Boolean {
        return prefs.getBoolean(KEY_SHOW_ARCHIVED_LINKS, true)
    }

    /**
     * Set whether to show archived links in collection details
     */
    fun setShowArchivedLinks(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_ARCHIVED_LINKS, show).apply()
    }

    /**
     * Check if onboarding has been completed
     */
    fun hasCompletedOnboarding(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    /**
     * Mark onboarding as completed
     */
    fun setOnboardingCompleted() {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply()
    }

    companion object {
        private const val PREFS_NAME = "linky_preferences"
        private const val KEY_SHOW_PREVIEW_FETCH_SUGGESTION = "show_preview_fetch_suggestion"
        private const val KEY_SHOW_ARCHIVED_LINKS = "show_archived_links"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }
}
