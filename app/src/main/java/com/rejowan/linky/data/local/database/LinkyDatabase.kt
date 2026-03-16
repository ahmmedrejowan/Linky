package com.rejowan.linky.data.local.database

import android.content.Context
import androidx.room.Room

/**
 * Singleton database instance provider
 * Used by both Koin DI module and widgets/receivers to ensure single instance
 */
object LinkyDatabase {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                AppDatabase.DATABASE_NAME
            )
                .addMigrations(*ALL_MIGRATIONS)
                .fallbackToDestructiveMigration(true)
                .build()
            INSTANCE = instance
            instance
        }
    }
}
