package com.snk.app.data.draft

import com.snk.app.data.local.FoodRecordDraftDao
import com.snk.app.data.local.FoodRecordDraftEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DraftRecordRepository(
    private val draftDao: FoodRecordDraftDao,
) : DraftRecordSaver {
    fun observeDrafts(): Flow<List<FoodRecordDraft>> = draftDao.observeAll().map { drafts ->
        drafts.map { it.toModel() }
    }

    override suspend fun createDraft(request: FoodRecordDraftCreateRequest): FoodRecordDraft {
        val now = System.currentTimeMillis()
        val draftId = draftDao.insert(
            FoodRecordDraftEntity(
                userId = request.userId,
                foodItemId = request.foodItemId,
                foodName = request.foodName,
                category = request.category,
                subcategory = request.subcategory,
                brand = request.brand,
                barcode = request.barcode,
                rating = request.rating,
                comment = request.comment.trim(),
                sourceType = request.sourceType,
                isPublic = request.isPublic,
                syncStatus = DraftSyncStatus.DRAFT.name,
                retryCount = 0,
                failureReason = DraftFailureReason.NETWORK.name,
                failureMessage = "当前无法连接服务端，已转存草稿等待补传。",
                remoteRecordId = null,
                remoteRecordTime = null,
                createdAt = now,
                updatedAt = now,
            ),
        )
        return requireNotNull(getDraft(draftId))
    }

    suspend fun getDraft(draftId: Long): FoodRecordDraft? = draftDao.findById(draftId)?.toModel()

    suspend fun getDraftPayload(draftId: Long): FoodRecordDraftPayload? = draftDao.findById(draftId)?.toPayload()

    suspend fun markSyncing(draftId: Long) {
        val draft = draftDao.findById(draftId) ?: return
        draftDao.updateSyncState(
            draftId = draftId,
            syncStatus = DraftSyncStatus.SYNCING.name,
            retryCount = draft.retryCount,
            failureReason = draft.failureReason,
            failureMessage = draft.failureMessage,
            updatedAt = System.currentTimeMillis(),
        )
    }

    suspend fun markRetryPending(
        draftId: Long,
        retryCount: Int,
        failureReason: DraftFailureReason,
        failureMessage: String,
    ) {
        draftDao.updateSyncState(
            draftId = draftId,
            syncStatus = DraftSyncStatus.DRAFT.name,
            retryCount = retryCount,
            failureReason = failureReason.name,
            failureMessage = failureMessage,
            updatedAt = System.currentTimeMillis(),
        )
    }

    suspend fun markFailed(
        draftId: Long,
        retryCount: Int,
        failureReason: DraftFailureReason,
        failureMessage: String,
    ) {
        draftDao.updateSyncState(
            draftId = draftId,
            syncStatus = DraftSyncStatus.FAILED.name,
            retryCount = retryCount,
            failureReason = failureReason.name,
            failureMessage = failureMessage,
            updatedAt = System.currentTimeMillis(),
        )
    }

    suspend fun markSynced(
        draftId: Long,
        retryCount: Int,
        remoteRecordId: Long,
        remoteRecordTime: String,
    ) {
        draftDao.markSynced(
            draftId = draftId,
            syncStatus = DraftSyncStatus.SYNCED.name,
            retryCount = retryCount,
            remoteRecordId = remoteRecordId,
            remoteRecordTime = remoteRecordTime,
            updatedAt = System.currentTimeMillis(),
        )
    }

    suspend fun requestRetry(draftId: Long) {
        val draft = draftDao.findById(draftId) ?: return
        draftDao.updateSyncState(
            draftId = draftId,
            syncStatus = DraftSyncStatus.DRAFT.name,
            retryCount = draft.retryCount,
            failureReason = draft.failureReason,
            failureMessage = "已加入重试队列，等待网络可用时补传。",
            updatedAt = System.currentTimeMillis(),
        )
    }

    private fun FoodRecordDraftEntity.toModel(): FoodRecordDraft = FoodRecordDraft(
        id = id,
        userId = userId,
        foodItemId = foodItemId,
        foodName = foodName,
        category = category,
        subcategory = subcategory,
        brand = brand,
        barcode = barcode,
        rating = rating,
        comment = comment,
        sourceType = sourceType,
        isPublic = isPublic,
        syncStatus = DraftSyncStatus.valueOf(syncStatus),
        retryCount = retryCount,
        failureReason = failureReason?.let(DraftFailureReason::valueOf),
        failureMessage = failureMessage,
        remoteRecordId = remoteRecordId,
        remoteRecordTime = remoteRecordTime,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun FoodRecordDraftEntity.toPayload(): FoodRecordDraftPayload = FoodRecordDraftPayload(
        id = id,
        userId = userId,
        foodItemId = foodItemId,
        rating = rating,
        comment = comment,
        sourceType = sourceType,
        isPublic = isPublic,
        retryCount = retryCount,
    )
}
