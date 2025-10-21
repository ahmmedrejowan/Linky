package com.rejowan.linky.presentation.feature.addlink

import com.rejowan.linky.domain.model.Collection

data class AddEditLinkState(
    val linkId: String? = null,
    val title: String = "",
    val description: String = "",
    val url: String = "",
    val note: String = "",
    val selectedCollectionId: String? = null,
    val collections: List<Collection> = emptyList(),
    val previewImagePath: String? = null,
    val previewUrl: String? = null,
    val isFavorite: Boolean = false,
    val hideFromHome: Boolean = false,
    val isLoading: Boolean = false,
    val isFetchingPreview: Boolean = false,
    val error: String? = null,
    val isEditMode: Boolean = false,
    val showPreviewFetchSuggestion: Boolean = false,
    // Create collection dialog state
    val showCreateCollectionDialog: Boolean = false,
    val newCollectionName: String = "",
    val newCollectionColor: String? = null,
    val newCollectionIsFavorite: Boolean = false
)
