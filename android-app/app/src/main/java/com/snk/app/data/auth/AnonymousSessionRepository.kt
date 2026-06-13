package com.snk.app.data.auth

import java.io.IOException
import retrofit2.HttpException

class AnonymousSessionRepository(
    private val api: AnonymousAuthApi,
    private val installationIdStore: InstallationIdStoreContract,
) {
    suspend fun ensureSession(): AnonymousSessionResult {
        val installationId = installationIdStore.getOrCreateInstallationId()

        return try {
            val response = api.initializeAnonymousUser(
                AnonymousAuthRequest(installationId = installationId),
            )
            val session = response.toSession()
            installationIdStore.saveSession(session)
            AnonymousSessionResult.Remote(session)
        } catch (exception: Exception) {
            val cachedSession = installationIdStore.getCachedSession()
            if (cachedSession != null && cachedSession.installationId == installationId) {
                AnonymousSessionResult.Cached(
                    session = cachedSession,
                    reason = exception.asUserFacingReason(),
                )
            } else {
                AnonymousSessionResult.Failure(exception.asUserFacingReason())
            }
        }
    }
}

data class AnonymousSession(
    val userId: Long,
    val authProvider: String,
    val installationId: String,
    val newlyCreated: Boolean,
    val createdAt: String,
    val lastSeenAt: String,
)

sealed interface AnonymousSessionResult {
    data class Remote(val session: AnonymousSession) : AnonymousSessionResult
    data class Cached(val session: AnonymousSession, val reason: String) : AnonymousSessionResult
    data class Failure(val reason: String) : AnonymousSessionResult
}

private fun AnonymousAuthResponse.toSession(): AnonymousSession = AnonymousSession(
    userId = userId,
    authProvider = authProvider,
    installationId = installationId,
    newlyCreated = newlyCreated,
    createdAt = createdAt,
    lastSeenAt = lastSeenAt,
)

private fun Exception.asUserFacingReason(): String = when (this) {
    is IOException -> "网络不可用，已尝试回退到本地游客身份缓存。"
    is HttpException -> "服务端返回异常状态，已尝试回退到本地游客身份缓存。"
    else -> "游客身份初始化失败。"
}
