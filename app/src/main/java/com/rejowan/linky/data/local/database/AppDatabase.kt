package com.rejowan.linky.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.rejowan.linky.data.local.database.dao.ConfigDao
import com.rejowan.linky.data.local.database.dao.CollectionDao
import com.rejowan.linky.data.local.database.dao.LinkDao
import com.rejowan.linky.data.local.database.dao.SnapshotDao
import com.rejowan.linky.data.local.database.entity.ConfigEntity
import com.rejowan.linky.data.local.database.entity.CollectionEntity
import com.rejowan.linky.data.local.database.entity.LinkEntity
import com.rejowan.linky.data.local.database.entity.SnapshotEntity

@Database(
    entities = [
        LinkEntity::class,
        CollectionEntity::class,
        SnapshotEntity::class,
        ConfigEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun linkDao(): LinkDao
    abstract fun collectionDao(): CollectionDao
    abstract fun snapshotDao(): SnapshotDao
    abstract fun configDao(): ConfigDao

    companion object {
        const val DATABASE_NAME = "linky_database"
    }
}
