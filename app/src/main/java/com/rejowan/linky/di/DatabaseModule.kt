package com.rejowan.linky.di

import androidx.room.Room
import com.rejowan.linky.data.local.database.AppDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            // DEV MODE: Allow destructive migration (drops and recreates tables on schema changes)
            // TODO: Before production, add migrations and set fallbackToDestructiveMigration(false)
            .fallbackToDestructiveMigration(true)
            .build()
    }

    single { get<AppDatabase>().linkDao() }
    single { get<AppDatabase>().collectionDao() }
    single { get<AppDatabase>().snapshotDao() }
    single { get<AppDatabase>().configDao() }
}
