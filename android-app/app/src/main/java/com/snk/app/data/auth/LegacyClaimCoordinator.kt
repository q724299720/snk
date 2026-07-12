package com.snk.app.data.auth

import com.snk.app.data.draft.LegacyDraftOwnerMigrator

data class LegacyIdentity(
    val userId: Long,
    val installationId: String,
)

fun interface LegacyIdentityClaimer {
    suspend fun claimLegacyIdentity(installationId: String): AuthResult<Unit>
}

fun interface CurrentUserIdProvider {
    fun currentUserId(): Long?
}

enum class LegacyClaimAvailability { HIDDEN, AVAILABLE }
enum class LegacyClaimResult { CLAIMED, ALREADY_CLAIMED, RETRYABLE_FAILURE, NOT_AVAILABLE }

class LegacyClaimCoordinator(
    private val remote: LegacyIdentityClaimer,
    private val installationStore: InstallationIdStoreContract,
    private val draftMigrator: LegacyDraftOwnerMigrator,
    private val currentUserId: CurrentUserIdProvider,
) {
    suspend fun availability(): LegacyClaimAvailability {
        if (currentUserId.currentUserId() == null || installationStore.isLegacyClaimResolved()) {
            return LegacyClaimAvailability.HIDDEN
        }
        if (resumePendingLocalMigration()) return LegacyClaimAvailability.HIDDEN
        return if (installationStore.readLegacyIdentity() == null) {
            LegacyClaimAvailability.HIDDEN
        } else {
            LegacyClaimAvailability.AVAILABLE
        }
    }

    suspend fun claim(): LegacyClaimResult {
        val accountId = currentUserId.currentUserId() ?: return LegacyClaimResult.NOT_AVAILABLE
        val pending = installationStore.readPendingLocalMigration()
        if (pending != null) {
            return if (migrateAndResolve(pending, accountId)) LegacyClaimResult.CLAIMED else LegacyClaimResult.RETRYABLE_FAILURE
        }
        if (installationStore.isLegacyClaimResolved()) return LegacyClaimResult.NOT_AVAILABLE
        val identity = installationStore.readLegacyIdentity() ?: return LegacyClaimResult.NOT_AVAILABLE
        return when (val response = remote.claimLegacyIdentity(identity.installationId)) {
            is AuthResult.Success -> {
                installationStore.markServerClaimedPendingLocalMigration(identity)
                if (migrateAndResolve(identity, accountId)) LegacyClaimResult.CLAIMED else LegacyClaimResult.RETRYABLE_FAILURE
            }
            is AuthResult.Failure -> {
                if (response.code == AuthErrorCode.LEGACY_IDENTITY_ALREADY_CLAIMED) {
                    installationStore.markLegacyClaimResolved()
                    LegacyClaimResult.ALREADY_CLAIMED
                } else {
                    LegacyClaimResult.RETRYABLE_FAILURE
                }
            }
        }
    }

    private suspend fun resumePendingLocalMigration(): Boolean {
        val pending = installationStore.readPendingLocalMigration() ?: return false
        val accountId = currentUserId.currentUserId() ?: return false
        return migrateAndResolve(pending, accountId)
    }

    private suspend fun migrateAndResolve(identity: LegacyIdentity, accountId: Long): Boolean = runCatching {
        draftMigrator.reassignLegacyDrafts(identity.userId, accountId)
        installationStore.markLegacyClaimResolved()
        true
    }.getOrDefault(false)
}
