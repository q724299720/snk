package com.snk.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_record_drafts")
data class FoodRecordDraftEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "user_id")
    val userId: Long,
    @ColumnInfo(name = "food_item_id")
    val foodItemId: Long,
    @ColumnInfo(name = "food_name")
    val foodName: String,
    val category: String,
    val subcategory: String?,
    val brand: String?,
    val barcode: String?,
    val rating: Int,
    val comment: String,
    @ColumnInfo(name = "source_type")
    val sourceType: String,
    @ColumnInfo(name = "is_public", defaultValue = "0")
    val isPublic: Boolean = false,
    @ColumnInfo(name = "sync_status")
    val syncStatus: String,
    @ColumnInfo(name = "retry_count")
    val retryCount: Int,
    @ColumnInfo(name = "failure_reason")
    val failureReason: String?,
    @ColumnInfo(name = "failure_message")
    val failureMessage: String?,
    @ColumnInfo(name = "remote_record_id")
    val remoteRecordId: Long?,
    @ColumnInfo(name = "remote_record_time")
    val remoteRecordTime: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
