package com.rejowan.linky.presentation.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes using Kotlin Serialization
 * All routes are defined as @Serializable objects or data classes
 */
@Serializable
sealed class Route {

    // ============ MAIN APP ROUTES (Phase 1) ============

    /**
     * Main screen - Container for bottom navigation
     * This is the entry point and contains the BottomNavHost with Home/Collections/Settings
     * @param initialTab The initial tab to navigate to (0=Home, 1=Collections, 2=Settings), null=remember last
     * @param navigateToCollectionId If set, navigates to this collection detail screen after opening Collections tab
     */
    @Serializable
    data class Main(
        val initialTab: Int? = null,
        val navigateToCollectionId: String? = null
    ) : Route()

    /**
     * Home screen - Main entry point with all links
     * Shows search, filters, and link list
     * NOTE: This is now nested within Main screen's BottomNavHost
     */
    @Serializable
    data object Home : Route()

    /**
     * Link detail screen
     * @param linkId The ID of the link to display
     */
    @Serializable
    data class LinkDetail(val linkId: String) : Route()

    /**
     * Add or Edit link screen
     * @param linkId Optional link ID for edit mode. Null for add mode
     * @param collectionId Optional collection ID to preselect collection when adding
     * @param url Optional URL to prefill when adding from clipboard/share
     */
    @Serializable
    data class AddEditLink(
        val linkId: String? = null,
        val collectionId: String? = null,
        val url: String? = null
    ) : Route()

    /**
     * Snapshot viewer screen
     * @param snapshotId The ID of the snapshot to display
     */
    @Serializable
    data class SnapshotViewer(val snapshotId: String) : Route()

    /**
     * Collections screen - Shows all collections
     */
    @Serializable
    data object Collections : Route()

    /**
     * Collection detail screen
     * Shows all links within a specific collection
     * @param collectionId The ID of the collection to display
     */
    @Serializable
    data class CollectionDetail(val collectionId: String) : Route()

    /**
     * Search screen - Dedicated search functionality
     * Searches across all links regardless of filter/collection
     */
    @Serializable
    data object Search : Route()

    /**
     * Settings screen - App settings and preferences
     */
    @Serializable
    data object Settings : Route()

    /**
     * Trash screen - Shows all trashed links with restore/delete options
     */
    @Serializable
    data object Trash : Route()

    /**
     * Advanced settings screen (Phase 3/4)
     */
    @Serializable
    data object AdvancedSettings : Route()

    // ============ SETTINGS DETAIL ROUTES ============

    /**
     * Data & Storage settings screen
     * Shows storage usage, trash management, export/import
     */
    @Serializable
    data object DataStorage : Route()

    /**
     * Appearance settings screen
     * Theme selection, view options, card design
     */
    @Serializable
    data object Appearance : Route()

    /**
     * Privacy & Security settings screen
     * Privacy policy, data management, security options
     */
    @Serializable
    data object PrivacySecurity : Route()

    /**
     * About screen
     * App version, licenses, credits, feedback
     */
    @Serializable
    data object About : Route()

    /**
     * Sync settings screen (Phase 2)
     * Account status, sync frequency, sync options
     */
    @Serializable
    data object SyncSettings : Route()

    /**
     * Batch Import screen
     * Import multiple links at once from pasted text
     * @param prefillText Optional text to pre-fill in the paste field (from share intent)
     */
    @Serializable
    data class BatchImport(val prefillText: String? = null) : Route()


    // ============ AUTH ROUTES (Phase 2) ============

    /**
     * Welcome/Onboarding screen
     * Shows benefits of sync and allows offline-only mode
     */
    @Serializable
    data object Welcome : Route()

    /**
     * Login screen with Email OTP authentication
     */
    @Serializable
    data object Login : Route()

    /**
     * Sync setup screen after successful login
     * Allows collection selection and sync frequency configuration
     */
    @Serializable
    data object SyncSetup : Route()
}
