package com.rejowan.linky.di

import com.rejowan.linky.presentation.feature.addlink.AddEditLinkViewModel
import com.rejowan.linky.presentation.feature.collections.CollectionsViewModel
import com.rejowan.linky.presentation.feature.home.HomeViewModel
import com.rejowan.linky.presentation.feature.linkdetail.LinkDetailViewModel
import com.rejowan.linky.presentation.settings.SettingsViewModel
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
            getAllFoldersUseCase = get(),
            linkPreviewFetcher = get(),
            fileStorageManager = get()
        )
    }

    viewModel {
        LinkDetailViewModel(
            savedStateHandle = get(),
            getLinkByIdUseCase = get(),
            getSnapshotsForLinkUseCase = get(),
            toggleFavoriteUseCase = get(),
            toggleArchiveUseCase = get(),
            deleteLinkUseCase = get()
        )
    }

    viewModel {
        CollectionsViewModel(
            getAllFoldersUseCase = get(),
            saveFolderUseCase = get(),
            deleteFolderUseCase = get()
        )
    }

    viewModel {
        SettingsViewModel(
            linkRepository = get(),
            folderRepository = get(),
            snapshotRepository = get(),
            themePreferences = get(),
            fileStorageManager = get()
        )
    }
}
