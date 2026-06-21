package com.snk.app.data.record

import com.snk.app.data.food.FoodSearchItem
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
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

    suspend fun listPublicRecords(limit: Int = 10): FoodRecordHistoryResult {
        return try {
            FoodRecordHistoryResult.Success(
                api.listPublicRecords(limit).map(FoodRecordHistoryResponse::toModel),
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
        isPublic: Boolean,
        images: List<FoodRecordImageAttachment>,
    ): FoodRecordCreateResult {
        if (rating !in 1..5) {
            return FoodRecordCreateResult.Failure(
                reason = FoodRecordCreateFailureReason.UNKNOWN,
                message = "评分必须在 1 到 5 之间。",
            )
        }
        val normalizedComment = comment.trim()
        if (normalizedComment.length > MAX_RECORD_COMMENT_LENGTH) {
            return FoodRecordCreateResult.Failure(
                reason = FoodRecordCreateFailureReason.UNKNOWN,
                message = "备注最长支持 500 个字符，请缩短后再保存。",
            )
        }

        return try {
            val response = api.createRecord(
                CreateFoodRecordRequest(
                    userId = userId,
                    foodItemId = foodItemId,
                    sourceType = sourceType,
                    isPublic = isPublic,
                    rating = rating,
                    comment = normalizedComment.ifBlank { null },
                    images = images.map {
                        FoodRecordImageRequest(
                            imageUrl = it.imageUrl,
                            thumbnailUrl = it.thumbnailUrl,
                        )
                    },
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

    suspend fun listRecordComments(
        recordId: Long,
        limit: Int = 10,
    ): FoodRecordCommentsResult {
        if (recordId <= 0L) {
            return FoodRecordCommentsResult.Failure("记录不存在，暂时无法查看评论。")
        }

        return try {
            FoodRecordCommentsResult.Success(
                api.listRecordComments(recordId, limit).map(FoodRecordCommentResponse::toModel),
            )
        } catch (exception: Exception) {
            FoodRecordCommentsResult.Failure(exception.asCommentMessage())
        }
    }

    suspend fun createRecordComment(
        recordId: Long,
        userId: Long,
        content: String,
    ): FoodRecordCommentCreateResult {
        val normalizedContent = content.trim()
        if (recordId <= 0L || userId <= 0L) {
            return FoodRecordCommentCreateResult.Failure("游客身份或记录信息无效，暂时无法评论。")
        }
        if (normalizedContent.isBlank()) {
            return FoodRecordCommentCreateResult.Failure("评论不能为空。")
        }

        return try {
            FoodRecordCommentCreateResult.Success(
                api.createRecordComment(
                    recordId = recordId,
                    request = CreateFoodRecordCommentRequest(
                        userId = userId,
                        content = normalizedContent,
                    ),
                ).toModel(),
            )
        } catch (exception: Exception) {
            FoodRecordCommentCreateResult.Failure(exception.asCommentMessage())
        }
    }

    suspend fun uploadRecordImage(
        imageBytes: ByteArray,
        fileName: String,
        contentType: String,
    ): RecordImageUploadResult {
        if (imageBytes.isEmpty()) {
            return RecordImageUploadResult.Failure("没有可上传的图片内容。")
        }

        return try {
            val requestBody = imageBytes.toRequestBody(contentType.toMediaTypeOrNull())
            val response = api.uploadImage(
                MultipartBody.Part.createFormData("file", fileName, requestBody),
            )
            RecordImageUploadResult.Success(
                FoodRecordImageAttachment(
                    imageUrl = response.resourceUrl,
                    thumbnailUrl = response.thumbnailUrl,
                ),
            )
        } catch (exception: Exception) {
            RecordImageUploadResult.Failure(exception.asImageUploadMessage())
        }
    }
}

private const val MAX_RECORD_COMMENT_LENGTH = 500

data class FoodRecordImageAttachment(
    val imageUrl: String,
    val thumbnailUrl: String?,
)

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
    val images: List<FoodRecordImageAttachment>,
)

data class FoodRecordComment(
    val id: Long,
    val recordId: Long,
    val userId: Long,
    val content: String,
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

sealed interface RecordImageUploadResult {
    data class Success(val image: FoodRecordImageAttachment) : RecordImageUploadResult

    data class Failure(val message: String) : RecordImageUploadResult
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

sealed interface FoodRecordCommentsResult {
    data class Success(
        val comments: List<FoodRecordComment>,
    ) : FoodRecordCommentsResult

    data class Failure(val message: String) : FoodRecordCommentsResult
}

sealed interface FoodRecordCommentCreateResult {
    data class Success(
        val comment: FoodRecordComment,
    ) : FoodRecordCommentCreateResult

    data class Failure(val message: String) : FoodRecordCommentCreateResult
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
    images = images.map {
        FoodRecordImageAttachment(
            imageUrl = it.imageUrl,
            thumbnailUrl = it.thumbnailUrl,
        )
    },
)

private fun FoodRecordCommentResponse.toModel(): FoodRecordComment = FoodRecordComment(
    id = id,
    recordId = recordId,
    userId = userId,
    content = content,
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

private fun Exception.asImageUploadMessage(): String = when (this) {
    is IOException -> "无法连接服务端上传图片，请稍后重试。"
    is HttpException -> "服务端拒绝了这张图片，请更换图片后重试。"
    else -> "图片上传失败，请稍后重试。"
}

private fun Exception.asCommentMessage(): String = when (this) {
    is IOException -> "无法连接服务端，暂时无法同步评论。"
    is HttpException -> "服务端拒绝了这次评论操作。"
    else -> "评论操作失败，请稍后重试。"
}
