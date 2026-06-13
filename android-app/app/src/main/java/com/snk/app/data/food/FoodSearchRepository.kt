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
                items = response.items.map { item ->
                    FoodSearchItem(
                        id = item.id,
                        name = item.name,
                        itemType = item.itemType,
                        category = item.category,
                        subcategory = item.subcategory,
                        brand = item.brand,
                        barcode = item.barcode,
                    )
                },
                qualitySignal = response.qualitySignal,
            )
        } catch (exception: Exception) {
            FoodSearchResult.Failure(exception.asUserFacingMessage())
        }
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
)

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
