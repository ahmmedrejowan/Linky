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
 * Current database version: 2
 */

/**
 * Migration 1 to 2: Remove Tags feature
 * Date: March 2026
 * Description: Drops tags and link_tag_cross_ref tables as tags feature is removed
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Drop the link-tag relationship table first (foreign key constraint)
        db.execSQL("DROP TABLE IF EXISTS link_tag_cross_ref")
        // Drop the tags table
        db.execSQL("DROP TABLE IF EXISTS tags")
    }
}

/**
 * Migration 2 to 3: Remove isFavorite from collections
 * Date: March 2026
 * Description: Removes isFavorite column from collections table as favorite functionality
 *              has been removed from collections
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // SQLite doesn't support DROP COLUMN directly, so we need to recreate the table
        // 1. Create a new table without isFavorite column
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS collections_new (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                color TEXT,
                icon TEXT,
                sortOrder INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                syncToRemote INTEGER NOT NULL DEFAULT 0,
                userId TEXT
            )
        """.trimIndent())

        // 2. Copy data from old table to new table (excluding isFavorite)
        db.execSQL("""
            INSERT INTO collections_new (id, name, color, icon, sortOrder, createdAt, updatedAt, syncToRemote, userId)
            SELECT id, name, color, icon, sortOrder, createdAt, updatedAt, syncToRemote, userId
            FROM collections
        """.trimIndent())

        // 3. Drop the old table
        db.execSQL("DROP TABLE collections")

        // 4. Rename new table to original name
        db.execSQL("ALTER TABLE collections_new RENAME TO collections")

        // 5. Recreate indexes
        db.execSQL("CREATE INDEX IF NOT EXISTS index_collections_sortOrder ON collections(sortOrder)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_collections_updatedAt ON collections(updatedAt)")
    }
}

/**
 * Migration 3 to 4: Add pending_vault_links table
 * Date: March 2026
 * Description: Adds staging table for links queued to be moved to vault.
 *              Links are stored unencrypted here until vault is unlocked.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS pending_vault_links (
                id TEXT NOT NULL PRIMARY KEY,
                url TEXT NOT NULL,
                title TEXT NOT NULL,
                description TEXT,
                notes TEXT,
                createdAt INTEGER NOT NULL,
                queuedAt INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

/**
 * List of all migrations to be applied
 * Add new migrations to this list as they are created
 */
val ALL_MIGRATIONS: Array<Migration> = arrayOf(
    MIGRATION_1_2,
    MIGRATION_2_3,
    MIGRATION_3_4
)
