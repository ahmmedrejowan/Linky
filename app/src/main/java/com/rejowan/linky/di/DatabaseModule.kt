package com.rejowan.linky.di

import com.rejowan.linky.data.local.database.AppDatabase
import com.rejowan.linky.data.local.database.LinkyDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single {
        LinkyDatabase.getDatabase(androidContext())
    }

    single { get<AppDatabase>().linkDao() }
    single { get<AppDatabase>().collectionDao() }
    single { get<AppDatabase>().snapshotDao() }
    single { get<AppDatabase>().configDao() }
    single { get<AppDatabase>().vaultLinkDao() }
    single { get<AppDatabase>().pendingVaultLinkDao() }
}
