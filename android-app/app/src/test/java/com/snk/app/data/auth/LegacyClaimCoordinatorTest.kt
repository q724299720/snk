package com.snk.app.data.auth

import com.snk.app.data.draft.LegacyDraftOwnerMigrator
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class LegacyClaimCoordinatorTest {
    @Test
    fun `successful claim reassigns local legacy drafts before marking resolution`() = runTest {
        val store = FakeLegacyClaimStore(LegacyIdentity(userId = 19, installationId = "legacy-installation"))
        val migrator = FakeMigrator()
        val coordinator = LegacyClaimCoordinator(
            remote = LegacyIdentityClaimer { AuthResult.Success(Unit) },
            installationStore = store,
            draftMigrator = migrator,
            currentUserId = CurrentUserIdProvider { 77 },
        )

        assertEquals(LegacyClaimAvailability.AVAILABLE, coordinator.availability())
        assertEquals(LegacyClaimResult.CLAIMED, coordinator.claim())
        assertEquals(listOf(19L to 77L), migrator.moves)
        assertEquals(true, store.resolved)
        assertEquals(LegacyClaimAvailability.HIDDEN, coordinator.availability())
    }

    @Test
    fun `network failure leaves the legacy identity available for a later retry`() = runTest {
        val store = FakeLegacyClaimStore(LegacyIdentity(userId = 19, installationId = "legacy-installation"))
        val coordinator = LegacyClaimCoordinator(
            remote = LegacyIdentityClaimer { AuthResult.Failure(AuthErrorCode.NETWORK) },
            installationStore = store,
            draftMigrator = FakeMigrator(),
            currentUserId = CurrentUserIdProvider { 77 },
        )

        assertEquals(LegacyClaimResult.RETRYABLE_FAILURE, coordinator.claim())
        assertEquals(false, store.resolved)
        assertEquals(LegacyClaimAvailability.AVAILABLE, coordinator.availability())
    }

    private class FakeLegacyClaimStore(
        private val identity: LegacyIdentity?,
    ) : InstallationIdStoreContract {
        var resolved = false
        var pending: LegacyIdentity? = null
        override suspend fun getOrCreateInstallationId() = identity?.installationId ?: "new"
        override suspend fun saveSession(session: AnonymousSession) = Unit
        override suspend fun getCachedSession(): AnonymousSession? = null
        override suspend fun readLegacyIdentity() = identity
        override suspend fun isLegacyClaimResolved() = resolved
        override suspend fun readPendingLocalMigration() = pending
        override suspend fun markServerClaimedPendingLocalMigration(identity: LegacyIdentity) { pending = identity }
        override suspend fun markLegacyClaimResolved() { resolved = true; pending = null }
    }

    private class FakeMigrator : LegacyDraftOwnerMigrator {
        val moves = mutableListOf<Pair<Long, Long>>()
        override suspend fun reassignLegacyDrafts(legacyOwnerUserId: Long, targetOwnerUserId: Long): Int {
            moves += legacyOwnerUserId to targetOwnerUserId
            return 1
        }
    }
}
