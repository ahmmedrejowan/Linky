package com.rejowan.linky.presentation.feature.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rejowan.linky.data.local.preferences.ThemePreferences
import com.rejowan.linky.domain.model.VaultLink
import com.rejowan.linky.domain.repository.VaultRepository
import com.rejowan.linky.domain.usecase.vault.AddVaultLinkUseCase
import com.rejowan.linky.domain.usecase.vault.DeleteVaultLinkUseCase
import com.rejowan.linky.domain.usecase.vault.GetAllVaultLinksUseCase
import com.rejowan.linky.domain.usecase.vault.LockVaultUseCase
import com.rejowan.linky.domain.usecase.vault.UpdateVaultLinkUseCase
import com.rejowan.linky.presentation.feature.home.ViewMode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Sort types for vault links
 */
enum class VaultSortType(val displayName: String) {
    DATE_DESC("Newest First"),
    DATE_ASC("Oldest First"),
    NAME_ASC("A → Z"),
    NAME_DESC("Z → A"),
    FAVORITES_FIRST("Favorites First")
}

/**
 * Filter types for vault links
 */
enum class VaultFilterType {
    ALL,
    FAVORITES
}

data class VaultState(
    val vaultLinks: List<VaultLink> = emptyList(),
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false,
    val linkToDelete: VaultLink? = null,
    val error: String? = null,
    // Filter type
    val filterType: VaultFilterType = VaultFilterType.ALL,
    // Sorting and view mode
    val sortType: VaultSortType = VaultSortType.DATE_DESC,
    val viewMode: ViewMode = ViewMode.LIST,
    // Selected link for info bottom sheet
    val selectedLinkForInfo: VaultLink? = null,
    // Total counts for display
    val totalLinkCount: Int = 0,
    val favoriteLinkCount: Int = 0
)

sealed class VaultEvent {
    data object OnLock : VaultEvent()
    data object OnShowAddDialog : VaultEvent()
    data object OnDismissAddDialog : VaultEvent()
    data class OnAddLink(val url: String, val title: String, val description: String?, val notes: String?) : VaultEvent()
    data class OnShowDeleteConfirm(val link: VaultLink) : VaultEvent()
    data object OnDismissDeleteConfirm : VaultEvent()
    data object OnConfirmDelete : VaultEvent()
    // Filter type
    data class OnFilterTypeChange(val filterType: VaultFilterType) : VaultEvent()
    // Sorting and view mode
    data class OnSortTypeChange(val sortType: VaultSortType) : VaultEvent()
    data class OnViewModeChange(val viewMode: ViewMode) : VaultEvent()
    // Favorite toggle
    data class OnToggleFavorite(val linkId: String) : VaultEvent()
    // Link info bottom sheet
    data class OnShowLinkInfo(val link: VaultLink) : VaultEvent()
    data object OnDismissLinkInfo : VaultEvent()
}

sealed class VaultUiEvent {
    data object Locked : VaultUiEvent()
    data class ShowMessage(val message: String) : VaultUiEvent()
}

class VaultViewModel(
    private val getAllVaultLinksUseCase: GetAllVaultLinksUseCase,
    private val addVaultLinkUseCase: AddVaultLinkUseCase,
    private val deleteVaultLinkUseCase: DeleteVaultLinkUseCase,
    private val updateVaultLinkUseCase: UpdateVaultLinkUseCase,
    private val lockVaultUseCase: LockVaultUseCase,
    private val vaultRepository: VaultRepository,
    private val themePreferences: ThemePreferences
) : ViewModel() {

    private val _state = MutableStateFlow(VaultState())
    val state: StateFlow<VaultState> = _state.asStateFlow()

    private val _uiEvents = MutableSharedFlow<VaultUiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    // Store unsorted links for re-sorting
    private var allLinks: List<VaultLink> = emptyList()

    init {
        loadVaultLinks()
        observeUnlockState()
        observeVaultViewMode()
    }

    private fun loadVaultLinks() {
        viewModelScope.launch {
            getAllVaultLinksUseCase().collect { links ->
                allLinks = links
                applyFilterAndSort()
            }
        }
    }

    private fun applyFilterAndSort() {
        val filteredLinks = filterLinks(allLinks, _state.value.filterType)
        val sortedLinks = sortLinks(filteredLinks, _state.value.sortType)
        _state.update {
            it.copy(
                vaultLinks = sortedLinks,
                isLoading = false,
                totalLinkCount = allLinks.size,
                favoriteLinkCount = allLinks.count { link -> link.isFavorite }
            )
        }
    }

    private fun filterLinks(links: List<VaultLink>, filterType: VaultFilterType): List<VaultLink> {
        return when (filterType) {
            VaultFilterType.ALL -> links
            VaultFilterType.FAVORITES -> links.filter { it.isFavorite }
        }
    }

    private fun observeUnlockState() {
        viewModelScope.launch {
            vaultRepository.isUnlocked.collect { isUnlocked ->
                if (!isUnlocked) {
                    _uiEvents.emit(VaultUiEvent.Locked)
                }
            }
        }
    }

    private fun observeVaultViewMode() {
        viewModelScope.launch {
            themePreferences.getLinkViewMode()
                .catch { e ->
                    Timber.e(e, "Failed to observe vault view mode")
                }
                .collect { viewModeName ->
                    val viewMode = try {
                        ViewMode.valueOf(viewModeName)
                    } catch (e: IllegalArgumentException) {
                        ViewMode.LIST
                    }
                    _state.update { it.copy(viewMode = viewMode) }
                }
        }
    }

    fun onEvent(event: VaultEvent) {
        when (event) {
            VaultEvent.OnLock -> lock()
            VaultEvent.OnShowAddDialog -> _state.update { it.copy(showAddDialog = true) }
            VaultEvent.OnDismissAddDialog -> _state.update { it.copy(showAddDialog = false) }
            is VaultEvent.OnAddLink -> addLink(event.url, event.title, event.description, event.notes)
            is VaultEvent.OnShowDeleteConfirm -> _state.update {
                it.copy(showDeleteConfirmDialog = true, linkToDelete = event.link)
            }
            VaultEvent.OnDismissDeleteConfirm -> _state.update {
                it.copy(showDeleteConfirmDialog = false, linkToDelete = null)
            }
            VaultEvent.OnConfirmDelete -> deleteLink()
            is VaultEvent.OnFilterTypeChange -> {
                _state.update { it.copy(filterType = event.filterType) }
                applyFilterAndSort()
            }
            is VaultEvent.OnSortTypeChange -> {
                _state.update { it.copy(sortType = event.sortType) }
                applyFilterAndSort()
            }
            is VaultEvent.OnViewModeChange -> {
                _state.update { it.copy(viewMode = event.viewMode) }
                viewModelScope.launch {
                    themePreferences.setLinkViewMode(event.viewMode.name)
                }
            }
            is VaultEvent.OnToggleFavorite -> toggleFavorite(event.linkId)
            is VaultEvent.OnShowLinkInfo -> _state.update { it.copy(selectedLinkForInfo = event.link) }
            VaultEvent.OnDismissLinkInfo -> _state.update { it.copy(selectedLinkForInfo = null) }
        }
    }

    private fun lock() {
        lockVaultUseCase()
    }

    private fun addLink(url: String, title: String, description: String?, notes: String?) {
        viewModelScope.launch {
            val vaultLink = VaultLink(
                url = url,
                title = title.ifBlank { url },
                description = description,
                notes = notes
            )

            addVaultLinkUseCase(vaultLink).fold(
                onSuccess = {
                    _state.update { it.copy(showAddDialog = false) }
                    _uiEvents.emit(VaultUiEvent.ShowMessage("Link added to vault"))
                },
                onFailure = { error ->
                    _uiEvents.emit(VaultUiEvent.ShowMessage(error.message ?: "Failed to add link"))
                }
            )
        }
    }

    private fun deleteLink() {
        val link = _state.value.linkToDelete ?: return

        viewModelScope.launch {
            deleteVaultLinkUseCase(link.id).fold(
                onSuccess = {
                    _state.update { it.copy(showDeleteConfirmDialog = false, linkToDelete = null) }
                    _uiEvents.emit(VaultUiEvent.ShowMessage("Link deleted"))
                },
                onFailure = { error ->
                    _uiEvents.emit(VaultUiEvent.ShowMessage(error.message ?: "Failed to delete link"))
                }
            )
        }
    }

    private fun toggleFavorite(linkId: String) {
        val link = allLinks.find { it.id == linkId } ?: return
        val updatedLink = link.copy(
            isFavorite = !link.isFavorite,
            updatedAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            updateVaultLinkUseCase(updatedLink).fold(
                onSuccess = {
                    val message = if (updatedLink.isFavorite) "Added to favorites" else "Removed from favorites"
                    _uiEvents.emit(VaultUiEvent.ShowMessage(message))
                },
                onFailure = { error ->
                    _uiEvents.emit(VaultUiEvent.ShowMessage(error.message ?: "Failed to update"))
                }
            )
        }
    }

    private fun applySorting() {
        applyFilterAndSort()
        Timber.d("Vault links sorted by ${_state.value.sortType.displayName}")
    }

    private fun sortLinks(links: List<VaultLink>, sortType: VaultSortType): List<VaultLink> {
        return when (sortType) {
            VaultSortType.DATE_DESC -> links.sortedByDescending { it.createdAt }
            VaultSortType.DATE_ASC -> links.sortedBy { it.createdAt }
            VaultSortType.NAME_ASC -> links.sortedBy { it.title.lowercase() }
            VaultSortType.NAME_DESC -> links.sortedByDescending { it.title.lowercase() }
            VaultSortType.FAVORITES_FIRST -> links.sortedWith(
                compareByDescending<VaultLink> { it.isFavorite }
                    .thenByDescending { it.createdAt }
            )
        }
    }
}
