package com.rejowan.linky.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Database Migrations
 *
 * This file contains all migrations for the Linky database.
 * Each migration should be documented with:
 * - Version range (e.g., 1 to 2)
 * - Date of migration
 * - Description of changes
 * - Author
 *
 * Current database version: 1 (Initial release)
 *
 * Example migration for future use:
 * ```
 * val MIGRATION_1_2 = object : Migration(1, 2) {
 *     override fun migrate(db: SupportSQLiteDatabase) {
 *         db.execSQL("ALTER TABLE links ADD COLUMN newColumn TEXT DEFAULT NULL")
 *     }
 * }
 * ```
 */

// Placeholder for future migrations
// Add migrations here as the database schema evolves

/**
 * List of all migrations to be applied
 * Add new migrations to this list as they are created
 */
val ALL_MIGRATIONS: Array<Migration> = arrayOf(
    // Add migrations here, e.g.:
    // MIGRATION_1_2,
    // MIGRATION_2_3,
)
