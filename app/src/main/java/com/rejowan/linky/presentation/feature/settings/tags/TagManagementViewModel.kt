package com.rejowan.linky.presentation.feature.settings.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rejowan.linky.domain.model.Tag
import com.rejowan.linky.domain.model.TagWithCount
import com.rejowan.linky.domain.usecase.tag.DeleteTagUseCase
import com.rejowan.linky.domain.usecase.tag.GetTagsWithLinkCountUseCase
import com.rejowan.linky.domain.usecase.tag.SaveTagUseCase
import com.rejowan.linky.domain.usecase.tag.UpdateTagUseCase
import com.rejowan.linky.util.Result
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

data class TagManagementState(
    val tags: List<TagWithCount> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    // Dialog state
    val showCreateDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val editingTag: TagWithCount? = null,
    val showDeleteConfirmDialog: Boolean = false,
    val deletingTag: TagWithCount? = null
)

sealed class TagManagementUiEvent {
    data class ShowMessage(val message: String) : TagManagementUiEvent()
}

sealed class TagManagementEvent {
    data object OnRefresh : TagManagementEvent()
    data object OnShowCreateDialog : TagManagementEvent()
    data object OnDismissCreateDialog : TagManagementEvent()
    data class OnCreateTag(val name: String, val color: String) : TagManagementEvent()
    data class OnShowEditDialog(val tag: TagWithCount) : TagManagementEvent()
    data object OnDismissEditDialog : TagManagementEvent()
    data class OnUpdateTag(val tag: Tag) : TagManagementEvent()
    data class OnShowDeleteConfirm(val tag: TagWithCount) : TagManagementEvent()
    data object OnDismissDeleteConfirm : TagManagementEvent()
    data class OnDeleteTag(val tagId: String) : TagManagementEvent()
}

class TagManagementViewModel(
    private val getTagsWithLinkCountUseCase: GetTagsWithLinkCountUseCase,
    private val saveTagUseCase: SaveTagUseCase,
    private val updateTagUseCase: UpdateTagUseCase,
    private val deleteTagUseCase: DeleteTagUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(TagManagementState())
    val state: StateFlow<TagManagementState> = _state.asStateFlow()

    private val _uiEvents = MutableSharedFlow<TagManagementUiEvent>()
    val uiEvents: SharedFlow<TagManagementUiEvent> = _uiEvents.asSharedFlow()

    init {
        loadTags()
    }

    fun onEvent(event: TagManagementEvent) {
        when (event) {
            is TagManagementEvent.OnRefresh -> loadTags()

            is TagManagementEvent.OnShowCreateDialog -> {
                _state.update { it.copy(showCreateDialog = true) }
            }

            is TagManagementEvent.OnDismissCreateDialog -> {
                _state.update { it.copy(showCreateDialog = false) }
            }

            is TagManagementEvent.OnCreateTag -> {
                createTag(event.name, event.color)
            }

            is TagManagementEvent.OnShowEditDialog -> {
                _state.update { it.copy(showEditDialog = true, editingTag = event.tag) }
            }

            is TagManagementEvent.OnDismissEditDialog -> {
                _state.update { it.copy(showEditDialog = false, editingTag = null) }
            }

            is TagManagementEvent.OnUpdateTag -> {
                updateTag(event.tag)
            }

            is TagManagementEvent.OnShowDeleteConfirm -> {
                _state.update { it.copy(showDeleteConfirmDialog = true, deletingTag = event.tag) }
            }

            is TagManagementEvent.OnDismissDeleteConfirm -> {
                _state.update { it.copy(showDeleteConfirmDialog = false, deletingTag = null) }
            }

            is TagManagementEvent.OnDeleteTag -> {
                deleteTag(event.tagId)
            }
        }
    }

    private fun loadTags() {
        Timber.d("loadTags: Loading tags with link count")
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            getTagsWithLinkCountUseCase()
                .catch { e ->
                    Timber.e(e, "loadTags: Failed to load tags")
                    _state.update { it.copy(isLoading = false, error = "Failed to load tags") }
                }
                .collect { tags ->
                    Timber.d("loadTags: Loaded ${tags.size} tags")
                    _state.update { it.copy(tags = tags, isLoading = false) }
                }
        }
    }

    private fun createTag(name: String, color: String) {
        Timber.d("createTag: Creating tag | Name: $name | Color: $color")
        viewModelScope.launch {
            val tag = Tag(
                name = name.trim(),
                color = color
            )

            when (val result = saveTagUseCase(tag)) {
                is Result.Success -> {
                    Timber.d("createTag: Tag created successfully")
                    _state.update { it.copy(showCreateDialog = false) }
                    _uiEvents.emit(TagManagementUiEvent.ShowMessage("Tag created"))
                }
                is Result.Error -> {
                    Timber.e(result.exception, "createTag: Failed to create tag")
                    _uiEvents.emit(
                        TagManagementUiEvent.ShowMessage(
                            result.exception.message ?: "Failed to create tag"
                        )
                    )
                }
                is Result.Loading -> {}
            }
        }
    }

    private fun updateTag(tag: Tag) {
        Timber.d("updateTag: Updating tag | ID: ${tag.id} | Name: ${tag.name}")
        viewModelScope.launch {
            when (val result = updateTagUseCase(tag)) {
                is Result.Success -> {
                    Timber.d("updateTag: Tag updated successfully")
                    _state.update { it.copy(showEditDialog = false, editingTag = null) }
                    _uiEvents.emit(TagManagementUiEvent.ShowMessage("Tag updated"))
                }
                is Result.Error -> {
                    Timber.e(result.exception, "updateTag: Failed to update tag")
                    _uiEvents.emit(
                        TagManagementUiEvent.ShowMessage(
                            result.exception.message ?: "Failed to update tag"
                        )
                    )
                }
                is Result.Loading -> {}
            }
        }
    }

    private fun deleteTag(tagId: String) {
        Timber.d("deleteTag: Deleting tag | ID: $tagId")
        viewModelScope.launch {
            when (val result = deleteTagUseCase(tagId)) {
                is Result.Success -> {
                    Timber.d("deleteTag: Tag deleted successfully")
                    _state.update { it.copy(showDeleteConfirmDialog = false, deletingTag = null) }
                    _uiEvents.emit(TagManagementUiEvent.ShowMessage("Tag deleted"))
                }
                is Result.Error -> {
                    Timber.e(result.exception, "deleteTag: Failed to delete tag")
                    _state.update { it.copy(showDeleteConfirmDialog = false, deletingTag = null) }
                    _uiEvents.emit(
                        TagManagementUiEvent.ShowMessage(
                            result.exception.message ?: "Failed to delete tag"
                        )
                    )
                }
                is Result.Loading -> {}
            }
        }
    }
}
