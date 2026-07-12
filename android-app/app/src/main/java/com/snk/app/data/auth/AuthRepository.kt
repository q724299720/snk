package com.snk.app.data.auth

import java.io.IOException
import kotlinx.serialization.json.Json
import retrofit2.HttpException

class AuthRepository(
    private val api: AuthApi,
    private val tokenStore: SecureTokenStoreContract,
    private val deviceIdProvider: DeviceIdProvider,
    private val sessionManager: AuthenticatedSessionManager? = null,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : LegacyIdentityClaimer {
    @Volatile private var accessToken: String? = null

    fun currentAccessToken(): String? = sessionManager?.currentAccessToken() ?: accessToken

    suspend fun pendingApprovalTicket(): String? = tokenStore.readApprovalTicket()

    suspend fun register(username: String, password: String): AuthResult<RegistrationResponse> = call {
        api.register(RegisterRequest(username.trim(), password)).also {
            tokenStore.saveApprovalTicket(it.approvalTicket)
        }
    }

    suspend fun registrationStatus(ticket: String): AuthResult<RegistrationStatusResponse> = call {
        api.registrationStatus(ticket)
    }

    suspend fun login(username: String, password: String): AuthResult<AuthenticatedAccount> = call {
        val deviceId = deviceIdProvider.get()
        val pair = api.login(LoginRequest(username.trim(), password, deviceId))
        val accountResponse = api.me("Bearer ${pair.accessToken}")
        val account = accountResponse.toAccount()
        tokenStore.save(StoredAuthenticatedSession(account, deviceId, pair.refreshToken))
        accessToken = pair.accessToken
        sessionManager?.publishAccessToken(pair.accessToken, account)
        account
    }

    suspend fun refresh(): AuthResult<AuthenticatedAccount> = call {
        val stored = tokenStore.read() ?: throw MissingSessionException()
        val pair = api.refresh(RefreshRequest(stored.refreshToken, stored.deviceId))
        val accountResponse = api.me("Bearer ${pair.accessToken}")
        val account = accountResponse.toAccount()
        tokenStore.save(StoredAuthenticatedSession(account, stored.deviceId, pair.refreshToken))
        accessToken = pair.accessToken
        sessionManager?.publishAccessToken(pair.accessToken, account)
        account
    }

    suspend fun logout(): AuthResult<Unit> {
        val stored = tokenStore.read()
        return try {
            val token = accessToken
            if (stored != null && token != null) api.logout("Bearer $token", LogoutRequest(stored.deviceId))
            AuthResult.Success(Unit)
        } catch (exception: Exception) {
            failure(exception)
        } finally {
            accessToken = null
            if (sessionManager != null) sessionManager.clearSession() else tokenStore.clear()
        }
    }

    suspend fun changePassword(oldPassword: String, newPassword: String): AuthResult<Unit> = authorizedCall {
        api.changePassword(it, PasswordChangeRequest(oldPassword, newPassword))
    }

    override suspend fun claimLegacyIdentity(installationId: String): AuthResult<Unit> = authorizedCall {
        api.legacyClaim(it, LegacyIdentityClaimRequest(installationId))
    }

    private suspend fun authorizedCall(block: suspend (String) -> Unit): AuthResult<Unit> {
        val token = currentAccessToken() ?: return AuthResult.Failure(AuthErrorCode.AUTH_REQUIRED)
        return call { block("Bearer $token") }
    }

    private suspend fun <T> call(block: suspend () -> T): AuthResult<T> = try {
        AuthResult.Success(block())
    } catch (exception: Exception) {
        failure(exception)
    }

    private fun failure(exception: Exception): AuthResult.Failure = AuthResult.Failure(
        when (exception) {
            is IOException -> AuthErrorCode.NETWORK
            is MissingSessionException -> AuthErrorCode.AUTH_REQUIRED
            is HttpException -> mapProblem(exception)
            else -> AuthErrorCode.UNKNOWN
        },
    )

    private fun mapProblem(exception: HttpException): AuthErrorCode {
        val problem = runCatching {
            json.decodeFromString<ApiProblem>(exception.response()?.errorBody()?.string().orEmpty())
        }.getOrNull()
        return when (problem?.code ?: problem?.title) {
            "AUTH_CREDENTIALS_INVALID", "INVALID_CREDENTIALS" -> AuthErrorCode.INVALID_CREDENTIALS
            "AUTH_ACCOUNT_PENDING", "ACCOUNT_PENDING" -> AuthErrorCode.ACCOUNT_PENDING
            "AUTH_ACCOUNT_REJECTED", "ACCOUNT_REJECTED" -> AuthErrorCode.ACCOUNT_REJECTED
            "AUTH_ACCOUNT_DISABLED", "ACCOUNT_DISABLED" -> AuthErrorCode.ACCOUNT_DISABLED
            "MUST_CHANGE_PASSWORD" -> AuthErrorCode.MUST_CHANGE_PASSWORD
            "TOKEN_EXPIRED", "TOKEN_INVALID" -> AuthErrorCode.TOKEN_EXPIRED
            "AUTH_REQUIRED" -> AuthErrorCode.AUTH_REQUIRED
            "AUTH_RATE_LIMITED" -> AuthErrorCode.RATE_LIMITED
            "LEGACY_IDENTITY_ALREADY_CLAIMED" -> AuthErrorCode.LEGACY_IDENTITY_ALREADY_CLAIMED
            else -> if (exception.code() == 400) AuthErrorCode.VALIDATION else AuthErrorCode.UNKNOWN
        }
    }

    private fun CurrentAccountResponse.toAccount() = AuthenticatedAccount(userId, username, role, mustChangePassword)
    private class MissingSessionException : IllegalStateException()
}

interface SecureTokenStoreContract {
    suspend fun save(session: StoredAuthenticatedSession)
    suspend fun read(): StoredAuthenticatedSession?
    suspend fun clear()
    suspend fun saveApprovalTicket(ticket: String) {}
    suspend fun readApprovalTicket(): String? = null
}
