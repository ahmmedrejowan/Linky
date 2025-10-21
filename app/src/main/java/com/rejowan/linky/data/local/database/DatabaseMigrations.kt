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
 * Example:
 * ```
 * import androidx.room.migration.Migration
 * import androidx.sqlite.db.SupportSQLiteDatabase
 *
 * val MIGRATION_1_2 = object : Migration(1, 2) {
 *     override fun migrate(db: SupportSQLiteDatabase) {
 *         db.execSQL("ALTER TABLE links ADD COLUMN newColumn TEXT DEFAULT NULL")
 *     }
 * }
 * ```
 */

/**
 * Migration from version 4 to 5
 * Date: 2025-01-22
 * Description: Add hideFromHome column to links table
 * - hideFromHome: Boolean field to hide links from home screen when in collections
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE links ADD COLUMN hideFromHome INTEGER NOT NULL DEFAULT 0")
    }
}
