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
        val ownerUserId = inputData.getLong(KEY_DRAFT_OWNER_USER_ID, -1L)
        if (draftId <= 0 || ownerUserId <= 0) return Result.failure()

        val application = applicationContext as? SnkApplication ?: return Result.failure()
        val container = application.container
        if (!isCurrentOwner(container, ownerUserId)) return Result.retry()
        val draft = container.draftRecordRepository.getDraftPayload(draftId, ownerUserId) ?: return Result.success()

        container.draftRecordRepository.markSyncing(draftId, ownerUserId)

        val images = when (val localPath = draft.localImagePath) {
            null -> emptyList()
            else -> {
                val file = File(localPath)
                if (!file.exists()) {
                    container.draftRecordRepository.markFailed(
                        draftId, ownerUserId, draft.retryCount, DraftFailureReason.IMAGE,
                        "本地图片已不可用，请编辑草稿后重试。",
                    )
                    return Result.success()
                }
                if (!isCurrentOwner(container, ownerUserId)) return pauseForSignIn(container.draftRecordRepository, draftId, ownerUserId, draft.retryCount)
                when (val upload = container.foodRecordRepository.uploadRecordImage(
                    file.readBytes(), file.name, "image/jpeg",
                )) {
                    is RecordImageUploadResult.Success -> listOf(upload.image)
                    is RecordImageUploadResult.Failure -> {
                        if (upload.isAuthenticationFailure) {
                            return pauseForSignIn(container.draftRecordRepository, draftId, ownerUserId, draft.retryCount)
                        }
                        container.draftRecordRepository.markRetryPending(
                            draftId, ownerUserId, draft.retryCount + 1, DraftFailureReason.IMAGE, upload.message,
                        )
                        return Result.retry()
                    }
                }
            }
        }

        val selectedRating = draft.rating
        if (selectedRating == null) {
            container.draftRecordRepository.markFailed(
                draftId, ownerUserId, draft.retryCount, DraftFailureReason.UNKNOWN, "请先选择评分再上传。",
            )
            return Result.success()
        }
        if (!isCurrentOwner(container, ownerUserId)) return pauseForSignIn(container.draftRecordRepository, draftId, ownerUserId, draft.retryCount)

        val result = if (draft.foodItemId == null) {
            container.foodRecordRepository.createQuickRecord(
                clientRequestId = draft.clientRequestId, userId = ownerUserId, name = draft.foodName,
                rating = selectedRating, comment = draft.comment, isPublic = draft.isPublic, images = images,
            )
        } else {
            container.foodRecordRepository.createRecord(
                clientRequestId = draft.clientRequestId, userId = ownerUserId, foodItemId = draft.foodItemId,
                rating = selectedRating, comment = draft.comment, sourceType = draft.sourceType,
                isPublic = draft.isPublic, images = images,
            )
        }

        return when (result) {
            is FoodRecordCreateResult.Success -> {
                container.draftRecordRepository.markSynced(
                    draftId, ownerUserId, draft.retryCount, result.recordId, result.recordTime,
                )
                draft.localImagePath?.let { File(it).delete() }
                Result.success()
            }
            is FoodRecordCreateResult.Failure -> handleFailure(
                draftId, ownerUserId, draft.retryCount, result.reason, result.message, container.draftRecordRepository,
            )
        }
    }

    private fun isCurrentOwner(container: com.snk.app.data.AppContainer, ownerUserId: Long): Boolean =
        DraftSyncOwnershipPolicy.canSync(ownerUserId, container.authenticatedSessionManager.currentUserId())

    private suspend fun pauseForSignIn(
        repository: DraftRecordRepository,
        draftId: Long,
        ownerUserId: Long,
        retryCount: Int,
    ): Result {
        repository.markRetryPending(
            draftId, ownerUserId, retryCount, DraftFailureReason.AUTH,
            "请重新登录原账号后继续上传这条草稿。",
        )
        return Result.retry()
    }

    private suspend fun handleFailure(
        draftId: Long,
        ownerUserId: Long,
        retryCount: Int,
        reason: FoodRecordCreateFailureReason,
        message: String,
        repository: DraftRecordRepository,
    ): Result = when (reason) {
        FoodRecordCreateFailureReason.AUTH -> pauseForSignIn(repository, draftId, ownerUserId, retryCount)
        FoodRecordCreateFailureReason.NETWORK -> {
            val nextRetryCount = retryCount + 1
            if (nextRetryCount >= DraftSyncScheduler.MAX_AUTO_RETRY_COUNT) {
                repository.markFailed(
                    draftId, ownerUserId, nextRetryCount, DraftFailureReason.NETWORK,
                    "已达到自动补传上限，请手动重试。",
                )
                Result.success()
            } else {
                repository.markRetryPending(
                    draftId, ownerUserId, nextRetryCount, DraftFailureReason.NETWORK,
                    "网络恢复后会自动重试（$nextRetryCount/${DraftSyncScheduler.MAX_AUTO_RETRY_COUNT}）。",
                )
                Result.retry()
            }
        }
        FoodRecordCreateFailureReason.SERVER -> {
            repository.markFailed(draftId, ownerUserId, retryCount, DraftFailureReason.SERVICE, message)
            Result.success()
        }
        FoodRecordCreateFailureReason.UNKNOWN -> {
            repository.markFailed(draftId, ownerUserId, retryCount, DraftFailureReason.UNKNOWN, message)
            Result.success()
        }
    }

    companion object {
        const val KEY_DRAFT_ID = "draft_id"
        const val KEY_DRAFT_OWNER_USER_ID = "draft_owner_user_id"
    }
}
