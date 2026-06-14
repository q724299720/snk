package com.snk.app.data.record

import java.io.IOException
import retrofit2.HttpException

class FoodRecordRepository(
    private val api: FoodRecordApi,
) : RemoteFoodRecordWriter {
    override suspend fun createRecord(
        userId: Long,
        foodItemId: Long,
        rating: Int,
        comment: String,
        sourceType: String,
    ): FoodRecordCreateResult {
        if (rating !in 1..5) {
            return FoodRecordCreateResult.Failure(
                reason = FoodRecordCreateFailureReason.UNKNOWN,
                message = "评分必须在 1 到 5 之间。",
            )
        }

        return try {
            val response = api.createRecord(
                CreateFoodRecordRequest(
                    userId = userId,
                    foodItemId = foodItemId,
                    sourceType = sourceType,
                    isPublic = false,
                    rating = rating,
                    comment = comment.trim().ifBlank { null },
                ),
            )
            FoodRecordCreateResult.Success(
                recordId = response.id,
                recordTime = response.recordTime,
            )
        } catch (exception: Exception) {
            FoodRecordCreateResult.Failure(
                reason = exception.asFailureReason(),
                message = exception.asUserFacingMessage(),
            )
        }
    }
}

enum class FoodRecordCreateFailureReason {
    NETWORK,
    SERVER,
    UNKNOWN,
}

sealed interface FoodRecordCreateResult {
    data class Success(
        val recordId: Long,
        val recordTime: String,
    ) : FoodRecordCreateResult

    data class Failure(
        val reason: FoodRecordCreateFailureReason,
        val message: String,
    ) : FoodRecordCreateResult
}

private fun Exception.asUserFacingMessage(): String = when (this) {
    is IOException -> "无法连接服务端，记录暂时没有提交成功。"
    is HttpException -> "服务端拒绝了这次记录提交。"
    else -> "保存记录失败，请稍后重试。"
}

private fun Exception.asFailureReason(): FoodRecordCreateFailureReason = when (this) {
    is IOException -> FoodRecordCreateFailureReason.NETWORK
    is HttpException -> FoodRecordCreateFailureReason.SERVER
    else -> FoodRecordCreateFailureReason.UNKNOWN
}
