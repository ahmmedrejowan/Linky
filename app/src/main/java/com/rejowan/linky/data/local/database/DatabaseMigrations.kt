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
 */

/**
 * Migration 1 to 2
 * Date: October 19, 2025
 * Changes: Added description field to links table
 * Author: Claude Code
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add description column to links table
        db.execSQL("ALTER TABLE links ADD COLUMN description TEXT DEFAULT NULL")
    }
}
