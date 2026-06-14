package com.snk.app.data.food

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Part
import retrofit2.http.Query

interface FoodSearchApi {
    @GET("/api/foods/search")
    suspend fun searchFoods(
        @Query("q") query: String,
    ): FoodSearchResponse

    @GET("/api/foods/barcode/{code}")
    suspend fun lookupFoodByBarcode(
        @Path("code") barcode: String,
    ): FoodSearchItemResponse

    @POST("/api/foods/manual")
    suspend fun createManualFoodItem(
        @Body request: CreateManualFoodItemRequest,
    ): FoodSearchItemResponse

    @Multipart
    @POST("/api/recognition/ocr")
    suspend fun recognizeByServerOcr(
        @Part file: MultipartBody.Part,
        @Part("clientRecognizedText") clientRecognizedText: String? = null,
    ): OcrRecognitionResponse
}

@Serializable
data class FoodSearchResponse(
    @SerialName("items")
    val items: List<FoodSearchItemResponse>,
    @SerialName("qualitySignal")
    val qualitySignal: String,
)

@Serializable
data class CreateManualFoodItemRequest(
    @SerialName("userId")
    val userId: Long,
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
    @SerialName("auditStatus")
    val auditStatus: String = "approved",
)

@Serializable
data class OcrRecognitionResponse(
    @SerialName("recognizedText")
    val recognizedText: String = "",
    @SerialName("attemptedQueries")
    val attemptedQueries: List<String> = emptyList(),
    @SerialName("matchedQuery")
    val matchedQuery: String? = null,
    @SerialName("qualitySignal")
    val qualitySignal: String = "weak",
    @SerialName("items")
    val items: List<FoodSearchItemResponse> = emptyList(),
)
