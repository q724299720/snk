package com.snk.app.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickRecordScreenTest {
    @Test
    fun `quick record starts without a rating and keeps one request id`() {
        val source = File("src/main/java/com/snk/app/ui/QuickRecordScreen.kt").readText()

        assertTrue(source.contains("var rating by remember { mutableStateOf<Int?>(null) }"))
        assertTrue(source.contains("val clientRequestId = remember { UUID.randomUUID().toString() }"))
        assertTrue(source.contains("enabled = name.isNotBlank() && rating != null"))
        assertTrue(source.contains("isPublic = false"))
    }
}
