package com.rejowan.linky.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rejowan.linky.domain.usecase.link.DeleteLinkUseCase
import com.rejowan.linky.domain.usecase.link.GetAllLinksUseCase
import com.rejowan.linky.domain.usecase.link.GetArchivedLinksUseCase
import com.rejowan.linky.domain.usecase.link.GetFavoriteLinksUseCase
import com.rejowan.linky.domain.usecase.link.GetTrashedLinksUseCase
import com.rejowan.linky.domain.usecase.link.SearchLinksUseCase
import com.rejowan.linky.domain.usecase.link.ToggleFavoriteUseCase
import com.rejowan.linky.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class HomeViewModel(
    private val getAllLinksUseCase: GetAllLinksUseCase,
    private val getFavoriteLinksUseCase: GetFavoriteLinksUseCase,
    private val getArchivedLinksUseCase: GetArchivedLinksUseCase,
    private val getTrashedLinksUseCase: GetTrashedLinksUseCase,
    private val searchLinksUseCase: SearchLinksUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val deleteLinkUseCase: DeleteLinkUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        loadLinks()
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.OnSearchQueryChange -> {
                _state.update { it.copy(searchQuery = event.query) }
                if (event.query.isBlank()) {
                    loadLinks()
                } else {
                    searchLinks(event.query)
                }
            }
            is HomeEvent.OnFilterTypeChange -> {
                _state.update { it.copy(filterType = event.filterType) }
                loadLinks()
            }
            is HomeEvent.OnToggleFavorite -> toggleFavorite(event.linkId, event.isFavorite)
            is HomeEvent.OnDeleteLink -> deleteLink(event.linkId)
            is HomeEvent.OnRefresh -> loadLinks()
        }
    }

    private fun loadLinks() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val flow = when (_state.value.filterType) {
                FilterType.ALL -> getAllLinksUseCase()
                FilterType.FAVORITES -> getFavoriteLinksUseCase()
                FilterType.ARCHIVED -> getArchivedLinksUseCase()
                FilterType.TRASH -> getTrashedLinksUseCase()
            }

            flow.catch { e ->
                    Timber.e(e, "Failed to load links")
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { links ->
                    _state.update { it.copy(links = links, isLoading = false, error = null) }
                }
        }
    }

    private fun searchLinks(query: String) {
        viewModelScope.launch {
            searchLinksUseCase(query)
                .catch { e ->
                    Timber.e(e, "Failed to search links")
                    _state.update { it.copy(error = e.message) }
                }
                .collect { links ->
                    _state.update { it.copy(links = links) }
                }
        }
    }

    private fun toggleFavorite(linkId: String, isFavorite: Boolean) {
        viewModelScope.launch {
            when (val result = toggleFavoriteUseCase(linkId, isFavorite)) {
                is Result.Success -> {
                    Timber.d("Toggled favorite for link: $linkId")
                }
                is Result.Error -> {
                    Timber.e(result.exception, "Failed to toggle favorite")
                    _state.update { it.copy(error = result.exception.message) }
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
                    Timber.e(result.exception, "Failed to delete link")
                    _state.update { it.copy(error = result.exception.message) }
                }
                is Result.Loading -> { /* No-op */ }
            }
        }
    }
}

sealed class HomeEvent {
    data class OnSearchQueryChange(val query: String) : HomeEvent()
    data class OnFilterTypeChange(val filterType: FilterType) : HomeEvent()
    data class OnToggleFavorite(val linkId: String, val isFavorite: Boolean) : HomeEvent()
    data class OnDeleteLink(val linkId: String) : HomeEvent()
    data object OnRefresh : HomeEvent()
}
