package com.rejowan.linky.di

import com.rejowan.linky.domain.usecase.collection.DeleteCollectionUseCase
import com.rejowan.linky.domain.usecase.collection.GetAllCollectionsUseCase
import com.rejowan.linky.domain.usecase.collection.GetCollectionByIdUseCase
import com.rejowan.linky.domain.usecase.collection.GetCollectionsWithLinkCountUseCase
import com.rejowan.linky.domain.usecase.collection.SaveCollectionUseCase
import com.rejowan.linky.domain.usecase.collection.SearchCollectionsUseCase
import com.rejowan.linky.domain.usecase.collection.UpdateCollectionUseCase
import com.rejowan.linky.domain.usecase.link.BatchSaveLinksUseCase
import com.rejowan.linky.domain.usecase.link.CheckUrlExistsUseCase
import com.rejowan.linky.domain.usecase.link.DeleteLinkUseCase
import com.rejowan.linky.domain.usecase.link.GetAllLinksUseCase
import com.rejowan.linky.domain.usecase.link.GetArchivedLinksUseCase
import com.rejowan.linky.domain.usecase.link.GetFavoriteLinksUseCase
import com.rejowan.linky.domain.usecase.link.GetLinkByIdUseCase
import com.rejowan.linky.domain.usecase.link.GetLinksByCollectionUseCase
import com.rejowan.linky.domain.usecase.link.GetTrashedLinksUseCase
import com.rejowan.linky.domain.usecase.link.RestoreLinkUseCase
import com.rejowan.linky.domain.usecase.link.SaveLinkUseCase
import com.rejowan.linky.domain.usecase.link.SearchLinksUseCase
import com.rejowan.linky.domain.usecase.link.ToggleArchiveUseCase
import com.rejowan.linky.domain.usecase.link.ToggleFavoriteUseCase
import com.rejowan.linky.domain.usecase.link.UpdateLinkUseCase
import com.rejowan.linky.domain.usecase.snapshot.CaptureSnapshotUseCase
import com.rejowan.linky.domain.usecase.snapshot.DeleteSnapshotUseCase
import com.rejowan.linky.domain.usecase.snapshot.GetSnapshotByIdUseCase
import com.rejowan.linky.domain.usecase.snapshot.GetSnapshotsForLinkUseCase
import com.rejowan.linky.domain.usecase.snapshot.SaveSnapshotUseCase
import com.rejowan.linky.domain.usecase.backup.ExportDataUseCase
import com.rejowan.linky.domain.usecase.backup.ImportDataUseCase
import org.koin.dsl.module

val useCaseModule = module {
    // Link use cases
    factory { GetAllLinksUseCase(get()) }
    factory { GetLinkByIdUseCase(get()) }
    factory { GetLinksByCollectionUseCase(get()) }
    factory { GetFavoriteLinksUseCase(get()) }
    factory { GetArchivedLinksUseCase(get()) }
    factory { GetTrashedLinksUseCase(get()) }
    factory { SaveLinkUseCase(get()) }
    factory { UpdateLinkUseCase(get()) }
    factory { DeleteLinkUseCase(get()) }
    factory { RestoreLinkUseCase(get()) }
    factory { SearchLinksUseCase(get()) }
    factory { ToggleFavoriteUseCase(get()) }
    factory { ToggleArchiveUseCase(get()) }
    factory { CheckUrlExistsUseCase(get()) }
    factory { BatchSaveLinksUseCase(get()) }

    // Collection use cases
    factory { GetAllCollectionsUseCase(get()) }
    factory { GetCollectionByIdUseCase(get()) }
    factory { GetCollectionsWithLinkCountUseCase(get()) }
    factory { SaveCollectionUseCase(get()) }
    factory { UpdateCollectionUseCase(get()) }
    factory { DeleteCollectionUseCase(get()) }
    factory { SearchCollectionsUseCase(get()) }

    // Snapshot use cases
    factory { GetSnapshotsForLinkUseCase(get()) }
    factory { GetSnapshotByIdUseCase(get()) }
    factory { SaveSnapshotUseCase(get()) }
    factory { DeleteSnapshotUseCase(get(), get()) }
    factory { CaptureSnapshotUseCase(get(), get(), get()) }

    // Backup use cases
    factory { ExportDataUseCase(get()) }
    factory { ImportDataUseCase(get()) }
}
