package com.snk.app.data.food

import java.io.IOException
import retrofit2.HttpException

class FoodSearchRepository(
    private val api: FoodSearchApi,
) {
    suspend fun search(query: String): FoodSearchResult {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) {
            return FoodSearchResult.Failure("请输入要搜索的食品名称。")
        }

        return try {
            val response = api.searchFoods(normalizedQuery)
            FoodSearchResult.Success(
                items = response.items.map(FoodSearchItemResponse::toModel),
                qualitySignal = response.qualitySignal,
            )
        } catch (exception: Exception) {
            FoodSearchResult.Failure(exception.asUserFacingMessage())
        }
    }

    suspend fun lookupByBarcode(barcode: String): FoodBarcodeLookupResult {
        val normalizedBarcode = barcode.trim()
        if (normalizedBarcode.isBlank()) {
            return FoodBarcodeLookupResult.Failure("请输入有效条码。")
        }

        return try {
            FoodBarcodeLookupResult.Success(api.lookupFoodByBarcode(normalizedBarcode).toModel())
        } catch (exception: Exception) {
            when {
                exception is HttpException && exception.code() == 404 -> FoodBarcodeLookupResult.NotFound(normalizedBarcode)
                else -> FoodBarcodeLookupResult.Failure(exception.asBarcodeLookupMessage())
            }
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

    suspend fun searchByRecognizedText(recognizedText: String): FoodOcrSearchResult {
        val attemptedQueries = OcrSearchQueryBuilder.build(recognizedText)
        if (attemptedQueries.isEmpty()) {
            return FoodOcrSearchResult.Failure(
                recognizedText = recognizedText,
                attemptedQueries = emptyList(),
                message = "未识别到可用于搜索的文字。",
            )
        }

        attemptedQueries.forEach { query ->
            when (val result = search(query)) {
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
}

data class FoodSearchItem(
    val id: Long,
    val name: String,
    val itemType: String,
    val category: String,
    val subcategory: String?,
    val brand: String?,
    val barcode: String?,
    val auditStatus: String,
)

sealed interface FoodBarcodeLookupResult {
    data class Success(val item: FoodSearchItem) : FoodBarcodeLookupResult

    data class NotFound(val barcode: String) : FoodBarcodeLookupResult

    data class Failure(val message: String) : FoodBarcodeLookupResult
}

sealed interface ManualFoodCreateResult {
    data class Success(val item: FoodSearchItem) : ManualFoodCreateResult

    data class Failure(val message: String) : ManualFoodCreateResult
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
    auditStatus = auditStatus,
)

private fun Exception.asBarcodeLookupMessage(): String = when (this) {
    is IOException -> "无法连接服务端条码接口。"
    is HttpException -> "服务端条码查询暂时不可用。"
    else -> "条码查询失败，请稍后重试。"
}

private fun Exception.asManualCreateMessage(): String = when (this) {
    is IOException -> "无法连接服务端，暂时不能创建待审核条目。"
    is HttpException -> "服务端拒绝了这次待审核条目创建。"
    else -> "创建待审核条目失败，请稍后重试。"
}
