package com.snk.app.data.auth

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface AuthApi {
    @POST("/api/auth/register") suspend fun register(@Body request: RegisterRequest): RegistrationResponse
    @GET("/api/auth/registration-status") suspend fun registrationStatus(@Query("ticket") ticket: String): RegistrationStatusResponse
    @POST("/api/auth/login") suspend fun login(@Body request: LoginRequest): TokenPairResponse
    @POST("/api/auth/refresh") suspend fun refresh(@Body request: RefreshRequest): TokenPairResponse
    @POST("/api/auth/logout") suspend fun logout(@Header("Authorization") authorization: String, @Body request: LogoutRequest)
    @GET("/api/auth/me") suspend fun me(@Header("Authorization") authorization: String): CurrentAccountResponse
    @POST("/api/auth/password/change") suspend fun changePassword(
        @Header("Authorization") authorization: String,
        @Body request: PasswordChangeRequest,
    )
    @POST("/api/auth/legacy-claim") suspend fun legacyClaim(
        @Header("Authorization") authorization: String,
        @Body request: LegacyIdentityClaimRequest,
    )
}
