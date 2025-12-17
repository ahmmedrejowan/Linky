package com.rejowan.linky.data.local.database

import android.content.Context
import androidx.room.Room

/**
 * Singleton database instance provider for widget access
 * This provides direct database access without Koin DI for use in widgets/receivers
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
            ).build()
            INSTANCE = instance
            instance
        }
    }
}
