package com.snk.app.data.auth

import java.net.Proxy
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AuthenticatedImageInterceptorTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer().apply { enqueue(MockResponse().setResponseCode(200)) }
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun `trusted upload image receives the current access token`() {
        execute(
            trustedHost = server.hostName,
            path = "/uploads/images/2026/07/photo.jpg",
            token = "access-token",
        )

        assertEquals("Bearer access-token", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `non-upload URL never receives the access token`() {
        execute(trustedHost = server.hostName, path = "/actuator/health", token = "access-token")

        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `untrusted host never receives the access token`() {
        execute(trustedHost = "snk.qiuxinmin.cn", path = "/uploads/images/photo.jpg", token = "access-token")

        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `existing authorization header is preserved`() {
        execute(
            trustedHost = server.hostName,
            path = "/uploads/images/photo.jpg",
            token = "new-token",
            authorization = "Bearer existing-token",
        )

        assertEquals("Bearer existing-token", server.takeRequest().getHeader("Authorization"))
    }

    private fun execute(
        trustedHost: String,
        path: String,
        token: String?,
        authorization: String? = null,
    ) {
        val client = OkHttpClient.Builder()
            .proxy(Proxy.NO_PROXY)
            .addInterceptor(AuthenticatedImageInterceptor(trustedHost) { token })
            .build()
        val request = Request.Builder().url(server.url(path)).apply {
            authorization?.let { header("Authorization", it) }
        }.build()
        client.newCall(request).execute().close()
    }
}
