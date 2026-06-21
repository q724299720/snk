package com.snk.app.data.local

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FoodRecordDraftEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class SnkDatabase : RoomDatabase() {
    abstract fun foodRecordDraftDao(): FoodRecordDraftDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE food_record_drafts ADD COLUMN is_public INTEGER NOT NULL DEFAULT 0",
                )
            }
        }
    }
}
