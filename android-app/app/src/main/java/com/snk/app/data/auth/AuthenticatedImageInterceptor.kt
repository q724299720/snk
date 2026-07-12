package com.snk.app.data.auth

import okhttp3.Interceptor
import okhttp3.Response

class AuthenticatedImageInterceptor(
    private val trustedHost: String,
    private val accessTokenProvider: () -> String?,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = accessTokenProvider()?.takeIf { it.isNotBlank() }
        val shouldAuthenticate = token != null &&
            original.header("Authorization") == null &&
            original.url.host.equals(trustedHost, ignoreCase = true) &&
            original.url.encodedPath.startsWith("/uploads/")
        val request = if (shouldAuthenticate) {
            original.newBuilder().header("Authorization", "Bearer $token").build()
        } else {
            original
        }
        return chain.proceed(request)
    }
}
