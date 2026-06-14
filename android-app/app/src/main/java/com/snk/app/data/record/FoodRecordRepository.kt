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
                likeCount = response.likeCount,
            )
        } catch (exception: Exception) {
            FoodRecordCreateResult.Failure(
                reason = exception.asFailureReason(),
                message = exception.asUserFacingMessage(),
            )
        }
    }

    suspend fun likeRecord(recordId: Long): FoodRecordLikeResult {
        return try {
            val response = api.likeRecord(recordId)
            FoodRecordLikeResult.Success(
                likeCount = response.likeCount,
            )
        } catch (exception: Exception) {
            FoodRecordLikeResult.Failure(
                reason = exception.asLikeFailureReason(),
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
        val likeCount: Int,
    ) : FoodRecordCreateResult

    data class Failure(
        val reason: FoodRecordCreateFailureReason,
        val message: String,
    ) : FoodRecordCreateResult
}

enum class FoodRecordLikeFailureReason {
    NETWORK,
    SERVER,
    UNKNOWN,
}

sealed interface FoodRecordLikeResult {
    data class Success(
        val likeCount: Int,
    ) : FoodRecordLikeResult

    data class Failure(
        val reason: FoodRecordLikeFailureReason,
        val message: String,
    ) : FoodRecordLikeResult
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

private fun Exception.asLikeFailureReason(): FoodRecordLikeFailureReason = when (this) {
    is IOException -> FoodRecordLikeFailureReason.NETWORK
    is HttpException -> FoodRecordLikeFailureReason.SERVER
    else -> FoodRecordLikeFailureReason.UNKNOWN
}
