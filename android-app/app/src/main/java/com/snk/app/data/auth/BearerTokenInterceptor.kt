package com.snk.app.data.auth

import okhttp3.Interceptor
import okhttp3.Response

class BearerTokenInterceptor(
    private val sessions: AuthenticatedSessionManager,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = sessions.currentAccessToken()
        val request = if (token == null || chain.request().header("Authorization") != null) {
            chain.request()
        } else {
            chain.request().newBuilder().header("Authorization", "Bearer $token").build()
        }
        return chain.proceed(request)
    }
}
