package com.snk.app.ui

import com.snk.app.data.record.FoodRecordSubmissionResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

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

    @Test
    fun `record comment validation disables submit when comment is too long`() {
        val valid = validateRecordCommentForUi("a".repeat(500))
        val overlong = validateRecordCommentForUi("a".repeat(501))

        assertFalse(valid.hasError)
        assertNull(valid.message)
        assertTrue(overlong.hasError)
        assertEquals("备注最长支持 500 个字符，当前 501 个。", overlong.message)
    }

    @Test
    fun `record create screen content remains reachable on short screens`() {
        val sourcePath = listOf(
            Path.of("src/main/java/com/snk/app/ui/RecordCreateScreen.kt"),
            Path.of("app/src/main/java/com/snk/app/ui/RecordCreateScreen.kt"),
        ).first(Files::exists)
        val source = String(Files.readAllBytes(sourcePath))

        assertTrue(source.contains(".verticalScroll("))
        assertTrue(source.contains(".imePadding()"))
        assertTrue(source.contains(".navigationBarsPadding()"))
    }

    @Test
    fun `record image validation blocks save until selected image uploads`() {
        val uploading = validateRecordImageForSave(
            hasSelectedImage = true,
            hasUploadedImage = false,
            isUploadingImage = true,
        )
        val failedOrMissingUpload = validateRecordImageForSave(
            hasSelectedImage = true,
            hasUploadedImage = false,
            isUploadingImage = false,
        )
        val uploaded = validateRecordImageForSave(
            hasSelectedImage = true,
            hasUploadedImage = true,
            isUploadingImage = false,
        )

        assertFalse(uploading.canSave)
        assertEquals("图片正在上传，上传完成后再保存。", uploading.message)
        assertFalse(failedOrMissingUpload.canSave)
        assertEquals("图片上传未完成或失败，请重新选择图片，或移除图片后保存。", failedOrMissingUpload.message)
        assertTrue(uploaded.canSave)
        assertNull(uploaded.message)
    }

    @Test
    fun `record create image upload normalizes selected media to jpeg`() {
        val sourcePath = listOf(
            Path.of("src/main/java/com/snk/app/ui/RecordCreateScreen.kt"),
            Path.of("app/src/main/java/com/snk/app/ui/RecordCreateScreen.kt"),
        ).first(Files::exists)
        val source = String(Files.readAllBytes(sourcePath))

        assertTrue(source.contains("ImageDecoder.decodeBitmap"))
        assertTrue(source.contains("Bitmap.CompressFormat.JPEG"))
        assertTrue(source.contains("contentType = \"image/jpeg\""))
        assertTrue(source.contains("record-${'$'}{System.currentTimeMillis()}.jpg"))
    }

    @Test
    fun `record submit feedback makes save result visible`() {
        val submitted = FoodRecordSubmissionResult.Submitted(
            recordId = 88L,
            recordTime = "2026-06-21T12:00:00Z",
            likeCount = 0,
        )
        val failure = FoodRecordSubmissionResult.Failure("保存失败")

        assertEquals("记录已保存，图片已保存。", buildRecordSubmitFeedback(submitted, hasUploadedImage = true))
        assertEquals("记录已保存。", buildRecordSubmitFeedback(submitted, hasUploadedImage = false))
        assertEquals("保存失败", buildRecordSubmitFeedback(failure, hasUploadedImage = false))
    }
}
