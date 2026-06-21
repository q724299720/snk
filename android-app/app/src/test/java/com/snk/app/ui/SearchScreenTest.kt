package com.snk.app.ui

import com.snk.app.data.record.FoodRecordLikeFailureReason
import com.snk.app.data.record.FoodRecordLikeResult
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchScreenTest {

    @Test
    fun `record like ui state uses backend count on success`() {
        val state = RecordLikeUiState(likeCount = 7)

        val updated = state.afterLikeResult(FoodRecordLikeResult.Success(likeCount = 8))

        assertEquals(8, updated.likeCount)
        assertEquals("已点赞。", updated.message)
    }

    @Test
    fun `record like ui state keeps current count on failure`() {
        val state = RecordLikeUiState(likeCount = 7)

        val updated = state.afterLikeResult(
            FoodRecordLikeResult.Failure(
                reason = FoodRecordLikeFailureReason.NETWORK,
                message = "无法连接服务端，记录暂时没有提交成功。",
            ),
        )

        assertEquals(7, updated.likeCount)
        assertEquals("无法连接服务端，记录暂时没有提交成功。", updated.message)
    }
}
