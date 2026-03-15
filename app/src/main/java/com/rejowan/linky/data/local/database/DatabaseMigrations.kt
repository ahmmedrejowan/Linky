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
 * List of all migrations to be applied
 * Add new migrations to this list as they are created
 */
val ALL_MIGRATIONS: Array<Migration> = arrayOf(
    MIGRATION_1_2
)
