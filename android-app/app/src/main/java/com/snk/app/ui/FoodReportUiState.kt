package com.snk.app.ui

import com.snk.app.data.food.FoodReportResult

data class FoodReportUiState(
    val reason: String = "",
    val message: String? = null,
    val isSubmitting: Boolean = false,
) {
    fun reasonOrError(): ReasonValidationResult {
        val normalizedReason = reason.trim()
        if (normalizedReason.isBlank()) {
            return ReasonValidationResult.Invalid(copy(message = "请先填写纠错原因。"))
        }
        return ReasonValidationResult.Valid(normalizedReason)
    }

    fun submitting(): FoodReportUiState = copy(isSubmitting = true, message = null)

    fun afterReportResult(result: FoodReportResult): FoodReportUiState = when (result) {
        is FoodReportResult.Success -> copy(
            reason = "",
            isSubmitting = false,
            message = "已提交纠错，当前 reportCount = ${result.reportCount}",
        )

        is FoodReportResult.Failure -> copy(
            isSubmitting = false,
            message = result.message,
        )
    }
}

sealed interface ReasonValidationResult {
    data class Valid(val reason: String) : ReasonValidationResult
    data class Invalid(val state: FoodReportUiState) : ReasonValidationResult
}
