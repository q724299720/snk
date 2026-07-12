package com.snk.app.data.auth

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.net.Proxy
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

class AuthRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var tokens: FakeSecureTokenStore
    private lateinit var repository: AuthRepository
    private lateinit var api: AuthApi

    @Before
    fun setUp() {
        server = MockWebServer()
        tokens = FakeSecureTokenStore()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().proxy(Proxy.NO_PROXY).build())
            .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
            .build().create(AuthApi::class.java)
        repository = AuthRepository(api, tokens, deviceIdProvider = DeviceIdProvider { "stable-device" })
    }

    @After fun tearDown() = server.shutdown()

    @Test
    fun `register never sends device id and does not treat approval ticket as token`() = runTest {
        server.enqueue(json(201, """{"userId":7,"username":"alice","role":"USER","accountStatus":"PENDING","approvalTicket":"approval-secret"}"""))

        val result = repository.register("alice", "correct-horse-12")

        val request = server.takeRequest()
        assertEquals("/api/auth/register", request.path)
        assertEquals("{\"username\":\"alice\",\"password\":\"correct-horse-12\"}", request.body.readUtf8())
        assertTrue(result is AuthResult.Success)
        assertEquals(null, tokens.saved)
        assertEquals("approval-secret", tokens.approvalTicket)
        assertFalse(result.toString().contains("approval-secret"))
    }

    @Test
    fun `login sends stable device id then stores refresh token and account metadata`() = runTest {
        server.enqueue(json(200, """{"accessToken":"access-secret","refreshToken":"refresh-secret","tokenType":"Bearer","expiresIn":900}"""))
        server.enqueue(json(200, """{"userId":7,"username":"alice","role":"USER","mustChangePassword":false}"""))

        val result = repository.login("alice", "correct-horse-12")

        assertTrue(result is AuthResult.Success)
        assertEquals("{\"username\":\"alice\",\"password\":\"correct-horse-12\",\"deviceId\":\"stable-device\"}", server.takeRequest().body.readUtf8())
        assertEquals("Bearer access-secret", server.takeRequest().getHeader("Authorization"))
        assertEquals("refresh-secret", tokens.saved?.refreshToken)
        assertEquals(7L, tokens.saved?.account?.userId)
        assertEquals("access-secret", repository.currentAccessToken())
        assertFalse(result.toString().contains("access-secret"))
    }

    @Test
    fun `maps stable server problem code without leaking response credentials`() = runTest {
        server.enqueue(json(403, """{"status":403,"title":"ACCOUNT_PENDING","code":"ACCOUNT_PENDING","detail":"approval-secret"}"""))

        val result = repository.login("alice", "wrong-password")

        assertEquals(AuthErrorCode.ACCOUNT_PENDING, (result as AuthResult.Failure).code)
        assertFalse(result.toString().contains("approval-secret"))
        assertFalse(result.toString().contains("wrong-password"))
    }

    @Test
    fun `legacy claim conflict is distinguishable so the prompt is not shown again`() = runTest {
        server.enqueue(json(200, """{"accessToken":"access-secret","refreshToken":"refresh-secret","tokenType":"Bearer","expiresIn":900}"""))
        server.enqueue(json(200, """{"userId":7,"username":"alice","role":"USER","mustChangePassword":false}"""))
        server.enqueue(json(409, """{"status":409,"code":"LEGACY_IDENTITY_ALREADY_CLAIMED"}"""))
        repository.login("alice", "correct-horse-12")

        val result = repository.claimLegacyIdentity("legacy-installation")

        assertEquals(
            AuthErrorCode.LEGACY_IDENTITY_ALREADY_CLAIMED,
            (result as AuthResult.Failure).code,
        )
    }

    @Test
    fun `remaining auth endpoints keep their headers paths and json contracts`() = runTest {
        server.enqueue(json(200, """{"accountStatus":"ACTIVE"}"""))
        api.registrationStatus("approval-ticket")
        assertEquals("/api/auth/registration-status?ticket=approval-ticket", server.takeRequest().path)

        server.enqueue(json(200, """{"accessToken":"a2","refreshToken":"r2","tokenType":"Bearer","expiresIn":900}"""))
        api.refresh(RefreshRequest("r1", "stable-device"))
        assertEquals("{\"refreshToken\":\"r1\",\"deviceId\":\"stable-device\"}", server.takeRequest().body.readUtf8())

        server.enqueue(MockResponse().setResponseCode(204))
        api.logout("Bearer access", LogoutRequest("stable-device"))
        val logout = server.takeRequest()
        assertEquals("/api/auth/logout", logout.path)
        assertEquals("Bearer access", logout.getHeader("Authorization"))

        server.enqueue(json(200, """{"userId":7,"username":"alice","role":"USER","mustChangePassword":false}"""))
        api.me("Bearer access")
        assertEquals("Bearer access", server.takeRequest().getHeader("Authorization"))

        server.enqueue(MockResponse().setResponseCode(204))
        api.changePassword("Bearer access", PasswordChangeRequest("old-secret-12", "new-secret-12"))
        val password = server.takeRequest()
        assertEquals("/api/auth/password/change", password.path)
        assertEquals("{\"oldPassword\":\"old-secret-12\",\"newPassword\":\"new-secret-12\"}", password.body.readUtf8())

        server.enqueue(MockResponse().setResponseCode(204))
        api.legacyClaim("Bearer access", LegacyIdentityClaimRequest("legacy-installation"))
        val claim = server.takeRequest()
        assertEquals("/api/auth/legacy-claim", claim.path)
        assertEquals("{\"installationId\":\"legacy-installation\"}", claim.body.readUtf8())
    }

    private fun json(code: Int, body: String) = MockResponse().setResponseCode(code)
        .setHeader("Content-Type", "application/json").setBody(body)

    private class FakeSecureTokenStore : SecureTokenStoreContract {
        var saved: StoredAuthenticatedSession? = null
        var approvalTicket: String? = null
        override suspend fun save(session: StoredAuthenticatedSession) { saved = session }
        override suspend fun read(): StoredAuthenticatedSession? = saved
        override suspend fun clear() { saved = null }
        override suspend fun saveApprovalTicket(ticket: String) { approvalTicket = ticket }
    }
}
