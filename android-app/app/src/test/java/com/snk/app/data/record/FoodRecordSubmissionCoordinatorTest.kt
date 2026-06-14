package com.snk.app.data.record

import com.snk.app.data.draft.DraftRecordSaver
import com.snk.app.data.draft.DraftSyncStatus
import com.snk.app.data.draft.FoodRecordDraft
import com.snk.app.data.draft.FoodRecordDraftCreateRequest
import com.snk.app.data.food.FoodSearchItem
import com.snk.app.sync.DraftSyncTrigger
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodRecordSubmissionCoordinatorTest {
    @Test
    fun `submit returns remote success when backend accepts record`() = runTest {
        val coordinator = FoodRecordSubmissionCoordinator(
            remoteWriter = FakeRemoteWriter(
                FoodRecordCreateResult.Success(
                    recordId = 88L,
                    recordTime = "2026-06-14T10:00:00Z",
                ),
            ),
            draftSaver = FakeDraftSaver(),
            draftSyncTrigger = FakeDraftSyncTrigger(),
        )

        val result = coordinator.submit(
            userId = 1L,
            selectedFood = testFood(),
            rating = 4,
            comment = "顺利提交",
        )

        assertTrue(result is FoodRecordSubmissionResult.Submitted)
        assertEquals(88L, (result as FoodRecordSubmissionResult.Submitted).recordId)
    }

    @Test
    fun `submit stores draft and schedules sync when network fails`() = runTest {
        val draftSaver = FakeDraftSaver()
        val syncTrigger = FakeDraftSyncTrigger()
        val coordinator = FoodRecordSubmissionCoordinator(
            remoteWriter = FakeRemoteWriter(
                FoodRecordCreateResult.Failure(
                    reason = FoodRecordCreateFailureReason.NETWORK,
                    message = "无法连接服务端，记录暂时没有提交成功。",
                ),
            ),
            draftSaver = draftSaver,
            draftSyncTrigger = syncTrigger,
        )

        val result = coordinator.submit(
            userId = 1L,
            selectedFood = testFood(),
            rating = 4,
            comment = "转草稿",
        )

        assertTrue(result is FoodRecordSubmissionResult.SavedToDraft)
        assertEquals(1L, draftSaver.savedDrafts.single().id)
        assertEquals(listOf(1L), syncTrigger.scheduledDraftIds)
    }

    private fun testFood(): FoodSearchItem = FoodSearchItem(
        id = 7L,
        name = "乐事黄瓜味薯片",
        itemType = "packaged_product",
        category = "snack",
        subcategory = "chips",
        brand = "乐事",
        barcode = "6900000000011",
        auditStatus = "approved",
    )
}

private class FakeRemoteWriter(
    private val result: FoodRecordCreateResult,
) : RemoteFoodRecordWriter {
    override suspend fun createRecord(
        userId: Long,
        foodItemId: Long,
        rating: Int,
        comment: String,
        sourceType: String,
    ): FoodRecordCreateResult = result
}

private class FakeDraftSaver : DraftRecordSaver {
    val savedDrafts = mutableListOf<FoodRecordDraft>()

    override suspend fun createDraft(request: FoodRecordDraftCreateRequest): FoodRecordDraft {
        val draft = FoodRecordDraft(
            id = (savedDrafts.size + 1).toLong(),
            userId = request.userId,
            foodItemId = request.foodItemId,
            foodName = request.foodName,
            category = request.category,
            subcategory = request.subcategory,
            brand = request.brand,
            barcode = request.barcode,
            rating = request.rating,
            comment = request.comment,
            sourceType = request.sourceType,
            syncStatus = DraftSyncStatus.DRAFT,
            retryCount = 0,
            failureReason = null,
            failureMessage = null,
            remoteRecordId = null,
            remoteRecordTime = null,
            createdAt = 0L,
            updatedAt = 0L,
        )
        savedDrafts += draft
        return draft
    }
}

private class FakeDraftSyncTrigger : DraftSyncTrigger {
    val scheduledDraftIds = mutableListOf<Long>()

    override fun scheduleDraftSync(draftId: Long) {
        scheduledDraftIds += draftId
    }
}
