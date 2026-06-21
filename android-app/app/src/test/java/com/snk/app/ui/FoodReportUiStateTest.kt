package com.snk.app.ui

import com.snk.app.data.food.FoodReportResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodReportUiStateTest {

    @Test
    fun `blank reason is rejected before submitting report`() {
        val state = FoodReportUiState(reason = "   ")

        val result = state.reasonOrError()

        assertTrue(result is ReasonValidationResult.Invalid)
        val invalid = result as ReasonValidationResult.Invalid
        assertEquals("请先填写纠错原因。", invalid.state.message)
    }

    @Test
    fun `valid reason is trimmed before submitting report`() {
        val state = FoodReportUiState(reason = "  图片和名称不匹配  ")

        val result = state.reasonOrError()

        assertTrue(result is ReasonValidationResult.Valid)
        val valid = result as ReasonValidationResult.Valid
        assertEquals("图片和名称不匹配", valid.reason)
    }

    @Test
    fun `successful report clears reason and shows updated count`() {
        val state = FoodReportUiState(reason = "图片和名称不匹配", isSubmitting = true)

        val updated = state.afterReportResult(
            FoodReportResult.Success(
                foodItemId = 18L,
                reportCount = 4,
                auditStatus = "approved",
            ),
        )

        assertEquals("", updated.reason)
        assertFalse(updated.isSubmitting)
        assertEquals("已提交纠错，当前 reportCount = 4", updated.message)
    }
}
