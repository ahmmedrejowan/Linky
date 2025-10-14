package com.rejowan.linky.presentation.collections

import com.rejowan.linky.domain.model.Folder

data class CollectionsState(
    val folders: List<Folder> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showCreateDialog: Boolean = false,
    val newFolderName: String = "",
    val selectedFolderColor: String? = null
)
