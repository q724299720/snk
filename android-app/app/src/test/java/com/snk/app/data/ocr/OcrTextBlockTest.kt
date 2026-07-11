package com.snk.app.data.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrTextBlockTest {
    @Test
    fun `noise detection lowers weight without deleting the block`() {
        assertTrue(isLikelyOcrNoise("净含量 100g"))
        assertTrue(isLikelyOcrNoise("2026-07-11"))
        assertTrue(isLikelyOcrNoise("6901234567890"))
        assertTrue(isLikelyOcrNoise("配料表：小麦粉、植物油"))
        assertFalse(isLikelyOcrNoise("乐事原切薯片"))
    }

    @Test
    fun `fit mapping accounts for letterboxing`() {
        val mapped = mapOcrBoxToFit(
            box = OcrBoundingBox(100f, 50f, 300f, 150f),
            imageWidth = 400f,
            imageHeight = 200f,
            containerWidth = 400f,
            containerHeight = 400f,
        )

        assertEquals(100f, mapped.left, 0.01f)
        assertEquals(150f, mapped.top, 0.01f)
        assertEquals(300f, mapped.right, 0.01f)
        assertEquals(250f, mapped.bottom, 0.01f)
    }
}
