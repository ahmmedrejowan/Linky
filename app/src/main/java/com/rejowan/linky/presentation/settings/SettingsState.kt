package com.rejowan.linky.presentation.settings

data class SettingsState(
    val totalLinks: Int = 0,
    val totalCollections: Int = 0,
    val totalStorageUsed: String = "0 MB",
    val appVersion: String = "",
    val theme: String = "System",
    val isLoading: Boolean = false,
    val error: String? = null
)
