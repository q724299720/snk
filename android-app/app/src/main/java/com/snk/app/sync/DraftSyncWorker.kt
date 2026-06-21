package com.snk.app.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.snk.app.SnkApplication
import com.snk.app.data.draft.DraftFailureReason
import com.snk.app.data.draft.DraftRecordRepository
import com.snk.app.data.record.FoodRecordCreateFailureReason
import com.snk.app.data.record.FoodRecordCreateResult

class DraftSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val draftId = inputData.getLong(KEY_DRAFT_ID, -1L)
        if (draftId <= 0) {
            return Result.failure()
        }

        val application = applicationContext as? SnkApplication ?: return Result.failure()
        val container = application.container
        val draft = container.draftRecordRepository.getDraftPayload(draftId) ?: return Result.success()

        container.draftRecordRepository.markSyncing(draftId)

        return when (
            val result = container.foodRecordRepository.createRecord(
                userId = draft.userId,
                foodItemId = draft.foodItemId,
                rating = draft.rating,
                comment = draft.comment,
                sourceType = draft.sourceType,
                isPublic = draft.isPublic,
                images = emptyList(),
            )
        ) {
            is FoodRecordCreateResult.Success -> {
                container.draftRecordRepository.markSynced(
                    draftId = draftId,
                    retryCount = draft.retryCount,
                    remoteRecordId = result.recordId,
                    remoteRecordTime = result.recordTime,
                )
                Result.success()
            }

            is FoodRecordCreateResult.Failure -> {
                handleFailure(
                    draftId = draftId,
                    retryCount = draft.retryCount,
                    reason = result.reason,
                    message = result.message,
                    repository = container.draftRecordRepository,
                )
            }
        }
    }

    private suspend fun handleFailure(
        draftId: Long,
        retryCount: Int,
        reason: FoodRecordCreateFailureReason,
        message: String,
        repository: DraftRecordRepository,
    ): Result {
        return when (reason) {
            FoodRecordCreateFailureReason.NETWORK -> {
                val nextRetryCount = retryCount + 1
                if (nextRetryCount >= DraftSyncScheduler.MAX_AUTO_RETRY_COUNT) {
                    repository.markFailed(
                        draftId = draftId,
                        retryCount = nextRetryCount,
                        failureReason = DraftFailureReason.NETWORK,
                        failureMessage = "已达到自动补传上限，请手动重试。",
                    )
                    Result.success()
                } else {
                    repository.markRetryPending(
                        draftId = draftId,
                        retryCount = nextRetryCount,
                        failureReason = DraftFailureReason.NETWORK,
                        failureMessage = "网络恢复后会自动重试（$nextRetryCount/${DraftSyncScheduler.MAX_AUTO_RETRY_COUNT}）。",
                    )
                    Result.retry()
                }
            }

            FoodRecordCreateFailureReason.SERVER -> {
                repository.markFailed(
                    draftId = draftId,
                    retryCount = retryCount,
                    failureReason = DraftFailureReason.SERVICE,
                    failureMessage = message,
                )
                Result.success()
            }

            FoodRecordCreateFailureReason.UNKNOWN -> {
                repository.markFailed(
                    draftId = draftId,
                    retryCount = retryCount,
                    failureReason = DraftFailureReason.UNKNOWN,
                    failureMessage = message,
                )
                Result.success()
            }
        }
    }

    companion object {
        const val KEY_DRAFT_ID = "draft_id"
    }
}
