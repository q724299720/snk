package com.snk.app.data.auth

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.net.Proxy
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

class RefreshTokenAuthenticatorTest {
    private lateinit var server: MockWebServer
    private lateinit var store: FakeTokenStore
    private lateinit var manager: AuthenticatedSessionManager
    private val refreshCount = AtomicInteger()
    private val protectedCount = AtomicInteger()

    @Before
    fun setUp() {
        server = MockWebServer()
        store = FakeTokenStore(session("old-refresh"))
        val publicClient = OkHttpClient.Builder().proxy(Proxy.NO_PROXY).build()
        val authApi = Retrofit.Builder().baseUrl(server.url("/"))
            .client(publicClient)
            .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
            .build().create(AuthApi::class.java)
        manager = AuthenticatedSessionManager(authApi, store)
        manager.publishAccessToken("old-access", account())
        store.accessTokenReader = { manager.currentAccessToken() }
    }

    @After fun tearDown() = server.shutdown()

    @Test
    fun `concurrent 401 responses perform one refresh and all replay with replacement`() = runTest {
        server.dispatcher = successfulRefreshDispatcher()
        val client = businessClient()

        val statuses = (1..8).map {
            async(Dispatchers.IO) { client.newCall(Request.Builder().url(server.url("/protected")).build()).execute().use { it.code } }
        }.awaitAll()

        assertEquals(List(8) { 200 }, statuses)
        assertEquals(1, refreshCount.get())
        assertEquals("new-refresh", store.value?.refreshToken)
        assertEquals("new-access", manager.currentAccessToken())
        assertEquals(listOf("save:new-refresh"), store.events)
        assertEquals("old-access", store.accessTokenObservedDuringSave)
    }

    @Test
    fun `request is replayed only once when replacement access token is also rejected`() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
                "/api/auth/refresh" -> refreshResponse().also { refreshCount.incrementAndGet() }
                "/api/auth/me" -> accountResponse()
                else -> MockResponse().setResponseCode(401).also { protectedCount.incrementAndGet() }
            }
        }

        val status = businessClient().newCall(Request.Builder().url(server.url("/protected")).build()).execute().use { it.code }

        assertEquals(401, status)
        assertEquals(1, refreshCount.get())
        assertEquals(2, protectedCount.get())
    }

    @Test
    fun `refresh failure clears persistent session and stops replay`() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
                "/api/auth/refresh" -> MockResponse().setResponseCode(401)
                    .setHeader("Content-Type", "application/json")
                    .setBody("""{"code":"TOKEN_EXPIRED"}""")
                else -> MockResponse().setResponseCode(401)
            }
        }

        val status = businessClient().newCall(Request.Builder().url(server.url("/protected")).build()).execute().use { it.code }

        assertEquals(401, status)
        assertNull(store.value)
        assertNull(manager.currentAccessToken())
        assertEquals(SessionState.SIGNED_OUT, manager.currentState())
    }

    @Test
    fun `account status failures become explicit session states`() = runTest {
        val cases = listOf(
            "ACCOUNT_PENDING" to SessionState.PENDING,
            "ACCOUNT_REJECTED" to SessionState.REJECTED,
            "ACCOUNT_DISABLED" to SessionState.DISABLED,
            "MUST_CHANGE_PASSWORD" to SessionState.MUST_CHANGE_PASSWORD,
        )
        cases.forEach { (code, expected) ->
            server.enqueue(MockResponse().setResponseCode(403)
                .setHeader("Content-Type", "application/json").setBody("""{"code":"$code"}"""))
            val localStore = FakeTokenStore(session("refresh-$code"))
            val localManager = AuthenticatedSessionManager(createAuthApi(), localStore)
            localManager.publishAccessToken("access-$code", account())

            localManager.refreshAfterUnauthorized("access-$code")

            assertEquals(expected, localManager.currentState())
            assertNull(localStore.value)
        }
    }

    private fun businessClient() = OkHttpClient.Builder().proxy(Proxy.NO_PROXY)
        .addInterceptor(BearerTokenInterceptor(manager))
        .authenticator(RefreshTokenAuthenticator(manager))
        .build()

    private fun createAuthApi(): AuthApi = Retrofit.Builder().baseUrl(server.url("/"))
        .client(OkHttpClient.Builder().proxy(Proxy.NO_PROXY).build())
        .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
        .build().create(AuthApi::class.java)

    private fun successfulRefreshDispatcher() = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
            "/api/auth/refresh" -> refreshResponse().also { refreshCount.incrementAndGet() }
            "/api/auth/me" -> accountResponse()
            "/protected" -> if (request.getHeader("Authorization") == "Bearer new-access") {
                MockResponse().setResponseCode(200)
            } else MockResponse().setResponseCode(401)
            else -> MockResponse().setResponseCode(404)
        }
    }

    private fun refreshResponse() = MockResponse().setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody("""{"accessToken":"new-access","refreshToken":"new-refresh","tokenType":"Bearer","expiresIn":900}""")
    private fun accountResponse() = MockResponse().setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody("""{"userId":7,"username":"alice","role":"USER","mustChangePassword":false}""")
    private fun account() = AuthenticatedAccount(7, "alice", "USER", false)
    private fun session(refresh: String) = StoredAuthenticatedSession(account(), "stable-device", refresh)

    private class FakeTokenStore(initial: StoredAuthenticatedSession?) : SecureTokenStoreContract {
        var value = initial
        val events = mutableListOf<String>()
        var accessTokenReader: () -> String? = { null }
        var accessTokenObservedDuringSave: String? = null
        override suspend fun save(session: StoredAuthenticatedSession) {
            accessTokenObservedDuringSave = accessTokenReader()
            value = session
            events += "save:${session.refreshToken}"
        }
        override suspend fun read() = value
        override suspend fun clear() { value = null }
    }
}
