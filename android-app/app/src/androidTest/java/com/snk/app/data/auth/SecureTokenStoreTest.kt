package com.snk.app.data.auth

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecureTokenStoreTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private lateinit var store: SecureTokenStore

    @Before
    fun setUp() = runBlocking {
        store = SecureTokenStore(context, keyAlias = "snk_test_auth_key")
        store.clear()
    }

    @Test
    fun encryptsRefreshTokenAndRestoresAccountMetadata() = runBlocking {
        val session = StoredAuthenticatedSession(
            account = AuthenticatedAccount(7L, "alice", "USER", false),
            deviceId = "stable-device",
            refreshToken = "refresh-token-plaintext",
        )

        store.save(session)

        assertEquals("refresh-token-plaintext", store.read()?.refreshToken)
        assertEquals("alice", store.read()?.account?.username)
        val disk = context.getSharedPreferences("secure_auth_tokens", 0).all.values.joinToString()
        assertFalse(disk.contains("refresh-token-plaintext"))
    }

    @Test
    fun encryptsApprovalTicketAndKeepsDeviceIdStableAfterSessionClear() = runBlocking {
        val deviceIds = PersistentDeviceIdProvider(context)
        val before = deviceIds.get()
        store.saveApprovalTicket("approval-ticket-plaintext")

        assertEquals("approval-ticket-plaintext", store.readApprovalTicket())
        val disk = context.getSharedPreferences("secure_auth_tokens", 0).all.values.joinToString()
        assertFalse(disk.contains("approval-ticket-plaintext"))

        store.clear()
        assertEquals(before, deviceIds.get())
    }
}
