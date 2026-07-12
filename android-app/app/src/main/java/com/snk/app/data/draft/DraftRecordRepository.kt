package com.snk.app.data.draft

import com.snk.app.data.auth.AuthenticatedSessionManager
import com.snk.app.data.local.FoodRecordDraftDao
import com.snk.app.data.local.FoodRecordDraftEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class DraftRecordRepository(
    private val draftDao: FoodRecordDraftDao,
    private val sessionManager: AuthenticatedSessionManager,
) : DraftRecordSaver {
    fun observeDrafts(): Flow<List<FoodRecordDraft>> {
        val ownerUserId = sessionManager.currentUserId() ?: return flowOf(emptyList())
        return draftDao.observeAll(ownerUserId).map { drafts -> drafts.map { it.toModel() } }
    }

    override suspend fun createDraft(request: FoodRecordDraftCreateRequest): FoodRecordDraft {
        val ownerUserId = sessionManager.requireUserId()
        val now = System.currentTimeMillis()
        val draftId = draftDao.insert(
            FoodRecordDraftEntity(
                // The caller may hold a stale compatibility userId. The signed-in account wins.
                userId = ownerUserId,
                draftOwnerUserId = ownerUserId,
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
                syncStatus = if (request.rating == null) DraftSyncStatus.EDITING.name else DraftSyncStatus.QUEUED.name,
                retryCount = 0,
                failureReason = DraftFailureReason.NETWORK.name,
                failureMessage = "当前无法连接服务端，已转存草稿等待补传。",
                remoteRecordId = null,
                remoteRecordTime = null,
                createdAt = now,
                updatedAt = now,
                clientRequestId = request.clientRequestId,
                localImagePath = request.localImagePath,
            ),
        )
        return requireNotNull(getDraft(draftId))
    }

    suspend fun getDraft(draftId: Long): FoodRecordDraft? =
        sessionManager.currentUserId()?.let { getDraft(draftId, it) }

    suspend fun getDraft(draftId: Long, ownerUserId: Long): FoodRecordDraft? =
        draftDao.findById(draftId, ownerUserId)?.toModel()

    suspend fun getDraftPayload(draftId: Long, ownerUserId: Long): FoodRecordDraftPayload? =
        draftDao.findById(draftId, ownerUserId)?.toPayload()

    suspend fun markSyncing(draftId: Long, ownerUserId: Long) {
        val draft = draftDao.findById(draftId, ownerUserId) ?: return
        draftDao.updateSyncState(
            draftId = draftId,
            ownerUserId = ownerUserId,
            syncStatus = DraftSyncStatus.SYNCING.name,
            retryCount = draft.retryCount,
            failureReason = draft.failureReason,
            failureMessage = draft.failureMessage,
            updatedAt = System.currentTimeMillis(),
        )
    }

    suspend fun markRetryPending(
        draftId: Long,
        ownerUserId: Long,
        retryCount: Int,
        failureReason: DraftFailureReason,
        failureMessage: String,
    ) = updateState(draftId, ownerUserId, DraftSyncStatus.QUEUED, retryCount, failureReason, failureMessage)

    suspend fun markFailed(
        draftId: Long,
        ownerUserId: Long,
        retryCount: Int,
        failureReason: DraftFailureReason,
        failureMessage: String,
    ) = updateState(draftId, ownerUserId, DraftSyncStatus.FAILED, retryCount, failureReason, failureMessage)

    suspend fun markSynced(
        draftId: Long,
        ownerUserId: Long,
        retryCount: Int,
        remoteRecordId: Long,
        remoteRecordTime: String,
    ) {
        draftDao.markSynced(
            draftId = draftId,
            ownerUserId = ownerUserId,
            syncStatus = DraftSyncStatus.SYNCED.name,
            retryCount = retryCount,
            remoteRecordId = remoteRecordId,
            remoteRecordTime = remoteRecordTime,
            updatedAt = System.currentTimeMillis(),
        )
    }

    suspend fun deleteDraft(draftId: Long) {
        sessionManager.currentUserId()?.let { draftDao.deleteById(draftId, it) }
    }

    suspend fun deleteAllSynced() {
        sessionManager.currentUserId()?.let { draftDao.deleteAllByStatus(DraftSyncStatus.SYNCED.name, it) }
    }

    suspend fun requestRetry(draftId: Long) {
        val ownerUserId = sessionManager.currentUserId() ?: return
        val draft = draftDao.findById(draftId, ownerUserId) ?: return
        draftDao.updateSyncState(
            draftId = draftId,
            ownerUserId = ownerUserId,
            syncStatus = DraftSyncStatus.QUEUED.name,
            retryCount = draft.retryCount,
            failureReason = draft.failureReason,
            failureMessage = "已加入重试队列，等待网络可用时补传。",
            updatedAt = System.currentTimeMillis(),
        )
    }

    suspend fun reassignLegacyDrafts(legacyOwnerUserId: Long, targetOwnerUserId: Long): Int =
        draftDao.reassignOwner(legacyOwnerUserId, targetOwnerUserId)

    private suspend fun updateState(
        draftId: Long,
        ownerUserId: Long,
        status: DraftSyncStatus,
        retryCount: Int,
        failureReason: DraftFailureReason,
        failureMessage: String,
    ) {
        draftDao.updateSyncState(
            draftId = draftId,
            ownerUserId = ownerUserId,
            syncStatus = status.name,
            retryCount = retryCount,
            failureReason = failureReason.name,
            failureMessage = failureMessage,
            updatedAt = System.currentTimeMillis(),
        )
    }

    private fun FoodRecordDraftEntity.toModel() = FoodRecordDraft(
        id = id, userId = userId, ownerUserId = draftOwnerUserId, foodItemId = foodItemId,
        foodName = foodName, category = category, subcategory = subcategory, brand = brand,
        barcode = barcode, rating = rating, comment = comment, sourceType = sourceType,
        isPublic = isPublic, syncStatus = DraftSyncStatus.valueOf(syncStatus), retryCount = retryCount,
        failureReason = failureReason?.let(DraftFailureReason::valueOf), failureMessage = failureMessage,
        remoteRecordId = remoteRecordId, remoteRecordTime = remoteRecordTime, createdAt = createdAt,
        updatedAt = updatedAt, clientRequestId = clientRequestId, localImagePath = localImagePath,
    )

    private fun FoodRecordDraftEntity.toPayload() = FoodRecordDraftPayload(
        id = id, userId = userId, ownerUserId = draftOwnerUserId, foodItemId = foodItemId,
        foodName = foodName, rating = rating, comment = comment, sourceType = sourceType,
        isPublic = isPublic, retryCount = retryCount, clientRequestId = clientRequestId,
        localImagePath = localImagePath,
    )
}
