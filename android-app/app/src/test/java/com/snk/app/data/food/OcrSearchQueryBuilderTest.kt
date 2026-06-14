package com.snk.app.data.food

import org.junit.Assert.assertEquals
import org.junit.Test

class OcrSearchQueryBuilderTest {
    @Test
    fun `build returns original compact and merged queries`() {
        val result = OcrSearchQueryBuilder.build("乐事\n黄瓜味 薯片！")

        assertEquals(
            listOf(
                "乐事 黄瓜味 薯片！",
                "乐事 黄瓜味 薯片",
                "乐事黄瓜味薯片",
            ),
            result,
        )
    }

    @Test
    fun `build returns empty list for blank text`() {
        assertEquals(emptyList<String>(), OcrSearchQueryBuilder.build("   "))
    }
}
