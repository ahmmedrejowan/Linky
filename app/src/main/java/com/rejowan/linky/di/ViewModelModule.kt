package com.rejowan.linky.di

import com.rejowan.linky.presentation.feature.addlink.AddEditLinkViewModel
import com.rejowan.linky.presentation.feature.collectiondetail.CollectionDetailViewModel
import com.rejowan.linky.presentation.feature.collections.CollectionsViewModel
import com.rejowan.linky.presentation.feature.home.HomeViewModel
import com.rejowan.linky.presentation.feature.linkdetail.LinkDetailViewModel
import com.rejowan.linky.presentation.feature.settings.SettingsViewModel
import com.rejowan.linky.presentation.feature.snapshotviewer.SnapshotViewerViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel {
        HomeViewModel(
            getAllLinksUseCase = get(),
            getFavoriteLinksUseCase = get(),
            getArchivedLinksUseCase = get(),
            getTrashedLinksUseCase = get(),
            searchLinksUseCase = get(),
            toggleFavoriteUseCase = get(),
            deleteLinkUseCase = get()
        )
    }

    viewModel {
        AddEditLinkViewModel(
            savedStateHandle = get(),
            saveLinkUseCase = get(),
            updateLinkUseCase = get(),
            getLinkByIdUseCase = get(),
            getAllCollectionsUseCase = get(),
            saveCollectionUseCase = get(),
            linkPreviewFetcher = get(),
            fileStorageManager = get()
        )
    }

    viewModel {
        LinkDetailViewModel(
            savedStateHandle = get(),
            getLinkByIdUseCase = get(),
            getSnapshotsForLinkUseCase = get(),
            captureSnapshotUseCase = get(),
            deleteSnapshotUseCase = get(),
            toggleFavoriteUseCase = get(),
            toggleArchiveUseCase = get(),
            deleteLinkUseCase = get()
        )
    }

    viewModel {
        CollectionsViewModel(
            getCollectionsWithLinkCountUseCase = get(),
            saveCollectionUseCase = get(),
            deleteCollectionUseCase = get()
        )
    }

    viewModel {
        CollectionDetailViewModel(
            savedStateHandle = get(),
            getCollectionByIdUseCase = get(),
            getLinksByCollectionUseCase = get(),
            updateCollectionUseCase = get(),
            deleteCollectionUseCase = get(),
            updateLinkUseCase = get(),
            deleteLinkUseCase = get()
        )
    }

    viewModel {
        SettingsViewModel(
            linkRepository = get(),
            collectionRepository = get(),
            snapshotRepository = get(),
            themePreferences = get(),
            fileStorageManager = get()
        )
    }

    viewModel {
        SnapshotViewerViewModel(
            savedStateHandle = get(),
            getSnapshotByIdUseCase = get(),
            deleteSnapshotUseCase = get()
        )
    }
}
