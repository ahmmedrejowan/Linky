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

/**
 * Migration from version 5 to 6
 * Date: 2025-12-17
 * Description: Add Tags feature
 * - New tags table for storing tag entities
 * - New link_tags junction table for many-to-many relationship
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create tags table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS tags (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                color TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
        """)

        // Create unique index on tag name
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_tags_name ON tags (name)")

        // Create link_tags junction table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS link_tags (
                linkId TEXT NOT NULL,
                tagId TEXT NOT NULL,
                PRIMARY KEY (linkId, tagId),
                FOREIGN KEY (linkId) REFERENCES links(id) ON DELETE CASCADE,
                FOREIGN KEY (tagId) REFERENCES tags(id) ON DELETE CASCADE
            )
        """)

        // Create indices for junction table
        db.execSQL("CREATE INDEX IF NOT EXISTS index_link_tags_linkId ON link_tags (linkId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_link_tags_tagId ON link_tags (tagId)")
    }
}
