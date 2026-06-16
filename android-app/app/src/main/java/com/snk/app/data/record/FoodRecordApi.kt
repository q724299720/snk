package com.snk.app.data.record

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Query

interface FoodRecordApi {
    @GET("/api/records")
    suspend fun listRecentRecords(
        @Query("userId") userId: Long,
        @Query("limit") limit: Int = 10,
    ): List<FoodRecordHistoryResponse>

    @POST("/api/records")
    suspend fun createRecord(
        @Body request: CreateFoodRecordRequest,
    ): FoodRecordResponse

    @POST("/api/records/{recordId}/like")
    suspend fun likeRecord(
        @Path("recordId") recordId: Long,
    ): FoodRecordResponse
}

@Serializable
data class CreateFoodRecordRequest(
    @SerialName("userId")
    val userId: Long,
    @SerialName("foodItemId")
    val foodItemId: Long,
    @SerialName("sourceType")
    val sourceType: String,
    @SerialName("isPublic")
    val isPublic: Boolean,
    @SerialName("rating")
    val rating: Int,
    @SerialName("comment")
    val comment: String? = null,
)

@Serializable
data class FoodRecordHistoryResponse(
    @SerialName("id")
    val id: Long,
    @SerialName("userId")
    val userId: Long,
    @SerialName("foodItemId")
    val foodItemId: Long,
    @SerialName("foodName")
    val foodName: String,
    @SerialName("foodItemType")
    val foodItemType: String,
    @SerialName("foodCategory")
    val foodCategory: String,
    @SerialName("foodSubcategory")
    val foodSubcategory: String? = null,
    @SerialName("foodBrand")
    val foodBrand: String? = null,
    @SerialName("foodCoverImageUrl")
    val foodCoverImageUrl: String? = null,
    @SerialName("sourceType")
    val sourceType: String,
    @SerialName("isPublic")
    val isPublic: Boolean,
    @SerialName("rating")
    val rating: Int,
    @SerialName("comment")
    val comment: String? = null,
    @SerialName("likeCount")
    val likeCount: Int = 0,
    @SerialName("recordTime")
    val recordTime: String,
    @SerialName("createdAt")
    val createdAt: String,
)

@Serializable
data class FoodRecordResponse(
    @SerialName("id")
    val id: Long,
    @SerialName("userId")
    val userId: Long,
    @SerialName("foodItemId")
    val foodItemId: Long,
    @SerialName("sourceType")
    val sourceType: String,
    @SerialName("isPublic")
    val isPublic: Boolean,
    @SerialName("rating")
    val rating: Int,
    @SerialName("comment")
    val comment: String? = null,
    @SerialName("likeCount")
    val likeCount: Int = 0,
    @SerialName("recordTime")
    val recordTime: String,
    @SerialName("createdAt")
    val createdAt: String,
)
