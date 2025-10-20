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
     */
    @Serializable
    data object Main : Route()

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
     */
    @Serializable
    data class AddEditLink(val linkId: String? = null, val collectionId: String? = null) : Route()

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
