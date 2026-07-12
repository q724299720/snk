package com.snk.app.data.local

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FoodRecordDraftEntity::class],
    version = 5,
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
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE food_record_drafts ADD COLUMN client_request_id TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE food_record_drafts SET client_request_id = printf('00000000-0000-0000-0000-%012x', id) WHERE client_request_id = ''")
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE food_record_drafts_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        user_id INTEGER NOT NULL,
                        food_item_id INTEGER,
                        food_name TEXT NOT NULL,
                        category TEXT NOT NULL,
                        subcategory TEXT,
                        brand TEXT,
                        barcode TEXT,
                        rating INTEGER,
                        comment TEXT NOT NULL,
                        source_type TEXT NOT NULL,
                        is_public INTEGER NOT NULL DEFAULT 0,
                        sync_status TEXT NOT NULL,
                        retry_count INTEGER NOT NULL,
                        failure_reason TEXT,
                        failure_message TEXT,
                        remote_record_id INTEGER,
                        remote_record_time TEXT,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        client_request_id TEXT NOT NULL DEFAULT '',
                        local_image_path TEXT
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO food_record_drafts_new (
                        id, user_id, food_item_id, food_name, category, subcategory, brand, barcode,
                        rating, comment, source_type, is_public, sync_status, retry_count,
                        failure_reason, failure_message, remote_record_id, remote_record_time,
                        created_at, updated_at, client_request_id
                    ) SELECT
                        id, user_id, food_item_id, food_name, category, subcategory, brand, barcode,
                        rating, comment, source_type, is_public,
                        CASE WHEN sync_status = 'DRAFT' THEN 'QUEUED' ELSE sync_status END,
                        retry_count, failure_reason, failure_message, remote_record_id, remote_record_time,
                        created_at, updated_at, client_request_id
                    FROM food_record_drafts
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE food_record_drafts")
                db.execSQL("ALTER TABLE food_record_drafts_new RENAME TO food_record_drafts")
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE food_record_drafts ADD COLUMN draft_owner_user_id INTEGER NOT NULL DEFAULT 0",
                )
                // V4 stored the only available identity in user_id. Keep it as a legacy owner
                // until the explicit one-time claim flow assigns the formal account.
                db.execSQL(
                    "UPDATE food_record_drafts SET draft_owner_user_id = user_id WHERE draft_owner_user_id = 0",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_food_record_drafts_draft_owner_user_id_updated_at " +
                        "ON food_record_drafts (draft_owner_user_id, updated_at)",
                )
            }
        }
    }
}
