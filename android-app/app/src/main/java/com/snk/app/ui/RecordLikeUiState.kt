package com.snk.app.ui

import com.snk.app.data.record.FoodRecordLikeResult

data class RecordLikeUiState(
    val likeCount: Int,
    val message: String? = null,
) {
    fun afterLikeResult(result: FoodRecordLikeResult): RecordLikeUiState = when (result) {
        is FoodRecordLikeResult.Success -> copy(
            likeCount = result.likeCount,
            message = "已点赞。",
        )

        is FoodRecordLikeResult.Failure -> copy(
            message = result.message,
        )
    }
}
