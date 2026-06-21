package com.snk.app.ui

import com.snk.app.data.record.FoodRecordSubmissionResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordCreateScreenTest {

    @Test
    fun `buildRecordShareText includes food rating and record metadata`() {
        val text = buildRecordShareText(
            foodName = "乐事黄瓜味薯片",
            rating = 5,
            comment = "很脆",
            recordId = 18L,
            recordTime = "2026-06-14T12:00:01Z",
        )

        assertTrue(text.contains("SNK 记录分享"))
        assertTrue(text.contains("乐事黄瓜味薯片"))
        assertTrue(text.contains("评分：5 分"))
        assertTrue(text.contains("备注：很脆"))
        assertTrue(text.contains("record_id：18"))
    }

    @Test
    fun `record create transient state resets when switching recommended food`() {
        val previous = RecordCreateTransientState(
            rating = 2,
            comment = "上一条记录备注",
            submitState = FoodRecordSubmissionResult.Submitted(
                recordId = 18L,
                recordTime = "2026-06-21T12:00:00Z",
                likeCount = 9,
            ),
            likeCount = 9,
            interactionMessage = "已更新点赞数",
            isPublic = true,
            imageUploadMessage = "Image uploaded. It will be saved with this record.",
            isUploadingImage = true,
        )

        val reset = previous.resetForFoodSwitch()

        assertEquals(DEFAULT_RECORD_RATING, reset.rating)
        assertEquals("", reset.comment)
        assertNull(reset.submitState)
        assertEquals(0, reset.likeCount)
        assertNull(reset.interactionMessage)
        assertFalse(reset.isPublic)
        assertNull(reset.imageUploadMessage)
        assertFalse(reset.isUploadingImage)
    }
}
