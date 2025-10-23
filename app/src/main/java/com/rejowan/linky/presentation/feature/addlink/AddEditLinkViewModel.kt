package com.rejowan.linky.presentation.feature.addlink

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rejowan.linky.domain.model.Collection
import com.rejowan.linky.domain.model.Link
import com.rejowan.linky.domain.usecase.collection.GetAllCollectionsUseCase
import com.rejowan.linky.domain.usecase.collection.SaveCollectionUseCase
import com.rejowan.linky.domain.usecase.link.GetLinkByIdUseCase
import com.rejowan.linky.domain.usecase.link.SaveLinkUseCase
import com.rejowan.linky.domain.usecase.link.UpdateLinkUseCase
import com.rejowan.linky.util.CollectionOperation
import com.rejowan.linky.util.ErrorHandler
import com.rejowan.linky.util.FileStorageManager
import com.rejowan.linky.util.LinkOperation
import com.rejowan.linky.util.LinkPreviewFetcher
import com.rejowan.linky.util.Result
import com.rejowan.linky.util.ValidationResult
import com.rejowan.linky.util.Validator
import java.util.UUID
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

class AddEditLinkViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val saveLinkUseCase: SaveLinkUseCase,
    private val updateLinkUseCase: UpdateLinkUseCase,
    private val getLinkByIdUseCase: GetLinkByIdUseCase,
    private val getAllCollectionsUseCase: GetAllCollectionsUseCase,
    private val saveCollectionUseCase: SaveCollectionUseCase,
    private val linkPreviewFetcher: LinkPreviewFetcher,
    private val fileStorageManager: FileStorageManager,
    private val preferencesManager: com.rejowan.linky.util.PreferencesManager
) : ViewModel() {

    private val _state = MutableStateFlow(AddEditLinkState())
    val state: StateFlow<AddEditLinkState> = _state.asStateFlow()

    private val _uiEvents = MutableSharedFlow<AddEditLinkUiEvent>()
    val uiEvents: SharedFlow<AddEditLinkUiEvent> = _uiEvents.asSharedFlow()

    init {
        val linkId = savedStateHandle.get<String>("linkId")
        val collectionId = savedStateHandle.get<String>("collectionId")
        val url = savedStateHandle.get<String>("url")
        Timber.d("AddEditLinkViewModel initialized | Edit mode: ${linkId != null} | LinkId: $linkId | PreselectedCollectionId: $collectionId | PrefilledUrl: $url")

        // Preselect collection if provided from navigation
        collectionId?.let {
            Timber.d("Preselecting collection: $it")
            _state.update { state -> state.copy(selectedCollectionId = it) }
        }

        // Prefill URL if provided from navigation (clipboard/share)
        url?.let {
            Timber.d("Prefilling URL from clipboard/share: $it")
            _state.update { state -> state.copy(url = it) }
            // Automatically fetch preview for pre-filled URLs
            Timber.d("Auto-fetching preview for pre-filled URL")
            fetchPreview()
        }

        loadCollections()

        // Initialize suggestion card visibility based on preferences
        _state.update { it.copy(showPreviewFetchSuggestion = preferencesManager.shouldShowPreviewFetchSuggestion()) }

        // Check if we're editing an existing link
        linkId?.let {
            Timber.d("Loading existing link for editing: $it")
            loadLink(it)
        } ?: Timber.d("Creating new link")
    }

    fun onEvent(event: AddEditLinkEvent) {
        Timber.d("onEvent: $event")

        when (event) {
            is AddEditLinkEvent.OnTitleChange -> {
                Timber.d("Title changed: ${event.title.take(50)}...")
                _state.update { it.copy(title = event.title) }
            }
            is AddEditLinkEvent.OnDescriptionChange -> {
                Timber.d("Description changed: ${event.description.take(100)}...")
                _state.update { it.copy(description = event.description) }
            }
            is AddEditLinkEvent.OnUrlChange -> {
                Timber.d("URL changed: ${event.url}")
                _state.update { it.copy(url = event.url) }
            }
            is AddEditLinkEvent.OnNoteChange -> {
                Timber.d("Note changed: ${event.note.take(100)}...")
                _state.update { it.copy(note = event.note) }
            }
            is AddEditLinkEvent.OnCollectionSelect -> {
                Timber.d("Collection selected: ${event.collectionId}")
                // When collection is deselected (null), automatically set hideFromHome to false
                if (event.collectionId == null) {
                    Timber.d("Collection deselected, resetting hideFromHome to false")
                    _state.update { it.copy(selectedCollectionId = null, hideFromHome = false) }
                } else {
                    _state.update { it.copy(selectedCollectionId = event.collectionId) }
                }
            }
            is AddEditLinkEvent.OnToggleFavorite -> {
                val newValue = !_state.value.isFavorite
                Timber.d("Favorite toggled: $newValue")
                _state.update { it.copy(isFavorite = newValue) }
            }
            is AddEditLinkEvent.OnToggleHideFromHome -> {
                val newValue = !_state.value.hideFromHome
                Timber.d("HideFromHome toggled: $newValue")
                _state.update { it.copy(hideFromHome = newValue) }
            }
            is AddEditLinkEvent.OnFetchPreview -> {
                Timber.d("Fetch preview requested")
                fetchPreview()
            }
            is AddEditLinkEvent.OnDismissPreviewSuggestion -> {
                Timber.d("Preview suggestion dismissed")
                preferencesManager.dismissPreviewFetchSuggestion()
                _state.update { it.copy(showPreviewFetchSuggestion = false) }
            }
            is AddEditLinkEvent.OnSave -> {
                Timber.d("Save link requested")
                saveLink()
            }
            is AddEditLinkEvent.OnCreateCollectionClick -> {
                Timber.d("Create collection dialog opened")
                _state.update { it.copy(showCreateCollectionDialog = true) }
            }
            is AddEditLinkEvent.OnNewCollectionNameChange -> {
                Timber.d("New collection name changed: ${event.name}")
                _state.update { it.copy(newCollectionName = event.name) }
            }
            is AddEditLinkEvent.OnNewCollectionColorChange -> {
                Timber.d("New collection color changed: ${event.color}")
                _state.update { it.copy(newCollectionColor = event.color) }
            }
            is AddEditLinkEvent.OnNewCollectionToggleFavorite -> {
                val newValue = !_state.value.newCollectionIsFavorite
                Timber.d("New collection favorite toggled: $newValue")
                _state.update { it.copy(newCollectionIsFavorite = newValue) }
            }
            is AddEditLinkEvent.OnCreateCollectionConfirm -> {
                Timber.d("Create collection confirm")
                createCollection()
            }
            is AddEditLinkEvent.OnCreateCollectionDismiss -> {
                Timber.d("Create collection dialog dismissed")
                _state.update {
                    it.copy(
                        showCreateCollectionDialog = false,
                        newCollectionName = "",
                        newCollectionColor = null,
                        newCollectionIsFavorite = false
                    )
                }
            }
        }
    }

    private fun loadCollections() {
        Timber.d("loadCollections: Starting to load collections")
        viewModelScope.launch {
            getAllCollectionsUseCase()
                .catch { e ->
                    Timber.e(e, "loadCollections: Failed to load collections - ${e.message}")
                }
                .collect { collections ->
                    Timber.d("loadCollections: Loaded ${collections.size} collections")
                    _state.update { it.copy(collections = collections) }
                }
        }
    }

    private fun createCollection() {
        Timber.d("createCollection: Starting to create collection")
        val name = _state.value.newCollectionName.trim()

        // Validate collection name
        when (val validationResult = Validator.validateCollectionName(name)) {
            is ValidationResult.Success -> {
                // Validation passed, continue
            }
            is ValidationResult.Error -> {
                Timber.w("createCollection: Invalid collection name - ${validationResult.message}")
                _state.update { it.copy(error = validationResult.message) }
                return
            }
        }

        viewModelScope.launch {
            val collection = Collection(
                id = UUID.randomUUID().toString(),
                name = name,
                color = _state.value.newCollectionColor,
                isFavorite = _state.value.newCollectionIsFavorite,
                sortOrder = _state.value.collections.size,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            Timber.d("createCollection: Saving collection | Name: $name | Color: ${_state.value.newCollectionColor} | IsFavorite: ${_state.value.newCollectionIsFavorite}")

            when (val result = saveCollectionUseCase(collection)) {
                is Result.Success -> {
                    Timber.d("createCollection: Collection created successfully | ID: ${collection.id}")
                    // Close dialog, clear form, and select the new collection
                    _state.update {
                        it.copy(
                            showCreateCollectionDialog = false,
                            newCollectionName = "",
                            newCollectionColor = null,
                            newCollectionIsFavorite = false,
                            selectedCollectionId = collection.id
                        )
                    }
                    // Reload collections to include the new one
                    loadCollections()
                }
                is Result.Error -> {
                    Timber.e(result.exception, "createCollection: Failed to create collection")
                    val errorMessage = ErrorHandler.getCollectionErrorMessage(result.exception, CollectionOperation.SAVE)
                    _state.update { it.copy(error = errorMessage) }
                }
                is Result.Loading -> {
                    // Loading state
                }
            }
        }
    }

    private fun loadLink(linkId: String) {
        Timber.d("loadLink: Starting to load link | LinkId: $linkId")
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, isEditMode = true) }

            getLinkByIdUseCase(linkId)
                .catch { e ->
                    Timber.e(e, "loadLink: Failed to load link - ${e.message}")
                    val errorMessage = ErrorHandler.getLinkErrorMessage(e, LinkOperation.LOAD)
                    _state.update { it.copy(isLoading = false, error = errorMessage) }
                }
                .collect { link ->
                    link?.let {
                        Timber.d("loadLink: Link loaded successfully | Title: ${it.title} | URL: ${it.url}")
                        Timber.d("loadLink: Link details | Favorite: ${it.isFavorite} | CollectionId: ${it.collectionId} | HideFromHome: ${it.hideFromHome} | PreviewPath: ${it.previewImagePath != null}")
                        _state.update { state ->
                            state.copy(
                                linkId = it.id,
                                title = it.title,
                                description = it.description ?: "",
                                url = it.url,
                                note = it.note ?: "",
                                selectedCollectionId = it.collectionId,
                                previewImagePath = it.previewImagePath,
                                previewUrl = it.previewUrl,
                                isFavorite = it.isFavorite,
                                hideFromHome = it.hideFromHome,
                                isArchived = it.isArchived, // Preserve archive status
                                isLoading = false
                            )
                        }
                    } ?: run {
                        Timber.w("loadLink: Link not found | LinkId: $linkId")
                        _state.update { it.copy(isLoading = false, error = "Link not found") }
                    }
                }
        }
    }

    private fun fetchPreview() {
        val currentUrl = _state.value.url.trim()
        Timber.d("fetchPreview: Starting preview fetch | URL: $currentUrl")

        // Validate URL before fetching
        if (currentUrl.isBlank()) {
            Timber.w("fetchPreview: URL is blank, cannot fetch preview")
            _state.update { it.copy(error = "Please enter a URL first") }
            return
        }

        Timber.d("fetchPreview: Setting fetching state to true, clearing previous error")
        _state.update { it.copy(isFetchingPreview = true, error = null) }

        viewModelScope.launch {
            try {
                Timber.d("fetchPreview: Calling linkPreviewFetcher...")
                val preview = linkPreviewFetcher.fetchPreview(currentUrl)

                if (preview != null) {
                    Timber.d("========================================")
                    Timber.d("fetchPreview: ✓ Preview fetched successfully")
                    Timber.d("fetchPreview: Title: '${preview.title}'")
                    Timber.d("fetchPreview: Description: ${preview.description?.take(50) ?: "None"}...")
                    Timber.d("fetchPreview: Site Name: ${preview.siteName ?: "None"}")
                    Timber.d("fetchPreview: ImageURL: ${preview.imageUrl ?: "None"}")
                    Timber.d("fetchPreview: Favicon: ${preview.favicon ?: "None"}")
                    Timber.d("========================================")

                    val currentState = _state.value
                    val ensuredLinkId = currentState.linkId ?: UUID.randomUUID().toString()
                    Timber.d("fetchPreview: LinkId: $ensuredLinkId (${if (currentState.linkId == null) "newly generated" else "existing from state"})")

                    // Implement fallback chain for preview image: imageUrl → favicon → placeholder (null)
                    Timber.d("fetchPreview: Starting image fallback chain...")
                    var localImagePath: String? = null
                    var previewUrlToStore: String? = null

                    // Try primary image (og:image, twitter:image)
                    if (preview.imageUrl != null) {
                        Timber.d("fetchPreview: [Fallback 1/3] Attempting primary image (og:image/twitter:image)")
                        Timber.d("fetchPreview: Image URL: ${preview.imageUrl}")
                        localImagePath = fileStorageManager.savePreviewImageFromUrl(preview.imageUrl, ensuredLinkId)
                        if (localImagePath != null) {
                            previewUrlToStore = preview.imageUrl
                            Timber.d("fetchPreview: ✓ Primary image saved successfully")
                            Timber.d("fetchPreview: Local path: $localImagePath")
                        } else {
                            Timber.w("fetchPreview: ✗ Failed to save primary image (download/save failed)")
                        }
                    } else {
                        Timber.d("fetchPreview: [Fallback 1/3] ✗ No primary image URL found in preview")
                    }

                    // Fallback to favicon if primary image failed
                    if (localImagePath == null && preview.favicon != null) {
                        Timber.d("fetchPreview: [Fallback 2/3] Primary image unavailable, trying favicon")
                        Timber.d("fetchPreview: Favicon URL: ${preview.favicon}")
                        localImagePath = fileStorageManager.savePreviewImageFromUrl(preview.favicon, ensuredLinkId)
                        if (localImagePath != null) {
                            previewUrlToStore = preview.favicon
                            Timber.d("fetchPreview: ✓ Favicon saved successfully as preview")
                            Timber.d("fetchPreview: Local path: $localImagePath")
                        } else {
                            Timber.w("fetchPreview: ✗ Failed to save favicon (download/save failed)")
                        }
                    } else if (localImagePath == null) {
                        Timber.d("fetchPreview: [Fallback 2/3] ✗ No favicon URL available")
                    }

                    // If both failed, use placeholder (null)
                    if (localImagePath == null) {
                        Timber.d("fetchPreview: [Fallback 3/3] No image available from any source")
                        Timber.d("fetchPreview: Using placeholder (no preview image)")
                    }

                    // Always update title with preview title (allow users to refresh title by re-fetching)
                    val oldTitle = _state.value.title
                    val newTitle = preview.title
                    Timber.d("fetchPreview: Title update | Old: '${oldTitle.take(50)}${if (oldTitle.length > 50) "..." else ""}' → New: '${newTitle.take(50)}${if (newTitle.length > 50) "..." else ""}'")

                    _state.update { latestState ->
                        latestState.copy(
                            linkId = ensuredLinkId,
                            isFetchingPreview = false,
                            // Always update title and description from preview
                            title = preview.title,
                            description = preview.description ?: "",
                            previewUrl = previewUrlToStore,
                            previewImagePath = localImagePath,
                            error = null // Clear any previous errors
                        )
                    }

                    Timber.d("fetchPreview: State update complete")
                    Timber.d("fetchPreview: Final state | Title: ${preview.title} | Description: ${preview.description?.take(50) ?: "None"} | Image: ${localImagePath != null} | Error: None")
                    Timber.d("========================================")
                } else {
                    // No preview found - reset preview data and show error
                    Timber.w("========================================")
                    Timber.w("fetchPreview: ✗ No preview data returned from fetcher")
                    Timber.w("fetchPreview: The website may not support previews or returned null data")
                    Timber.w("fetchPreview: Resetting all preview state (title, description, image, URL)")
                    _state.update {
                        it.copy(
                            isFetchingPreview = false,
                            // Reset all preview data when not found
                            title = "", // Clear title field
                            description = "", // Clear description field
                            previewUrl = null,
                            previewImagePath = null,
                            error = "No preview found for this URL. The website may not support previews or is unavailable."
                        )
                    }
                    Timber.w("fetchPreview: State updated | Title cleared | Description cleared | Preview cleared | Error message set")
                    Timber.w("========================================")
                }
            } catch (e: Exception) {
                Timber.e("========================================")
                Timber.e(e, "fetchPreview: ✗ Exception occurred during preview fetch")
                Timber.e("fetchPreview: Exception type: ${e.javaClass.simpleName}")
                Timber.e("fetchPreview: Exception message: ${e.message}")

                val errorMessage = ErrorHandler.getLinkErrorMessage(e, LinkOperation.FETCH_PREVIEW)
                Timber.e("fetchPreview: User-friendly error: $errorMessage")
                Timber.e("fetchPreview: Resetting all preview state due to error (title, description, image, URL)")

                // Reset all preview data on error
                _state.update {
                    it.copy(
                        isFetchingPreview = false,
                        title = "", // Clear title field
                        description = "", // Clear description field
                        previewUrl = null,
                        previewImagePath = null,
                        error = errorMessage
                    )
                }
                Timber.e("fetchPreview: State updated | Title cleared | Description cleared | Preview cleared | Error message set")
                Timber.e("========================================")
            }
        }
    }

    private fun saveLink() {
        val currentState = _state.value
        Timber.d("saveLink: Starting save process | Edit mode: ${currentState.isEditMode}")
        Timber.d("saveLink: Current state | URL: ${currentState.url.trim()} | Title: ${currentState.title.trim()}")
        Timber.d("saveLink: Current state | CollectionId: ${currentState.selectedCollectionId} | Favorite: ${currentState.isFavorite}")

        // Comprehensive validation using Validator
        val validationResult = Validator.validateLink(
            url = currentState.url.trim(),
            title = currentState.title.trim(),
            description = currentState.description.trim().ifBlank { null },
            note = currentState.note.trim().ifBlank { null }
        )

        if (validationResult is ValidationResult.Error) {
            Timber.w("saveLink: Validation failed - ${validationResult.message}")
            _state.update { it.copy(error = validationResult.message) }
            return
        }

        Timber.d("saveLink: Validation passed, proceeding to save")

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val linkId = currentState.linkId ?: UUID.randomUUID().toString()
            Timber.d("saveLink: Link ID: $linkId (${if (currentState.linkId == null) "generated" else "existing"})")

            val link = Link(
                id = linkId,
                title = currentState.title.trim(),
                description = currentState.description.trim().ifBlank { null },
                url = currentState.url.trim(),
                note = currentState.note.trim().ifBlank { null },
                collectionId = currentState.selectedCollectionId,
                previewImagePath = currentState.previewImagePath,
                previewUrl = currentState.previewUrl,
                isFavorite = currentState.isFavorite,
                hideFromHome = currentState.hideFromHome,
                isArchived = currentState.isArchived // Preserve archive status when editing
            )

            Timber.d("saveLink: Link object created | ID: ${link.id} | Title: ${link.title}")
            Timber.d("saveLink: Link details | URL: ${link.url} | Collection: ${link.collectionId} | Favorite: ${link.isFavorite} | HideFromHome: ${link.hideFromHome}")
            Timber.d("saveLink: Link previews | ImagePath: ${link.previewImagePath != null} | PreviewURL: ${link.previewUrl != null}")

            val operation = if (currentState.isEditMode) "UPDATE" else "SAVE"
            Timber.d("saveLink: Calling ${operation} use case...")

            val result = if (currentState.isEditMode) {
                updateLinkUseCase(link)
            } else {
                saveLinkUseCase(link)
            }

            when (result) {
                is Result.Success -> {
                    Timber.d("saveLink: Link ${operation.lowercase()}d successfully | ID: ${link.id}")
                    // Add small delay to allow database changes to propagate through Flows
                    kotlinx.coroutines.delay(150)
                    _state.update { it.copy(isLoading = false) }
                    Timber.d("saveLink: Emitting LinkSaved event")
                    _uiEvents.emit(AddEditLinkUiEvent.LinkSaved)
                }
                is Result.Error -> {
                    Timber.e(result.exception, "saveLink: Failed to ${operation.lowercase()} link - ${result.exception.message}")
                    val operationEnum = if (currentState.isEditMode) LinkOperation.UPDATE else LinkOperation.SAVE
                    val errorMessage = ErrorHandler.getLinkErrorMessage(result.exception, operationEnum)
                    Timber.w("saveLink: Error message for user: $errorMessage")
                    _state.update {
                        it.copy(isLoading = false, error = errorMessage)
                    }
                }
                is Result.Loading -> {
                    Timber.d("saveLink: Result is Loading (no-op)")
                }
            }
        }
    }
}

sealed class AddEditLinkUiEvent {
    data object LinkSaved : AddEditLinkUiEvent()
}

sealed class AddEditLinkEvent {
    data class OnTitleChange(val title: String) : AddEditLinkEvent()
    data class OnDescriptionChange(val description: String) : AddEditLinkEvent()
    data class OnUrlChange(val url: String) : AddEditLinkEvent()
    data class OnNoteChange(val note: String) : AddEditLinkEvent()
    data class OnCollectionSelect(val collectionId: String?) : AddEditLinkEvent()
    data object OnToggleFavorite : AddEditLinkEvent()
    data object OnToggleHideFromHome : AddEditLinkEvent()
    data object OnFetchPreview : AddEditLinkEvent()
    data object OnDismissPreviewSuggestion : AddEditLinkEvent()
    data object OnSave : AddEditLinkEvent()
    // Create collection dialog events
    data object OnCreateCollectionClick : AddEditLinkEvent()
    data class OnNewCollectionNameChange(val name: String) : AddEditLinkEvent()
    data class OnNewCollectionColorChange(val color: String?) : AddEditLinkEvent()
    data object OnNewCollectionToggleFavorite : AddEditLinkEvent()
    data object OnCreateCollectionConfirm : AddEditLinkEvent()
    data object OnCreateCollectionDismiss : AddEditLinkEvent()
}
