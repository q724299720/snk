package com.snk.app.data.auth

import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import java.net.Proxy

class AnonymousSessionRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: AnonymousSessionRepository
    private lateinit var store: FakeInstallationIdStore

    @Before
    fun setUp() {
        server = MockWebServer()
        store = FakeInstallationIdStore("installation-1")
        repository = AnonymousSessionRepository(
            api = Retrofit.Builder()
                .baseUrl(server.url("/"))
                .client(
                    OkHttpClient.Builder()
                        .proxy(Proxy.NO_PROXY)
                        .build(),
                )
                .addConverterFactory(
                    Json {
                        ignoreUnknownKeys = true
                        explicitNulls = false
                    }.asConverterFactory("application/json".toMediaType()),
                )
                .build()
                .create(AnonymousAuthApi::class.java),
            installationIdStore = store,
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `ensureSession saves remote session when server succeeds`() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "userId": 42,
                      "authProvider": "anonymous",
                      "installationId": "installation-1",
                      "newlyCreated": true,
                      "createdAt": "2026-06-13T10:00:00Z",
                      "lastSeenAt": "2026-06-13T10:00:00Z"
                    }
                    """.trimIndent(),
                ),
        )

        val result = repository.ensureSession()

        assertTrue(result.toString(), result is AnonymousSessionResult.Remote)
        val session = (result as AnonymousSessionResult.Remote).session
        assertEquals(42L, session.userId)
        assertEquals(42L, store.cachedSession?.userId)
    }

    @Test
    fun `ensureSession falls back to cached session when network fails`() = runTest {
        store.cachedSession = AnonymousSession(
            userId = 99L,
            authProvider = "anonymous",
            installationId = "installation-1",
            newlyCreated = false,
            createdAt = "2026-06-13T10:00:00Z",
            lastSeenAt = "2026-06-13T10:10:00Z",
        )

        server.enqueue(
            MockResponse()
                .setSocketPolicy(SocketPolicy.DISCONNECT_AT_START),
        )

        val result = repository.ensureSession()

        assertTrue(result.toString(), result is AnonymousSessionResult.Cached)
        assertEquals(99L, (result as AnonymousSessionResult.Cached).session.userId)
    }

    private class FakeInstallationIdStore(
        private val installationId: String,
    ) : InstallationIdStoreContract {
        var cachedSession: AnonymousSession? = null

        override suspend fun getOrCreateInstallationId(): String = installationId

        override suspend fun saveSession(session: AnonymousSession) {
            cachedSession = session
        }

        override suspend fun getCachedSession(): AnonymousSession? = cachedSession
    }
}
