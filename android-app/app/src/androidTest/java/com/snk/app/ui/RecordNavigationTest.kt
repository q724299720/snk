package com.snk.app.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.snk.app.data.record.FoodRecordHistoryItem
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RecordNavigationTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun recordNameOpensEditing() {
        var opened = false
        compose.setContent {
            GalleryItemCard(record = sampleRecord(), onEditRecord = { opened = true })
        }

        compose.onNodeWithText("麦当劳薯条").performClick()
        compose.runOnIdle { assertTrue(opened) }
    }

    private fun sampleRecord() = FoodRecordHistoryItem(
        id = 58L, userId = 100L, foodItemId = 202L, foodName = "麦当劳薯条",
        foodItemType = "dish", foodBrand = "麦当劳", foodCoverImageUrl = null,
        sourceType = "manual", isPublic = true, rating = 5, comment = null,
        likeCount = 0, recordTime = "2026-07-12T00:00:00Z", createdAt = "2026-07-12T00:00:00Z",
        images = emptyList(),
    )
}
