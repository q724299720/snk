package com.snk.app.data.record

interface RemoteFoodRecordWriter {
    suspend fun createRecord(
        userId: Long,
        foodItemId: Long,
        rating: Int,
        comment: String,
        sourceType: String = "text_search",
    ): FoodRecordCreateResult
}
