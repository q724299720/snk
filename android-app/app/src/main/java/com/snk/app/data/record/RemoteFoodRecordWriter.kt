package com.snk.app.data.record

interface RemoteFoodRecordWriter {
    suspend fun createRecord(
        clientRequestId: String = java.util.UUID.randomUUID().toString(),
        userId: Long,
        foodItemId: Long,
        rating: Int,
        comment: String,
        sourceType: String = "text_search",
        isPublic: Boolean = false,
        images: List<FoodRecordImageAttachment> = emptyList(),
    ): FoodRecordCreateResult
}
