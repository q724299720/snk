package com.snk.app.sync

/** Prevents a queued WorkManager job from submitting a different account's draft. */
object DraftSyncOwnershipPolicy {
    fun canSync(ownerUserId: Long, currentUserId: Long?): Boolean =
        currentUserId != null && currentUserId == ownerUserId
}
