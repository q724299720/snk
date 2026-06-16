package com.snk.app.data.record

import com.snk.app.data.food.FoodSearchItem
import java.io.IOException
import retrofit2.HttpException

class FoodRecordRepository(
    private val api: FoodRecordApi,
) : RemoteFoodRecordWriter {
    suspend fun listRecentRecords(
        userId: Long,
        limit: Int = 10,
    ): FoodRecordHistoryResult {
        if (userId <= 0L) {
            return FoodRecordHistoryResult.Failure("游客身份尚未完成初始化。")
        }

        return try {
            FoodRecordHistoryResult.Success(
                api.listRecentRecords(userId, limit).map(FoodRecordHistoryResponse::toModel),
            )
        } catch (exception: Exception) {
            FoodRecordHistoryResult.Failure(exception.asHistoryMessage())
        }
    }

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

data class FoodRecordHistoryItem(
    val id: Long,
    val userId: Long,
    val foodItemId: Long,
    val foodName: String,
    val foodItemType: String,
    val foodCategory: String,
    val foodSubcategory: String?,
    val foodBrand: String?,
    val foodCoverImageUrl: String?,
    val sourceType: String,
    val isPublic: Boolean,
    val rating: Int,
    val comment: String?,
    val likeCount: Int,
    val recordTime: String,
    val createdAt: String,
)

fun FoodRecordHistoryItem.toFoodSearchItem(): FoodSearchItem = FoodSearchItem(
    id = foodItemId,
    name = foodName,
    itemType = foodItemType,
    category = foodCategory,
    subcategory = foodSubcategory,
    brand = foodBrand,
    barcode = null,
    coverImageUrl = foodCoverImageUrl,
    averageRating = null,
    auditStatus = "approved",
)

sealed interface FoodRecordHistoryResult {
    data class Success(
        val items: List<FoodRecordHistoryItem>,
    ) : FoodRecordHistoryResult

    data class Failure(val message: String) : FoodRecordHistoryResult
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

private fun FoodRecordHistoryResponse.toModel(): FoodRecordHistoryItem = FoodRecordHistoryItem(
    id = id,
    userId = userId,
    foodItemId = foodItemId,
    foodName = foodName,
    foodItemType = foodItemType,
    foodCategory = foodCategory,
    foodSubcategory = foodSubcategory,
    foodBrand = foodBrand,
    foodCoverImageUrl = foodCoverImageUrl,
    sourceType = sourceType,
    isPublic = isPublic,
    rating = rating,
    comment = comment,
    likeCount = likeCount,
    recordTime = recordTime,
    createdAt = createdAt,
)

private fun Exception.asUserFacingMessage(): String = when (this) {
    is IOException -> "无法连接服务端，记录暂时没有提交成功。"
    is HttpException -> "服务端拒绝了这次记录提交。"
    else -> "保存记录失败，请稍后重试。"
}

private fun Exception.asHistoryMessage(): String = when (this) {
    is IOException -> "无法连接服务端，暂时拉取不到历史记录。"
    is HttpException -> "服务端暂时无法返回历史记录。"
    else -> "加载历史记录失败，请稍后重试。"
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
