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

sealed interface FoodBarcodeLookupResult {
    data class Success(val item: FoodSearchItem) : FoodBarcodeLookupResult

    data class NotFound(val barcode: String) : FoodBarcodeLookupResult

    data class Failure(val message: String) : FoodBarcodeLookupResult
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
)

private fun Exception.asBarcodeLookupMessage(): String = when (this) {
    is IOException -> "无法连接服务端条码接口。"
    is HttpException -> "服务端条码查询暂时不可用。"
    else -> "条码查询失败，请稍后重试。"
}
