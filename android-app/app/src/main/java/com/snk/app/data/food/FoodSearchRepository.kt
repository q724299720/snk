package com.snk.app.data.food

import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException

class FoodSearchRepository(
    private val api: FoodSearchApi,
) {
    suspend fun search(query: String, userId: Long? = null): FoodSearchResult {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) {
            return FoodSearchResult.Failure("请输入要搜索的食物名称。")
        }
        if (normalizedQuery.length > MAX_SEARCH_QUERY_LENGTH) {
            return FoodSearchResult.Failure("搜索词最长支持 128 个字符，请缩短后再试。")
        }

        return try {
            val response = api.searchFoods(normalizedQuery, userId)
            FoodSearchResult.Success(
                items = response.items.map(FoodSearchItemResponse::toModel),
                qualitySignal = response.qualitySignal,
            )
        } catch (exception: Exception) {
            FoodSearchResult.Failure(exception.asUserFacingMessage())
        }
    }

    suspend fun createManualFoodItem(
        userId: Long,
        name: String,
        itemType: String,
        category: String,
        subcategory: String,
        brand: String,
        barcode: String,
    ): ManualFoodCreateResult {
        val normalizedName = name.trim()
        if (normalizedName.isBlank()) {
            return ManualFoodCreateResult.Failure("请先输入名称。")
        }

        return try {
            ManualFoodCreateResult.Success(
                api.createManualFoodItem(
                    CreateManualFoodItemRequest(
                        userId = userId,
                        name = normalizedName,
                        itemType = itemType.trim(),
                        category = category.trim(),
                        subcategory = subcategory.trim().ifBlank { null },
                        brand = brand.trim().ifBlank { null },
                        barcode = barcode.trim().ifBlank { null },
                    ),
                ).toModel(),
            )
        } catch (exception: Exception) {
            ManualFoodCreateResult.Failure(exception.asManualCreateMessage())
        }
    }

    suspend fun reportFoodItem(
        userId: Long,
        foodItemId: Long,
        reason: String = "识别错误",
    ): FoodReportResult {
        if (userId <= 0L || foodItemId <= 0L) {
            return FoodReportResult.Failure("无法提交纠错信号，请稍后重试。")
        }

        return try {
            val response = api.reportFoodItem(
                foodItemId = foodItemId,
                request = CreateFoodReportRequest(
                    userId = userId,
                    reason = reason.trim().ifBlank { null },
                ),
            )
            FoodReportResult.Success(
                foodItemId = response.foodItemId,
                reportCount = response.reportCount,
                auditStatus = response.auditStatus,
            )
        } catch (exception: Exception) {
            FoodReportResult.Failure(exception.asFoodReportMessage())
        }
    }

    suspend fun recommendRelatedFoods(foodItemId: Long, limit: Int = 5): FoodSearchResult {
        if (foodItemId <= 0L) {
            return FoodSearchResult.Failure("请输入有效的食物条目。")
        }

        return try {
            val response = api.getRelatedFoods(foodItemId, limit)
            FoodSearchResult.Success(
                items = response.items.map(FoodSearchItemResponse::toModel),
                qualitySignal = response.qualitySignal,
            )
        } catch (exception: Exception) {
            FoodSearchResult.Failure(exception.asUserFacingMessage())
        }
    }

    suspend fun searchByRecognizedText(recognizedText: String, userId: Long? = null): FoodOcrSearchResult {
        val attemptedQueries = OcrSearchQueryBuilder.build(recognizedText)
        if (attemptedQueries.isEmpty()) {
            return FoodOcrSearchResult.Failure(
                recognizedText = recognizedText,
                attemptedQueries = emptyList(),
                message = "未识别到可用于搜索的文字。",
            )
        }

        attemptedQueries.forEach { query ->
            if (query.length > MAX_SEARCH_QUERY_LENGTH) {
                return@forEach
            }
            when (val result = search(query, userId)) {
                is FoodSearchResult.Success -> {
                    if (result.items.isNotEmpty()) {
                        return FoodOcrSearchResult.Success(
                            recognizedText = recognizedText,
                            attemptedQueries = attemptedQueries,
                            matchedQuery = query,
                            result = result,
                        )
                    }
                }

                is FoodSearchResult.Failure -> {
                    return FoodOcrSearchResult.Failure(
                        recognizedText = recognizedText,
                        attemptedQueries = attemptedQueries,
                        message = result.message,
                    )
                }
            }
        }

        return FoodOcrSearchResult.NoMatch(
            recognizedText = recognizedText,
            attemptedQueries = attemptedQueries,
        )
    }

    suspend fun searchByServerOcr(
        imageBytes: ByteArray,
        fileName: String,
        contentType: String,
        clientRecognizedText: String? = null,
    ): FoodOcrSearchResult {
        if (imageBytes.isEmpty()) {
            return FoodOcrSearchResult.Failure(
                recognizedText = clientRecognizedText.orEmpty(),
                attemptedQueries = emptyList(),
                message = "没有可上传的图片内容，暂时无法继续服务端 OCR。",
            )
        }

        return try {
            val requestBody = imageBytes.toRequestBody(contentType.toMediaTypeOrNull())
            val response = api.recognizeByServerOcr(
                file = MultipartBody.Part.createFormData("file", fileName, requestBody),
                clientRecognizedText = clientRecognizedText?.trim()?.ifBlank { null },
            )
            val result = FoodSearchResult.Success(
                items = response.items.map(FoodSearchItemResponse::toModel),
                qualitySignal = response.qualitySignal,
            )
            if (result.items.isNotEmpty()) {
                FoodOcrSearchResult.Success(
                    recognizedText = response.recognizedText,
                    attemptedQueries = response.attemptedQueries,
                    matchedQuery = response.matchedQuery ?: response.attemptedQueries.firstOrNull().orEmpty(),
                    result = result,
                )
            } else {
                FoodOcrSearchResult.NoMatch(
                    recognizedText = response.recognizedText,
                    attemptedQueries = response.attemptedQueries,
                )
            }
        } catch (exception: Exception) {
            FoodOcrSearchResult.Failure(
                recognizedText = clientRecognizedText.orEmpty(),
                attemptedQueries = emptyList(),
                message = exception.asServerOcrMessage(),
            )
        }
    }

    private companion object {
        const val MAX_SEARCH_QUERY_LENGTH = 128
    }
}

data class FoodSearchItem(
    val id: Long,
    val name: String,
    val itemType: String,
    val category: String,
    val subcategory: String?,
    val brand: String?,
    val barcode: String?,
    val coverImageUrl: String?,
    val averageRating: Double?,
    val auditStatus: String,
)

sealed interface ManualFoodCreateResult {
    data class Success(val item: FoodSearchItem) : ManualFoodCreateResult

    data class Failure(val message: String) : ManualFoodCreateResult
}

sealed interface FoodReportResult {
    data class Success(
        val foodItemId: Long,
        val reportCount: Int,
        val auditStatus: String,
    ) : FoodReportResult

    data class Failure(val message: String) : FoodReportResult
}

sealed interface FoodOcrSearchResult {
    data class Success(
        val recognizedText: String,
        val attemptedQueries: List<String>,
        val matchedQuery: String,
        val result: FoodSearchResult.Success,
    ) : FoodOcrSearchResult

    data class NoMatch(
        val recognizedText: String,
        val attemptedQueries: List<String>,
    ) : FoodOcrSearchResult

    data class Failure(
        val recognizedText: String,
        val attemptedQueries: List<String>,
        val message: String,
    ) : FoodOcrSearchResult
}

sealed interface FoodSearchResult {
    data class Success(
        val items: List<FoodSearchItem>,
        val qualitySignal: String,
    ) : FoodSearchResult

    data class Failure(val message: String) : FoodSearchResult
}

private fun Exception.asUserFacingMessage(): String = when (this) {
    is IOException -> "无法连接服务端搜索接口。"
    is HttpException -> "服务端搜索暂时不可用。"
    else -> "搜索失败，请稍后重试。"
}

private fun FoodSearchItemResponse.toModel(): FoodSearchItem = FoodSearchItem(
    id = id,
    name = name,
    itemType = itemType,
    category = category,
    subcategory = subcategory,
    brand = brand,
    barcode = barcode,
    coverImageUrl = coverImageUrl,
    averageRating = averageRating,
    auditStatus = auditStatus,
)

private fun Exception.asManualCreateMessage(): String = when (this) {
    is IOException -> "无法连接服务端，暂时不能创建待审核条目。"
    is HttpException -> "服务端拒绝了这次待审核条目创建。"
    else -> "创建待审核条目失败，请稍后重试。"
}

private fun Exception.asFoodReportMessage(): String = when (this) {
    is IOException -> "无法连接服务端，纠错信号暂时提交失败。"
    is HttpException -> "服务端已接收到纠错信号，可稍后继续报错。"
    else -> "纠错信号提交失败，请稍后重试。"
}

private fun Exception.asServerOcrMessage(): String = when (this) {
    is IOException -> "无法连接服务端 OCR 接口，请稍后再试或直接手动创建。"
    is HttpException -> if (code() == 503) {
        "服务端 OCR 还未配置，当前先使用手动创建兜底。"
    } else {
        "服务端 OCR 暂时不可用，请稍后重试。"
    }
    else -> "服务端 OCR 识别失败，请稍后重试。"
}
