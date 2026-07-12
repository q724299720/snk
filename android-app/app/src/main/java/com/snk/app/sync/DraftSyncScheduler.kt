package com.snk.app.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

class DraftSyncScheduler(
    private val context: Context,
) : DraftSyncTrigger {
    override fun scheduleDraftSync(draftId: Long, ownerUserId: Long) {
        val request = OneTimeWorkRequestBuilder<DraftSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10,
                TimeUnit.SECONDS,
            )
            .setInputData(
                workDataOf(
                    DraftSyncWorker.KEY_DRAFT_ID to draftId,
                    DraftSyncWorker.KEY_DRAFT_OWNER_USER_ID to ownerUserId,
                ),
            )
            .addTag(TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueWorkName(draftId, ownerUserId),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    companion object {
        const val MAX_AUTO_RETRY_COUNT = 3
        const val TAG = "food-record-draft-sync"

        fun uniqueWorkName(draftId: Long, ownerUserId: Long): String =
            "food-record-draft-sync-$ownerUserId-$draftId"
    }
}
