package com.snk.app.ui

import com.snk.app.data.record.FoodRecordUpdateResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class RecordEditScreenTest {

    @Test
    fun `record edit feedback makes update result visible`() {
        val success = FoodRecordUpdateResult.Success(
            recordId = 55L,
            rating = 5,
            comment = "better after edit",
            isPublic = true,
        )
        val failure = FoodRecordUpdateResult.Failure("update failed")

        assertEquals("记录已更新。", buildRecordEditFeedback(success))
        assertEquals("update failed", buildRecordEditFeedback(failure))
        assertNull(buildRecordEditFeedback(null))
    }

    @Test
    fun `record edit validation reuses record comment length limit`() {
        val valid = validateRecordCommentForUi("a".repeat(500))
        val overlong = validateRecordCommentForUi("a".repeat(501))

        assertFalse(valid.hasError)
        assertTrue(overlong.hasError)
    }

    @Test
    fun `record edit screen supports image replacement controls`() {
        val sourcePath = listOf(
            Path.of("src/main/java/com/snk/app/ui/RecordEditScreen.kt"),
            Path.of("app/src/main/java/com/snk/app/ui/RecordEditScreen.kt"),
        ).first(Files::exists)
        val source = String(Files.readAllBytes(sourcePath))

        assertTrue(source.contains("PickVisualMedia"))
        assertTrue(source.contains("uploadRecordImage"))
        assertTrue(source.contains("选择新图片"))
        assertTrue(source.contains("移除图片"))
    }

    @Test
    fun `search screen exposes record edit entry`() {
        val sourcePath = listOf(
            Path.of("src/main/java/com/snk/app/ui/SearchScreen.kt"),
            Path.of("app/src/main/java/com/snk/app/ui/SearchScreen.kt"),
        ).first(Files::exists)
        val source = String(Files.readAllBytes(sourcePath))

        assertTrue(source.contains("onEditRecord"))
        assertTrue(source.contains("编辑记录"))
    }

    @Test
    fun `app navigation defines record edit route`() {
        val sourcePath = listOf(
            Path.of("src/main/java/com/snk/app/ui/SnkApp.kt"),
            Path.of("app/src/main/java/com/snk/app/ui/SnkApp.kt"),
        ).first(Files::exists)
        val source = String(Files.readAllBytes(sourcePath))

        assertTrue(source.contains("record_edit"))
        assertTrue(source.contains("RecordEditScreen"))
    }
}
