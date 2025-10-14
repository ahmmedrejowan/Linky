package com.rejowan.linky.di

import com.rejowan.linky.presentation.addlink.AddEditLinkViewModel
import com.rejowan.linky.presentation.collections.CollectionsViewModel
import com.rejowan.linky.presentation.home.HomeViewModel
import com.rejowan.linky.presentation.linkdetail.LinkDetailViewModel
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
            getAllFoldersUseCase = get()
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
            themePreferences = get()
        )
    }
}
