package com.snk.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodRecordDraftDao {
    @Query("SELECT * FROM food_record_drafts ORDER BY updated_at DESC")
    fun observeAll(): Flow<List<FoodRecordDraftEntity>>

    @Query("SELECT * FROM food_record_drafts WHERE id = :draftId LIMIT 1")
    suspend fun findById(draftId: Long): FoodRecordDraftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FoodRecordDraftEntity): Long

    @Query(
        """
        UPDATE food_record_drafts
        SET sync_status = :syncStatus,
            retry_count = :retryCount,
            failure_reason = :failureReason,
            failure_message = :failureMessage,
            updated_at = :updatedAt
        WHERE id = :draftId
        """,
    )
    suspend fun updateSyncState(
        draftId: Long,
        syncStatus: String,
        retryCount: Int,
        failureReason: String?,
        failureMessage: String?,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE food_record_drafts
        SET sync_status = :syncStatus,
            retry_count = :retryCount,
            failure_reason = NULL,
            failure_message = NULL,
            remote_record_id = :remoteRecordId,
            remote_record_time = :remoteRecordTime,
            updated_at = :updatedAt
        WHERE id = :draftId
        """,
    )
    suspend fun markSynced(
        draftId: Long,
        syncStatus: String,
        retryCount: Int,
        remoteRecordId: Long,
        remoteRecordTime: String,
        updatedAt: Long,
    )
}
