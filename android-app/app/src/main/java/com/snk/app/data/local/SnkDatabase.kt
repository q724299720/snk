package com.snk.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [FoodRecordDraftEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class SnkDatabase : RoomDatabase() {
    abstract fun foodRecordDraftDao(): FoodRecordDraftDao
}
