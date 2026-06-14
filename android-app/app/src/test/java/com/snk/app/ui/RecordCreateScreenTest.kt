package com.snk.app.ui

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
}
