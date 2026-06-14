package com.snk.app.sync

interface DraftSyncTrigger {
    fun scheduleDraftSync(draftId: Long)
}
