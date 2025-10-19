package com.rejowan.linky.presentation.feature.collections

import com.rejowan.linky.domain.model.FolderWithLinkCount

data class CollectionsState(
    val folders: List<FolderWithLinkCount> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showCreateDialog: Boolean = false,
    val newFolderName: String = "",
    val selectedFolderColor: String? = null
)
