package com.rejowan.linky.presentation.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.rejowan.linky.domain.model.Link
import com.rejowan.linky.domain.repository.CollectionRepository
import com.rejowan.linky.domain.usecase.link.DeleteLinkUseCase
import com.rejowan.linky.domain.usecase.link.GetAllLinksUseCase
import com.rejowan.linky.domain.usecase.link.GetArchivedLinksUseCase
import com.rejowan.linky.domain.usecase.link.GetFavoriteLinksUseCase
import com.rejowan.linky.domain.usecase.link.GetTrashedLinksUseCase
import com.rejowan.linky.domain.usecase.link.ToggleFavoriteUseCase
import com.rejowan.linky.util.ErrorHandler
import com.rejowan.linky.util.LinkOperation
import com.rejowan.linky.util.Result
import java.util.Calendar
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
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val getAllLinksUseCase: GetAllLinksUseCase,
    private val getFavoriteLinksUseCase: GetFavoriteLinksUseCase,
    private val getArchivedLinksUseCase: GetArchivedLinksUseCase,
    private val getTrashedLinksUseCase: GetTrashedLinksUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val toggleArchiveUseCase: com.rejowan.linky.domain.usecase.link.ToggleArchiveUseCase,
    private val deleteLinkUseCase: DeleteLinkUseCase,
    private val restoreLinkUseCase: com.rejowan.linky.domain.usecase.link.RestoreLinkUseCase,
    private val linkRepository: com.rejowan.linky.domain.repository.LinkRepository,
    private val collectionRepository: CollectionRepository
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
        loadFilterOptions()
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
            is HomeEvent.OnViewModeChange -> {
                _state.update { it.copy(viewMode = event.viewMode) }
            }
            is HomeEvent.OnToggleFavorite -> toggleFavorite(event.linkId, event.isFavorite, event.silent)
            is HomeEvent.OnDeleteLink -> deleteLink(event.linkId)
            is HomeEvent.OnRestoreLink -> restoreLink(event.linkId)
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
            HomeEvent.OnShowAdvancedFilterSheet -> {
                _state.update { it.copy(showAdvancedFilterSheet = true) }
            }
            HomeEvent.OnDismissAdvancedFilterSheet -> {
                _state.update { it.copy(showAdvancedFilterSheet = false) }
            }
            is HomeEvent.OnApplyAdvancedFilter -> {
                _state.update { it.copy(advancedFilter = event.filter, showAdvancedFilterSheet = false) }
                // Trigger refresh to apply the new filter
                filterTypeFlow.value = _state.value.filterType
            }
            HomeEvent.OnClearAdvancedFilter -> {
                _state.update { it.copy(advancedFilter = AdvancedFilter.EMPTY) }
                // Trigger refresh
                filterTypeFlow.value = _state.value.filterType
            }
            // Bulk selection events
            HomeEvent.OnEnterSelectionMode -> {
                _state.update { it.copy(isSelectionMode = true, selectedLinkIds = emptySet()) }
            }
            HomeEvent.OnExitSelectionMode -> {
                _state.update { it.copy(isSelectionMode = false, selectedLinkIds = emptySet()) }
            }
            is HomeEvent.OnToggleLinkSelection -> {
                val currentSelection = _state.value.selectedLinkIds
                val newSelection = if (currentSelection.contains(event.linkId)) {
                    currentSelection - event.linkId
                } else {
                    currentSelection + event.linkId
                }
                _state.update { it.copy(selectedLinkIds = newSelection) }
            }
            HomeEvent.OnSelectAll -> {
                val allLinkIds = _state.value.links.map { it.id }.toSet()
                _state.update { it.copy(selectedLinkIds = allLinkIds) }
            }
            HomeEvent.OnDeselectAll -> {
                _state.update { it.copy(selectedLinkIds = emptySet()) }
            }
            HomeEvent.OnBulkDelete -> bulkDelete()
            HomeEvent.OnBulkFavorite -> bulkFavorite(true)
            HomeEvent.OnBulkUnfavorite -> bulkFavorite(false)
            HomeEvent.OnShowBulkMoveSheet -> {
                _state.update { it.copy(showBulkMoveSheet = true) }
            }
            HomeEvent.OnDismissBulkMoveSheet -> {
                _state.update { it.copy(showBulkMoveSheet = false) }
            }
            is HomeEvent.OnBulkMoveToCollection -> bulkMoveToCollection(event.collectionId)
        }
    }

    /**
     * Handle clipboard URL detection
     * Validates URL and shows prompt if valid, not already prompted, and not already saved
     */
    private fun handleClipboardUrl(url: String) {
        Timber.tag("HomeViewModel").d("handleClipboardUrl called with: $url")
        val trimmedUrl = url.trim()

        // Check if we've already prompted for this URL in this session
        if (_state.value.promptedUrls.contains(trimmedUrl)) {
            Timber.tag("HomeViewModel").d("✗ Already prompted for this URL: $trimmedUrl")
            return
        }

        // Basic URL validation
        val isValid = isValidUrl(trimmedUrl)
        Timber.tag("HomeViewModel").d("URL validation result: $isValid for URL: $trimmedUrl")

        if (!isValid) {
            Timber.tag("HomeViewModel").d("✗ Invalid URL in clipboard: $trimmedUrl")
            return
        }

        // Check if URL already exists in database
        viewModelScope.launch {
            val exists = linkRepository.existsByUrl(trimmedUrl)
            if (exists) {
                Timber.tag("HomeViewModel").d("✗ URL already exists in database: $trimmedUrl")
                // Add to prompted set to avoid checking again in this session
                _state.update { it.copy(promptedUrls = it.promptedUrls + trimmedUrl) }
                return@launch
            }

            // URL is valid and doesn't exist - show prompt
            _state.update {
                it.copy(
                    clipboardUrl = trimmedUrl,
                    showClipboardPrompt = true,
                    promptedUrls = it.promptedUrls + trimmedUrl // Add to prompted set
                )
            }
            Timber.tag("HomeViewModel").d("✓ State updated - showClipboardPrompt=true, clipboardUrl=$trimmedUrl")
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
                        FilterType.TRASH -> getTrashedLinksUseCase()
                    }
                }
                .catch { e ->
                    val errorMessage = ErrorHandler.getLinkErrorMessage(e, LinkOperation.LOAD_ALL)
                    _state.update { it.copy(isLoading = false, error = errorMessage) }
                }
                .collect { links ->
                    val filteredLinks = applyAdvancedFilter(links, _state.value.advancedFilter)
                    val sortedLinks = sortLinks(filteredLinks, _state.value.sortType, _state.value.filterType)
                    _state.update { it.copy(links = sortedLinks, isLoading = false, error = null) }
                }
        }
    }

    private fun applyAdvancedFilter(links: List<Link>, filter: AdvancedFilter): List<Link> {
        if (!filter.isActive) return links

        return links.filter { link ->
            // Date range filter
            val passesDateFilter = when (filter.dateRange) {
                DateRangeFilter.ALL_TIME -> true
                DateRangeFilter.TODAY -> {
                    val startOfDay = getStartOfDay(0)
                    link.createdAt >= startOfDay
                }
                DateRangeFilter.LAST_7_DAYS -> {
                    val startTime = getStartOfDay(7)
                    link.createdAt >= startTime
                }
                DateRangeFilter.LAST_30_DAYS -> {
                    val startTime = getStartOfDay(30)
                    link.createdAt >= startTime
                }
                DateRangeFilter.LAST_90_DAYS -> {
                    val startTime = getStartOfDay(90)
                    link.createdAt >= startTime
                }
                DateRangeFilter.THIS_YEAR -> {
                    val startOfYear = getStartOfYear()
                    link.createdAt >= startOfYear
                }
            }

            // Domain filter
            val passesDomainFilter = if (filter.domains.isEmpty()) {
                true
            } else {
                val linkDomain = extractDomain(link.url)
                filter.domains.contains(linkDomain)
            }

            // Collection filter
            val passesCollectionFilter = if (filter.collectionIds.isEmpty()) {
                true
            } else {
                link.collectionId != null && filter.collectionIds.contains(link.collectionId)
            }

            // Note filter
            val passesNoteFilter = when (filter.hasNote) {
                null -> true
                true -> !link.note.isNullOrBlank()
                false -> link.note.isNullOrBlank()
            }

            // Preview filter
            val passesPreviewFilter = when (filter.hasPreview) {
                null -> true
                true -> link.previewImagePath != null || link.previewUrl != null
                false -> link.previewImagePath == null && link.previewUrl == null
            }

            passesDateFilter && passesDomainFilter && passesCollectionFilter && passesNoteFilter && passesPreviewFilter
        }
    }

    private fun getStartOfDay(daysAgo: Int): Long {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -daysAgo)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    private fun getStartOfYear(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    private fun extractDomain(url: String): String {
        return try {
            val uri = Uri.parse(url)
            uri.host?.removePrefix("www.") ?: url
        } catch (e: Exception) {
            url
        }
    }

    private fun loadFilterOptions() {
        // Load available domains
        viewModelScope.launch {
            try {
                val urls = linkRepository.getAllActiveUrls()
                val domainCounts = urls
                    .map { extractDomain(it) }
                    .groupingBy { it }
                    .eachCount()
                    .entries
                    .sortedByDescending { it.value }
                    .map { DomainInfo(it.key, it.value) }
                _state.update { it.copy(availableDomains = domainCounts) }
            } catch (e: CancellationException) {
                throw e // Don't catch cancellation - it's normal coroutine behavior
            } catch (e: Exception) {
                Timber.e(e, "Failed to load domain options")
            }
        }

        // Load available collections
        viewModelScope.launch {
            collectionRepository.getCollectionsWithLinkCount()
                .catch { e ->
                    if (e !is CancellationException) {
                        Timber.e(e, "Failed to load collection options")
                    }
                }
                .collect { collectionsWithCount ->
                    val collectionOptions = collectionsWithCount.map { (collection, count) ->
                        CollectionFilterInfo(
                            id = collection.id,
                            name = collection.name,
                            count = count
                        )
                    }
                    _state.update { it.copy(availableCollections = collectionOptions) }
                }
        }
    }

    private fun applySorting() {
        val currentLinks = _state.value.links
        val sortedLinks = sortLinks(currentLinks, _state.value.sortType, _state.value.filterType)
        _state.update { it.copy(links = sortedLinks) }
    }

    private fun sortLinks(links: List<Link>, sortType: SortType, filterType: FilterType): List<Link> {
        // Apply the selected sort to all links (use updatedAt for date sorting)
        return when (sortType) {
            SortType.DATE_DESC -> links.sortedByDescending { it.updatedAt }
            SortType.DATE_ASC -> links.sortedBy { it.updatedAt }
            SortType.NAME_ASC -> links.sortedBy { it.title.lowercase() }
            SortType.NAME_DESC -> links.sortedByDescending { it.title.lowercase() }
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
    }

    private fun toggleFavorite(linkId: String, isFavorite: Boolean, silent: Boolean = false) {
        viewModelScope.launch {
            when (val result = toggleFavoriteUseCase(linkId, isFavorite)) {
                is Result.Success -> {
                    Timber.d("Toggled favorite for link: $linkId")
                    // Only emit snackbar event if not silent (not an undo action)
                    if (!silent) {
                        _uiEvents.emit(HomeUiEvent.ShowFavoriteToggled(
                            linkId = linkId,
                            isFavorite = isFavorite
                        ))
                    }
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
                    // Emit UI event for undo functionality
                    _uiEvents.emit(HomeUiEvent.ShowLinkTrashed(linkId))
                }
                is Result.Error -> {
                    val errorMessage = ErrorHandler.getLinkErrorMessage(result.exception, LinkOperation.DELETE)
                    _uiEvents.emit(HomeUiEvent.ShowError(errorMessage))
                }
                is Result.Loading -> { /* No-op */ }
            }
        }
    }

    private fun restoreLink(linkId: String) {
        viewModelScope.launch {
            when (val result = restoreLinkUseCase(linkId)) {
                is Result.Success -> {
                    Timber.d("Restored link: $linkId")
                }
                is Result.Error -> {
                    val errorMessage = ErrorHandler.getLinkErrorMessage(result.exception, LinkOperation.RESTORE)
                    _uiEvents.emit(HomeUiEvent.ShowError(errorMessage))
                }
                is Result.Loading -> { /* No-op */ }
            }
        }
    }

    // ============ BULK OPERATIONS ============

    private fun bulkDelete() {
        val selectedIds = _state.value.selectedLinkIds.toList()
        if (selectedIds.isEmpty()) return

        viewModelScope.launch {
            var successCount = 0
            selectedIds.forEach { linkId ->
                when (deleteLinkUseCase(linkId, softDelete = true)) {
                    is Result.Success -> successCount++
                    else -> { /* continue with others */ }
                }
            }
            _state.update { it.copy(isSelectionMode = false, selectedLinkIds = emptySet()) }
            _uiEvents.emit(HomeUiEvent.ShowBulkOperationResult("$successCount links moved to trash"))
        }
    }

    private fun bulkFavorite(favorite: Boolean) {
        val selectedIds = _state.value.selectedLinkIds.toList()
        if (selectedIds.isEmpty()) return

        viewModelScope.launch {
            var successCount = 0
            selectedIds.forEach { linkId ->
                when (toggleFavoriteUseCase(linkId, favorite)) {
                    is Result.Success -> successCount++
                    else -> { /* continue with others */ }
                }
            }
            _state.update { it.copy(isSelectionMode = false, selectedLinkIds = emptySet()) }
            val action = if (favorite) "added to favorites" else "removed from favorites"
            _uiEvents.emit(HomeUiEvent.ShowBulkOperationResult("$successCount links $action"))
        }
    }

    private fun bulkMoveToCollection(collectionId: String?) {
        val selectedIds = _state.value.selectedLinkIds.toList()
        if (selectedIds.isEmpty()) return

        viewModelScope.launch {
            var successCount = 0
            selectedIds.forEach { linkId ->
                val link = linkRepository.getLinkByIdOnce(linkId)
                if (link != null) {
                    val updatedLink = link.copy(collectionId = collectionId)
                    when (linkRepository.updateLink(updatedLink)) {
                        is Result.Success -> successCount++
                        else -> { /* continue with others */ }
                    }
                }
            }
            _state.update {
                it.copy(
                    isSelectionMode = false,
                    selectedLinkIds = emptySet(),
                    showBulkMoveSheet = false
                )
            }
            val action = if (collectionId != null) "moved to collection" else "removed from collection"
            _uiEvents.emit(HomeUiEvent.ShowBulkOperationResult("$successCount links $action"))
        }
    }
}

sealed class HomeUiEvent {
    data class ShowError(val message: String) : HomeUiEvent()
    data class ShowFavoriteToggled(val linkId: String, val isFavorite: Boolean) : HomeUiEvent()
    data class ShowLinkTrashed(val linkId: String) : HomeUiEvent()
    data class ShowBulkOperationResult(val message: String) : HomeUiEvent()
}

sealed class HomeEvent {
    data class OnFilterTypeChange(val filterType: FilterType) : HomeEvent()
    data class OnSortTypeChange(val sortType: SortType) : HomeEvent()
    data class OnViewModeChange(val viewMode: ViewMode) : HomeEvent()
    data class OnToggleFavorite(val linkId: String, val isFavorite: Boolean, val silent: Boolean = false) : HomeEvent()
    data class OnDeleteLink(val linkId: String) : HomeEvent()
    data class OnRestoreLink(val linkId: String) : HomeEvent()
    data object OnRefresh : HomeEvent()
    data class OnClipboardUrlDetected(val url: String) : HomeEvent()
    data object OnDismissClipboardPrompt : HomeEvent()
    // Advanced filter events
    data object OnShowAdvancedFilterSheet : HomeEvent()
    data object OnDismissAdvancedFilterSheet : HomeEvent()
    data class OnApplyAdvancedFilter(val filter: AdvancedFilter) : HomeEvent()
    data object OnClearAdvancedFilter : HomeEvent()
    // Bulk selection events
    data object OnEnterSelectionMode : HomeEvent()
    data object OnExitSelectionMode : HomeEvent()
    data class OnToggleLinkSelection(val linkId: String) : HomeEvent()
    data object OnSelectAll : HomeEvent()
    data object OnDeselectAll : HomeEvent()
    data object OnBulkDelete : HomeEvent()
    data object OnBulkFavorite : HomeEvent()
    data object OnBulkUnfavorite : HomeEvent()
    data object OnShowBulkMoveSheet : HomeEvent()
    data object OnDismissBulkMoveSheet : HomeEvent()
    data class OnBulkMoveToCollection(val collectionId: String?) : HomeEvent()
}
