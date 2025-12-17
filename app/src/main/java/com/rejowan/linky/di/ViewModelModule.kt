package com.rejowan.linky.di

import com.rejowan.linky.presentation.feature.addlink.AddEditLinkViewModel
import com.rejowan.linky.presentation.feature.batchimport.BatchImportViewModel
import com.rejowan.linky.presentation.feature.collectiondetail.CollectionDetailViewModel
import com.rejowan.linky.presentation.feature.collections.CollectionsViewModel
import com.rejowan.linky.presentation.feature.home.HomeViewModel
import com.rejowan.linky.presentation.feature.linkdetail.LinkDetailViewModel
import com.rejowan.linky.presentation.feature.search.SearchViewModel
import com.rejowan.linky.presentation.feature.settings.SettingsViewModel
import com.rejowan.linky.presentation.feature.snapshotviewer.SnapshotViewerViewModel
import com.rejowan.linky.presentation.feature.trash.TrashViewModel
import com.rejowan.linky.presentation.feature.settings.duplicates.DuplicateDetectionViewModel
import com.rejowan.linky.presentation.feature.settings.healthcheck.LinkHealthCheckViewModel
import com.rejowan.linky.presentation.feature.vault.VaultSetupViewModel
import com.rejowan.linky.presentation.feature.vault.VaultUnlockViewModel
import com.rejowan.linky.presentation.feature.vault.VaultViewModel
import com.rejowan.linky.presentation.feature.vault.VaultSettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel {
        HomeViewModel(
            getAllLinksUseCase = get(),
            getFavoriteLinksUseCase = get(),
            getArchivedLinksUseCase = get(),
            getTrashedLinksUseCase = get(),
            toggleFavoriteUseCase = get(),
            toggleArchiveUseCase = get(),
            deleteLinkUseCase = get(),
            restoreLinkUseCase = get(),
            linkRepository = get(),
            collectionRepository = get(),
            tagRepository = get()
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
            fileStorageManager = get(),
            preferencesManager = get(),
            getAllTagsUseCase = get(),
            getTagsForLinkUseCase = get(),
            saveTagUseCase = get(),
            setTagsForLinkUseCase = get()
        )
    }

    viewModel {
        LinkDetailViewModel(
            savedStateHandle = get(),
            getLinkByIdUseCase = get(),
            getSnapshotsForLinkUseCase = get(),
            getCollectionByIdUseCase = get(),
            captureSnapshotUseCase = get(),
            deleteSnapshotUseCase = get(),
            toggleFavoriteUseCase = get(),
            toggleArchiveUseCase = get(),
            deleteLinkUseCase = get(),
            restoreLinkUseCase = get()
        )
    }

    viewModel {
        CollectionsViewModel(
            getCollectionsWithLinkCountUseCase = get(),
            saveCollectionUseCase = get(),
            updateCollectionUseCase = get(),
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
            deleteLinkUseCase = get(),
            toggleArchiveUseCase = get(),
            restoreLinkUseCase = get()
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

    viewModel {
        TrashViewModel(
            getTrashedLinksUseCase = get(),
            restoreLinkUseCase = get(),
            deleteLinkUseCase = get()
        )
    }

    viewModel {
        SearchViewModel(
            searchLinksUseCase = get(),
            toggleFavoriteUseCase = get()
        )
    }

    viewModel {
        BatchImportViewModel(
            saveLinkUseCase = get(),
            checkUrlExistsUseCase = get(),
            linkPreviewFetcher = get(),
            batchSaveLinksUseCase = get(),
            getAllCollectionsUseCase = get(),
            saveCollectionUseCase = get()
        )
    }

    viewModel {
        com.rejowan.linky.presentation.feature.settings.tags.TagManagementViewModel(
            getTagsWithLinkCountUseCase = get(),
            saveTagUseCase = get(),
            updateTagUseCase = get(),
            deleteTagUseCase = get()
        )
    }

    // Vault ViewModels
    viewModel {
        VaultSetupViewModel(
            setupVaultPinUseCase = get()
        )
    }

    viewModel {
        VaultUnlockViewModel(
            unlockVaultUseCase = get(),
            vaultRepository = get()
        )
    }

    viewModel {
        VaultViewModel(
            getAllVaultLinksUseCase = get(),
            addVaultLinkUseCase = get(),
            deleteVaultLinkUseCase = get(),
            lockVaultUseCase = get(),
            vaultRepository = get()
        )
    }

    viewModel {
        VaultSettingsViewModel(
            vaultRepository = get(),
            changeVaultPinUseCase = get(),
            clearVaultUseCase = get()
        )
    }

    viewModel {
        DuplicateDetectionViewModel(
            linkRepository = get(),
            deleteLinkUseCase = get()
        )
    }

    viewModel {
        LinkHealthCheckViewModel(
            linkRepository = get(),
            deleteLinkUseCase = get()
        )
    }
}
