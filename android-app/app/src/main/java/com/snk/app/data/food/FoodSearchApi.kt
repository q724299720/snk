package com.snk.app.data.food

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

interface FoodSearchApi {
    @GET("/api/foods/search")
    suspend fun searchFoods(
        @Query("q") query: String,
    ): FoodSearchResponse
}

@Serializable
data class FoodSearchResponse(
    @SerialName("items")
    val items: List<FoodSearchItemResponse>,
    @SerialName("qualitySignal")
    val qualitySignal: String,
)

@Serializable
data class FoodSearchItemResponse(
    @SerialName("id")
    val id: Long,
    @SerialName("name")
    val name: String,
    @SerialName("itemType")
    val itemType: String,
    @SerialName("category")
    val category: String,
    @SerialName("subcategory")
    val subcategory: String? = null,
    @SerialName("brand")
    val brand: String? = null,
    @SerialName("barcode")
    val barcode: String? = null,
    @SerialName("coverImageUrl")
    val coverImageUrl: String? = null,
)
