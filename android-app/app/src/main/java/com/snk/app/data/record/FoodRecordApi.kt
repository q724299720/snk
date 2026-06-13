package com.snk.app.data.record

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

interface FoodRecordApi {
    @POST("/api/records")
    suspend fun createRecord(
        @Body request: CreateFoodRecordRequest,
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
    @SerialName("recordTime")
    val recordTime: String,
    @SerialName("createdAt")
    val createdAt: String,
)
