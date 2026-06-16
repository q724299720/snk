package com.snk.app.data.food

import org.junit.Assert.assertEquals
import org.junit.Test

class OcrSearchQueryBuilderTest {
    @Test
    fun `build keeps english full query and merged fallback`() {
        val result = OcrSearchQueryBuilder.build("Lays\nCucumber Chips!")

        assertEquals(
            listOf(
                "Lays Cucumber Chips!",
                "Lays Cucumber Chips",
                "LaysCucumberChips",
                "Cucumber Chips",
                "Chips",
                "Lays",
            ),
            result,
        )
    }

    @Test
    fun `buildDisplayQueries creates grouped chinese phrases`() {
        val result = OcrSearchQueryBuilder.buildDisplayQueries("康师傅红烧牛肉面")

        assertEquals(
            listOf(
                "康师傅红烧牛肉面",
                "红烧牛肉面",
                "牛肉面",
                "康师傅",
            ),
            result,
        )
    }

    @Test
    fun `build returns empty list for blank text`() {
        assertEquals(emptyList<String>(), OcrSearchQueryBuilder.build("   "))
    }
}
