package com.snk.app.data.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

interface AnonymousAuthApi {
    @POST("/api/auth/anonymous")
    suspend fun initializeAnonymousUser(
        @Body request: AnonymousAuthRequest,
    ): AnonymousAuthResponse
}

@Serializable
data class AnonymousAuthRequest(
    @SerialName("installationId")
    val installationId: String,
)

@Serializable
data class AnonymousAuthResponse(
    @SerialName("userId")
    val userId: Long,
    @SerialName("authProvider")
    val authProvider: String,
    @SerialName("installationId")
    val installationId: String,
    @SerialName("newlyCreated")
    val newlyCreated: Boolean,
    @SerialName("createdAt")
    val createdAt: String,
    @SerialName("lastSeenAt")
    val lastSeenAt: String,
)
