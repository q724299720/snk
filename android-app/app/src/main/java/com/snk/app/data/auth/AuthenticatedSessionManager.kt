package com.snk.app.data.auth

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import retrofit2.HttpException

enum class SessionState {
    SIGNED_OUT, AUTHENTICATED, PENDING, REJECTED, DISABLED, MUST_CHANGE_PASSWORD,
}

class AuthenticatedSessionManager(
    private val authApi: AuthApi,
    private val tokenStore: SecureTokenStoreContract,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : CurrentUserIdProvider {
    private val refreshMutex = Mutex()
    @Volatile private var accessToken: String? = null
    @Volatile private var account: AuthenticatedAccount? = null
    @Volatile private var state: SessionState = SessionState.SIGNED_OUT

    fun currentAccessToken(): String? = accessToken
    fun currentAccount(): AuthenticatedAccount? = account
    fun currentState(): SessionState = state
    override fun currentUserId(): Long? = account?.userId
    fun requireUserId(): Long = requireNotNull(currentUserId()) { "A signed-in account is required." }

    suspend fun restoreSession(): SessionState {
        refreshAfterUnauthorized(null)
        return state
    }

    fun publishAccessToken(token: String, authenticatedAccount: AuthenticatedAccount) {
        accessToken = token
        account = authenticatedAccount
        state = if (authenticatedAccount.mustChangePassword) SessionState.MUST_CHANGE_PASSWORD else SessionState.AUTHENTICATED
    }

    suspend fun refreshAfterUnauthorized(failedAccessToken: String?): String? = refreshMutex.withLock {
        val published = accessToken
        if (published != null && failedAccessToken != null && published != failedAccessToken) return published
        val stored = tokenStore.read() ?: return clearAndReturnNull(SessionState.SIGNED_OUT)
        try {
            val pair = authApi.refresh(RefreshRequest(stored.refreshToken, stored.deviceId))
            val response = authApi.me("Bearer ${pair.accessToken}")
            val refreshedAccount = AuthenticatedAccount(
                response.userId, response.username, response.role, response.mustChangePassword,
            )
            // Persist the replacement first. A process death can then recover the newest refresh token.
            tokenStore.save(StoredAuthenticatedSession(refreshedAccount, stored.deviceId, pair.refreshToken))
            publishAccessToken(pair.accessToken, refreshedAccount)
            pair.accessToken
        } catch (exception: Exception) {
            clearAndReturnNull(mapFailureState(exception))
        }
    }

    suspend fun clearSession(stateAfterClear: SessionState = SessionState.SIGNED_OUT) {
        refreshMutex.withLock { clearAndReturnNull(stateAfterClear) }
    }

    private suspend fun clearAndReturnNull(nextState: SessionState): String? {
        accessToken = null
        account = null
        state = nextState
        tokenStore.clear()
        return null
    }

    private fun mapFailureState(exception: Exception): SessionState {
        if (exception !is HttpException) return SessionState.SIGNED_OUT
        val code = runCatching {
            val problem = json.decodeFromString<ApiProblem>(exception.response()?.errorBody()?.string().orEmpty())
            problem.code ?: problem.title
        }.getOrNull()
        return when (code) {
            "ACCOUNT_PENDING", "AUTH_ACCOUNT_PENDING" -> SessionState.PENDING
            "ACCOUNT_REJECTED", "AUTH_ACCOUNT_REJECTED" -> SessionState.REJECTED
            "ACCOUNT_DISABLED", "AUTH_ACCOUNT_DISABLED" -> SessionState.DISABLED
            "MUST_CHANGE_PASSWORD" -> SessionState.MUST_CHANGE_PASSWORD
            else -> SessionState.SIGNED_OUT
        }
    }
}
