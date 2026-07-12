package com.snk.app.data.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable data class RegisterRequest(val username: String, val password: String) {
    override fun toString() = "RegisterRequest(username=$username,password=<redacted>)"
}
@Serializable data class LoginRequest(val username: String, val password: String, val deviceId: String) {
    override fun toString() = "LoginRequest(username=$username,password=<redacted>,deviceId=$deviceId)"
}
@Serializable data class RefreshRequest(val refreshToken: String, val deviceId: String) {
    override fun toString() = "RefreshRequest(refreshToken=<redacted>,deviceId=$deviceId)"
}
@Serializable data class LogoutRequest(val deviceId: String)
@Serializable data class PasswordChangeRequest(val oldPassword: String, val newPassword: String) {
    override fun toString() = "PasswordChangeRequest(oldPassword=<redacted>,newPassword=<redacted>)"
}
@Serializable data class LegacyIdentityClaimRequest(val installationId: String)

@Serializable
class RegistrationResponse(
    val userId: Long,
    val username: String,
    val role: String,
    val accountStatus: String,
    val approvalTicket: String,
) {
    override fun toString() = "RegistrationResponse(userId=$userId,username=$username,role=$role,accountStatus=$accountStatus,approvalTicket=<redacted>)"
}

@Serializable data class RegistrationStatusResponse(val accountStatus: String)

@Serializable
class TokenPairResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
) {
    override fun toString() = "TokenPairResponse(accessToken=<redacted>,refreshToken=<redacted>,tokenType=$tokenType,expiresIn=$expiresIn)"
}

@Serializable data class CurrentAccountResponse(
    val userId: Long,
    val username: String,
    val role: String,
    val mustChangePassword: Boolean,
)

@Serializable internal data class ApiProblem(
    val code: String? = null,
    val title: String? = null,
)

data class AuthenticatedAccount(
    val userId: Long,
    val username: String,
    val role: String,
    val mustChangePassword: Boolean,
)

class StoredAuthenticatedSession(
    val account: AuthenticatedAccount,
    val deviceId: String,
    val refreshToken: String,
) {
    override fun toString() = "StoredAuthenticatedSession(account=$account,deviceId=$deviceId,refreshToken=<redacted>)"
}

enum class AuthErrorCode {
    INVALID_CREDENTIALS, ACCOUNT_PENDING, ACCOUNT_REJECTED, ACCOUNT_DISABLED,
    MUST_CHANGE_PASSWORD, TOKEN_EXPIRED, AUTH_REQUIRED, RATE_LIMITED,
    LEGACY_IDENTITY_ALREADY_CLAIMED, VALIDATION, NETWORK, UNKNOWN,
}

sealed interface AuthResult<out T> {
    class Success<T>(val value: T) : AuthResult<T> {
        override fun toString() = "AuthResult.Success(value=$value)"
    }
    class Failure(val code: AuthErrorCode) : AuthResult<Nothing> {
        override fun toString() = "AuthResult.Failure(code=$code)"
    }
}

fun interface DeviceIdProvider { suspend fun get(): String }
