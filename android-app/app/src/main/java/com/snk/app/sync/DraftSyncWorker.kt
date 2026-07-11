package com.snk.app.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.snk.app.SnkApplication
import com.snk.app.data.draft.DraftFailureReason
import com.snk.app.data.draft.DraftRecordRepository
import com.snk.app.data.record.FoodRecordCreateFailureReason
import com.snk.app.data.record.FoodRecordCreateResult
import com.snk.app.data.record.RecordImageUploadResult
import java.io.File

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

        val images = when (val localPath = draft.localImagePath) {
            null -> emptyList()
            else -> {
                val file = File(localPath)
                if (!file.exists()) {
                    container.draftRecordRepository.markFailed(
                        draftId,
                        draft.retryCount,
                        DraftFailureReason.IMAGE,
                        "本地图片已不可用，请编辑草稿后重试。",
                    )
                    return Result.success()
                }
                when (val upload = container.foodRecordRepository.uploadRecordImage(
                    file.readBytes(),
                    file.name,
                    "image/jpeg",
                )) {
                    is RecordImageUploadResult.Success -> listOf(upload.image)
                    is RecordImageUploadResult.Failure -> {
                        container.draftRecordRepository.markRetryPending(
                            draftId,
                            draft.retryCount + 1,
                            DraftFailureReason.IMAGE,
                            upload.message,
                        )
                        return Result.retry()
                    }
                }
            }
        }

        val selectedRating = draft.rating
        if (selectedRating == null) {
            container.draftRecordRepository.markFailed(
                draftId,
                draft.retryCount,
                DraftFailureReason.UNKNOWN,
                "请先选择评分再上传。",
            )
            return Result.success()
        }

        val result = if (draft.foodItemId == null) {
            container.foodRecordRepository.createQuickRecord(
                clientRequestId = draft.clientRequestId,
                userId = draft.userId,
                name = draft.foodName,
                rating = selectedRating,
                comment = draft.comment,
                isPublic = draft.isPublic,
                images = images,
            )
        } else {
            container.foodRecordRepository.createRecord(
                clientRequestId = draft.clientRequestId,
                userId = draft.userId,
                foodItemId = draft.foodItemId,
                rating = selectedRating,
                comment = draft.comment,
                sourceType = draft.sourceType,
                isPublic = draft.isPublic,
                images = images,
            )
        }

        return when (result) {
            is FoodRecordCreateResult.Success -> {
                container.draftRecordRepository.markSynced(
                    draftId = draftId,
                    retryCount = draft.retryCount,
                    remoteRecordId = result.recordId,
                    remoteRecordTime = result.recordTime,
                )
                draft.localImagePath?.let { File(it).delete() }
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
