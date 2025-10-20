package com.rejowan.linky.presentation.feature.collectiondetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rejowan.linky.domain.usecase.collection.GetCollectionByIdUseCase
import com.rejowan.linky.domain.usecase.link.GetLinksByCollectionUseCase
import com.rejowan.linky.util.ErrorHandler
import com.rejowan.linky.util.CollectionOperation
import com.rejowan.linky.util.LinkOperation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class CollectionDetailViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val getCollectionByIdUseCase: GetCollectionByIdUseCase,
    private val getLinksByCollectionUseCase: GetLinksByCollectionUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(CollectionDetailState())
    val state: StateFlow<CollectionDetailState> = _state.asStateFlow()

    init {
        val collectionId = savedStateHandle.get<String>("collectionId")
        Timber.d("CollectionDetailViewModel initialized | CollectionId: $collectionId")

        collectionId?.let {
            loadCollectionDetails(it)
        } ?: run {
            Timber.e("CollectionDetailViewModel: collectionId is null")
            _state.update { it.copy(error = "Collection not found") }
        }
    }

    fun onEvent(event: CollectionDetailEvent) {
        Timber.d("onEvent: $event")
        when (event) {
            is CollectionDetailEvent.OnRefresh -> {
                savedStateHandle.get<String>("collectionId")?.let { collectionId ->
                    loadCollectionDetails(collectionId)
                }
            }
        }
    }

    private fun loadCollectionDetails(collectionId: String) {
        Timber.d("loadCollectionDetails: Loading collection | CollectionId: $collectionId")
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            // Load collection info
            launch {
                getCollectionByIdUseCase(collectionId)
                    .catch { e ->
                        Timber.e(e, "loadCollectionDetails: Failed to load collection - ${e.message}")
                        val errorMessage = ErrorHandler.getCollectionErrorMessage(e, CollectionOperation.LOAD)
                        _state.update { it.copy(error = errorMessage, isLoading = false) }
                    }
                    .collect { collection ->
                        collection?.let {
                            Timber.d("loadCollectionDetails: Collection loaded | Name: ${it.name}")
                            _state.update { state -> state.copy(collection = it) }
                        } ?: run {
                            Timber.w("loadCollectionDetails: Collection not found")
                            _state.update { it.copy(error = "Collection not found", isLoading = false) }
                        }
                    }
            }

            // Load links in this collection
            launch {
                getLinksByCollectionUseCase(collectionId)
                    .catch { e ->
                        Timber.e(e, "loadCollectionDetails: Failed to load links - ${e.message}")
                        val errorMessage = ErrorHandler.getLinkErrorMessage(e, LinkOperation.LOAD_ALL)
                        _state.update { it.copy(error = errorMessage, isLoading = false) }
                    }
                    .collect { links ->
                        Timber.d("loadCollectionDetails: Loaded ${links.size} links")
                        _state.update { it.copy(links = links, isLoading = false) }
                    }
            }
        }
    }
}

sealed class CollectionDetailEvent {
    data object OnRefresh : CollectionDetailEvent()
}
