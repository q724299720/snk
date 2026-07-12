package com.snk.app.ui

import com.snk.app.data.record.FoodRecordHistoryItem
import com.snk.app.data.record.FoodRecordImageAttachment
import org.junit.Assert.assertEquals
import org.junit.Test

class DiscoverImageSelectionTest {
    @Test
    fun `discover prefers thumbnail then record image then product cover`() {
        assertEquals("thumb.jpg", sampleRecord(FoodRecordImageAttachment("image.jpg", "thumb.jpg")).preferredDiscoverImageUrl())
        assertEquals("image.jpg", sampleRecord(FoodRecordImageAttachment("image.jpg", null)).preferredDiscoverImageUrl())
        assertEquals("cover.jpg", sampleRecord(null).preferredDiscoverImageUrl())
    }

    private fun sampleRecord(image: FoodRecordImageAttachment?) = FoodRecordHistoryItem(
        id = 1L,
        userId = 2L,
        foodItemId = 3L,
        foodName = "测试产品",
        foodItemType = "packaged_product",
        foodBrand = null,
        foodCoverImageUrl = "cover.jpg",
        sourceType = "manual",
        isPublic = true,
        rating = 5,
        comment = null,
        likeCount = 0,
        recordTime = "2026-07-12T00:00:00Z",
        createdAt = "2026-07-12T00:00:00Z",
        images = listOfNotNull(image),
    )
}
