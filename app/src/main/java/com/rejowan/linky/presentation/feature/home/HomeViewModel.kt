package com.rejowan.linky.presentation.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rejowan.linky.domain.model.Link
import com.rejowan.linky.domain.usecase.link.DeleteLinkUseCase
import com.rejowan.linky.domain.usecase.link.GetAllLinksUseCase
import com.rejowan.linky.domain.usecase.link.GetArchivedLinksUseCase
import com.rejowan.linky.domain.usecase.link.GetFavoriteLinksUseCase
import com.rejowan.linky.domain.usecase.link.GetTrashedLinksUseCase
import com.rejowan.linky.domain.usecase.link.ToggleFavoriteUseCase
import com.rejowan.linky.util.ErrorHandler
import com.rejowan.linky.util.LinkOperation
import com.rejowan.linky.util.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val getAllLinksUseCase: GetAllLinksUseCase,
    private val getFavoriteLinksUseCase: GetFavoriteLinksUseCase,
    private val getArchivedLinksUseCase: GetArchivedLinksUseCase,
    private val getTrashedLinksUseCase: GetTrashedLinksUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val deleteLinkUseCase: DeleteLinkUseCase,
    private val linkRepository: com.rejowan.linky.domain.repository.LinkRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _uiEvents = MutableSharedFlow<HomeUiEvent>()
    val uiEvents: SharedFlow<HomeUiEvent> = _uiEvents.asSharedFlow()

    // Flow that tracks the current filter type
    private val filterTypeFlow = MutableStateFlow(FilterType.ALL)

    init {
        observeFilteredLinks()
        observeCounts()
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.OnFilterTypeChange -> {
                _state.update { it.copy(filterType = event.filterType) }
                filterTypeFlow.value = event.filterType
            }
            is HomeEvent.OnSortTypeChange -> {
                _state.update { it.copy(sortType = event.sortType) }
                // Re-apply sorting to current links
                applySorting()
            }
            is HomeEvent.OnToggleFavorite -> toggleFavorite(event.linkId, event.isFavorite)
            is HomeEvent.OnDeleteLink -> deleteLink(event.linkId)
            is HomeEvent.OnRefresh -> {
                // Trigger a refresh by re-emitting the current filter
                filterTypeFlow.value = _state.value.filterType
            }
            is HomeEvent.OnClipboardUrlDetected -> {
                Timber.tag("HomeViewModel").d("OnClipboardUrlDetected event received: ${event.url}")
                handleClipboardUrl(event.url)
            }
            is HomeEvent.OnDismissClipboardPrompt -> {
                _state.update { it.copy(showClipboardPrompt = false, clipboardUrl = null) }
                Timber.tag("HomeViewModel").d("Clipboard prompt dismissed")
            }
        }
    }

    /**
     * Handle clipboard URL detection
     * Validates URL and shows prompt if valid and not already prompted
     */
    private fun handleClipboardUrl(url: String) {
        Timber.tag("HomeViewModel").d("handleClipboardUrl called with: $url")
        val trimmedUrl = url.trim()

        // Check if we've already prompted for this URL
        if (_state.value.promptedUrls.contains(trimmedUrl)) {
            Timber.tag("HomeViewModel").d("✗ Already prompted for this URL: $trimmedUrl")
            return
        }

        // Basic URL validation
        val isValid = isValidUrl(trimmedUrl)
        Timber.tag("HomeViewModel").d("URL validation result: $isValid for URL: $trimmedUrl")

        if (isValid) {
            _state.update {
                it.copy(
                    clipboardUrl = trimmedUrl,
                    showClipboardPrompt = true,
                    promptedUrls = it.promptedUrls + trimmedUrl // Add to prompted set
                )
            }
            Timber.tag("HomeViewModel").d("✓ State updated - showClipboardPrompt=true, clipboardUrl=$trimmedUrl")
        } else {
            Timber.tag("HomeViewModel").d("✗ Invalid URL in clipboard: $trimmedUrl")
        }
    }

    /**
     * Validate if string is a valid URL
     */
    private fun isValidUrl(url: String): Boolean {
        return try {
            val urlPattern = Regex(
                "^(https?://)" + // Protocol
                "([a-zA-Z0-9.-]+)" + // Domain
                "(:[0-9]{1,5})?" + // Optional port
                "(/.*)?$" // Optional path
            )
            urlPattern.matches(url) ||
            // Also accept URLs without protocol
            Regex("^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/.*)?$").matches(url)
        } catch (e: Exception) {
            Timber.e(e, "Error validating URL: $url")
            false
        }
    }

    private fun observeFilteredLinks() {
        viewModelScope.launch {
            filterTypeFlow
                .flatMapLatest { filterType ->
                    _state.update { it.copy(isLoading = true, error = null) }

                    when (filterType) {
                        FilterType.ALL -> getAllLinksUseCase()
                        FilterType.FAVORITES -> getFavoriteLinksUseCase()
                        FilterType.ARCHIVED -> getArchivedLinksUseCase()
                        FilterType.TRASH -> getTrashedLinksUseCase()
                    }
                }
                .catch { e ->
                    val errorMessage = ErrorHandler.getLinkErrorMessage(e, LinkOperation.LOAD_ALL)
                    _state.update { it.copy(isLoading = false, error = errorMessage) }
                }
                .collect { links ->
                    val sortedLinks = sortLinks(links, _state.value.sortType, _state.value.filterType)
                    _state.update { it.copy(links = sortedLinks, isLoading = false, error = null) }
                }
        }
    }

    private fun applySorting() {
        val currentLinks = _state.value.links
        val sortedLinks = sortLinks(currentLinks, _state.value.sortType, _state.value.filterType)
        _state.update { it.copy(links = sortedLinks) }
    }

    private fun sortLinks(links: List<Link>, sortType: SortType, filterType: FilterType): List<Link> {
        // Apply the selected sort to all links
        val sorted = when (sortType) {
            SortType.DATE_ADDED_DESC -> links.sortedByDescending { it.createdAt }
            SortType.DATE_ADDED_ASC -> links.sortedBy { it.createdAt }
            SortType.TITLE_ASC -> links.sortedBy { it.title.lowercase() }
            SortType.TITLE_DESC -> links.sortedByDescending { it.title.lowercase() }
            SortType.LAST_MODIFIED -> links.sortedByDescending { it.updatedAt }
        }

        // Prioritize favorites at top only for ALL and ARCHIVED filters
        return if (filterType == FilterType.ALL || filterType == FilterType.ARCHIVED) {
            val favorites = sorted.filter { it.isFavorite }
            val nonFavorites = sorted.filter { !it.isFavorite }
            favorites + nonFavorites
        } else {
            sorted
        }
    }

    private fun observeCounts() {
        // Observe all links count
        viewModelScope.launch {
            linkRepository.getAllLinksCount()
                .catch { e ->
                    Timber.e(e, "Failed to observe all links count")
                }
                .collect { count ->
                    _state.update { it.copy(allLinksCount = count) }
                }
        }

        // Observe favorites count
        viewModelScope.launch {
            linkRepository.getFavoriteLinksCount()
                .catch { e ->
                    Timber.e(e, "Failed to observe favorites count")
                }
                .collect { count ->
                    _state.update { it.copy(favoriteLinksCount = count) }
                }
        }

        // Observe archived count
        viewModelScope.launch {
            linkRepository.getArchivedLinksCount()
                .catch { e ->
                    Timber.e(e, "Failed to observe archived count")
                }
                .collect { count ->
                    _state.update { it.copy(archivedLinksCount = count) }
                }
        }
    }

    private fun toggleFavorite(linkId: String, isFavorite: Boolean) {
        viewModelScope.launch {
            when (val result = toggleFavoriteUseCase(linkId, isFavorite)) {
                is Result.Success -> {
                    Timber.d("Toggled favorite for link: $linkId")
                    // Emit event with undo action
                    _uiEvents.emit(HomeUiEvent.ShowFavoriteToggled(
                        linkId = linkId,
                        isFavorite = isFavorite
                    ))
                }
                is Result.Error -> {
                    val errorMessage = ErrorHandler.getLinkErrorMessage(result.exception, LinkOperation.TOGGLE_FAVORITE)
                    _uiEvents.emit(HomeUiEvent.ShowError(errorMessage))
                }
                is Result.Loading -> { /* No-op */ }
            }
        }
    }

    private fun deleteLink(linkId: String) {
        viewModelScope.launch {
            when (val result = deleteLinkUseCase(linkId, softDelete = true)) {
                is Result.Success -> {
                    Timber.d("Deleted link: $linkId")
                }
                is Result.Error -> {
                    val errorMessage = ErrorHandler.getLinkErrorMessage(result.exception, LinkOperation.DELETE)
                    _uiEvents.emit(HomeUiEvent.ShowError(errorMessage))
                }
                is Result.Loading -> { /* No-op */ }
            }
        }
    }
}

sealed class HomeUiEvent {
    data class ShowError(val message: String) : HomeUiEvent()
    data class ShowFavoriteToggled(val linkId: String, val isFavorite: Boolean) : HomeUiEvent()
}

sealed class HomeEvent {
    data class OnFilterTypeChange(val filterType: FilterType) : HomeEvent()
    data class OnSortTypeChange(val sortType: SortType) : HomeEvent()
    data class OnToggleFavorite(val linkId: String, val isFavorite: Boolean) : HomeEvent()
    data class OnDeleteLink(val linkId: String) : HomeEvent()
    data object OnRefresh : HomeEvent()
    data class OnClipboardUrlDetected(val url: String) : HomeEvent()
    data object OnDismissClipboardPrompt : HomeEvent()
}
