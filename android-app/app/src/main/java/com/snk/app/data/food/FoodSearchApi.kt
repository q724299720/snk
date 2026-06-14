package com.snk.app.data.food

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
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

    @GET("/api/foods/{foodItemId}/related")
    suspend fun getRelatedFoods(
        @Path("foodItemId") foodItemId: Long,
        @Query("limit") limit: Int = 5,
    ): FoodSearchResponse

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

    @Multipart
    @POST("/api/upload/image")
    suspend fun uploadImage(
        @Part file: MultipartBody.Part,
    ): UploadImageResponse

    @POST("/api/recognition/tasks")
    suspend fun createRecognitionTask(
        @Body request: CreateRecognitionTaskRequest,
    ): RecognitionTaskResponse

    @GET("/api/recognition/tasks/{taskId}")
    suspend fun getRecognitionTask(
        @Path("taskId") taskId: Long,
    ): RecognitionTaskResponse
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

@Serializable
data class UploadImageResponse(
    @SerialName("objectKey")
    val objectKey: String,
    @SerialName("resourceUrl")
    val resourceUrl: String,
    @SerialName("contentType")
    val contentType: String,
    @SerialName("size")
    val size: Long,
)

@Serializable
data class CreateRecognitionTaskRequest(
    @SerialName("userId")
    val userId: Long,
    @SerialName("inputImageUrl")
    val inputImageUrl: String,
)

@Serializable
data class RecognitionTaskResponse(
    @SerialName("id")
    val id: Long,
    @SerialName("userId")
    val userId: Long,
    @SerialName("inputImageUrl")
    val inputImageUrl: String,
    @SerialName("status")
    val status: String,
    @SerialName("topCandidates")
    val topCandidates: List<FoodSearchItemResponse> = emptyList(),
    @SerialName("selectedFoodItemId")
    val selectedFoodItemId: Long? = null,
    @SerialName("confidence")
    val confidence: String? = null,
    @SerialName("createdAt")
    val createdAt: String? = null,
    @SerialName("finishedAt")
    val finishedAt: String? = null,
    @SerialName("statusReason")
    val statusReason: String? = null,
)
