package com.snk.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class DraftOwnerMigrationTest {
    private val databaseName = "draft-owner-migration-test.db"
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun v4ToV5_keepsDraftAndUsesItsLegacyUserIdAsOwner() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(databaseName)
                .callback(object : SupportSQLiteOpenHelper.Callback(4) {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(
                        db: androidx.sqlite.db.SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int,
                    ) = Unit
                })
                .build(),
        )
        val database = helper.writableDatabase
        database.execSQL(
            """
            CREATE TABLE food_record_drafts (
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
        database.execSQL(
            """
            INSERT INTO food_record_drafts (
                id, user_id, food_name, category, rating, comment, source_type,
                is_public, sync_status, retry_count, created_at, updated_at, client_request_id
            ) VALUES (1, 91, '旧草稿', 'legacy', 4, '', 'manual', 0, 'QUEUED', 0, 1, 2, 'legacy-id')
            """.trimIndent(),
        )

        val migration = runCatching {
            SnkDatabase.Companion::class.java
                .getDeclaredMethod("getMIGRATION_4_5")
                .invoke(SnkDatabase.Companion)
        }.getOrNull()
        assertNotNull("Room v5 must provide a v4 to v5 draft-owner migration.", migration)
        (migration as androidx.room.migration.Migration).migrate(database)

        database.query(
            "SELECT user_id, draft_owner_user_id, food_name FROM food_record_drafts WHERE id = 1",
        ).use { cursor ->
            check(cursor.moveToFirst())
            assertEquals(91L, cursor.getLong(0))
            assertEquals(91L, cursor.getLong(1))
            assertEquals("旧草稿", cursor.getString(2))
        }
        database.query("PRAGMA index_list('food_record_drafts')").use { cursor ->
            var foundOwnerIndex = false
            while (cursor.moveToNext()) {
                if (cursor.getString(1) == "index_food_record_drafts_draft_owner_user_id_updated_at") {
                    foundOwnerIndex = true
                }
            }
            assertEquals(true, foundOwnerIndex)
        }
        helper.close()
    }

    @Test
    fun dao_exposesAndMutatesOnlyTheCurrentOwnerDrafts() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, SnkDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = database.foodRecordDraftDao()
        dao.insert(draft(id = 1, ownerUserId = 101))
        dao.insert(draft(id = 2, ownerUserId = 202))

        assertEquals(listOf("101"), dao.observeAll(101).first().map { it.draftOwnerUserId.toString() })
        assertNull(dao.findById(1, 202))
        dao.deleteById(1, 202)
        assertNotNull(dao.findById(1, 101))

        database.close()
    }

    private fun draft(id: Long, ownerUserId: Long) = FoodRecordDraftEntity(
        id = id,
        userId = ownerUserId,
        draftOwnerUserId = ownerUserId,
        foodItemId = null,
        foodName = "草稿$ownerUserId",
        category = "legacy",
        subcategory = null,
        brand = null,
        barcode = null,
        rating = 4,
        comment = "",
        sourceType = "manual",
        syncStatus = "QUEUED",
        retryCount = 0,
        failureReason = null,
        failureMessage = null,
        remoteRecordId = null,
        remoteRecordTime = null,
        createdAt = id,
        updatedAt = id,
    )
}
