package com.rejowan.linky.di

import androidx.room.Room
import com.rejowan.linky.data.local.database.AppDatabase
import com.rejowan.linky.data.local.database.MIGRATION_1_2
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigration(false)
            .build()
    }

    single { get<AppDatabase>().linkDao() }
    single { get<AppDatabase>().folderDao() }
    single { get<AppDatabase>().snapshotDao() }
    single { get<AppDatabase>().configDao() }
}
