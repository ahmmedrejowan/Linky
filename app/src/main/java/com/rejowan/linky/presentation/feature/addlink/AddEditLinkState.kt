package com.rejowan.linky.presentation.feature.addlink

import com.rejowan.linky.domain.model.Folder

data class AddEditLinkState(
    val linkId: String? = null,
    val title: String = "",
    val url: String = "",
    val note: String = "",
    val selectedFolderId: String? = null,
    val folders: List<Folder> = emptyList(),
    val previewImagePath: String? = null,
    val previewUrl: String? = null,
    val isFavorite: Boolean = false,
    val isLoading: Boolean = false,
    val isFetchingPreview: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false,
    val isEditMode: Boolean = false
)
