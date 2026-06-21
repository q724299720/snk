package com.snk.app.data.record

import com.snk.app.data.draft.DraftRecordSaver
import com.snk.app.data.draft.FoodRecordDraft
import com.snk.app.data.draft.FoodRecordDraftCreateRequest
import com.snk.app.data.food.FoodSearchItem
import com.snk.app.sync.DraftSyncTrigger

class FoodRecordSubmissionCoordinator(
    private val remoteWriter: RemoteFoodRecordWriter,
    private val draftSaver: DraftRecordSaver,
    private val draftSyncTrigger: DraftSyncTrigger,
) {
    suspend fun submit(
        userId: Long,
        selectedFood: FoodSearchItem,
        rating: Int,
        comment: String,
        sourceType: String = "text_search",
        images: List<FoodRecordImageAttachment> = emptyList(),
    ): FoodRecordSubmissionResult {
        return when (
            val result = remoteWriter.createRecord(
                userId = userId,
                foodItemId = selectedFood.id,
                rating = rating,
                comment = comment,
                sourceType = sourceType,
                images = images,
            )
        ) {
            is FoodRecordCreateResult.Success -> {
                FoodRecordSubmissionResult.Submitted(
                    recordId = result.recordId,
                    recordTime = result.recordTime,
                    likeCount = result.likeCount,
                )
            }

            is FoodRecordCreateResult.Failure -> {
                if (result.reason == FoodRecordCreateFailureReason.NETWORK) {
                    val draft = draftSaver.createDraft(
                        FoodRecordDraftCreateRequest(
                            userId = userId,
                            foodItemId = selectedFood.id,
                            foodName = selectedFood.name,
                            category = selectedFood.category,
                            subcategory = selectedFood.subcategory,
                            brand = selectedFood.brand,
                            barcode = selectedFood.barcode,
                            rating = rating,
                            comment = comment,
                            sourceType = sourceType,
                        ),
                    )
                    draftSyncTrigger.scheduleDraftSync(draft.id)
                    FoodRecordSubmissionResult.SavedToDraft(draft)
                } else {
                    FoodRecordSubmissionResult.Failure(result.message)
                }
            }
        }
    }
}

sealed interface FoodRecordSubmissionResult {
    data class Submitted(
        val recordId: Long,
        val recordTime: String,
        val likeCount: Int,
    ) : FoodRecordSubmissionResult

    data class SavedToDraft(
        val draft: FoodRecordDraft,
    ) : FoodRecordSubmissionResult

    data class Failure(val message: String) : FoodRecordSubmissionResult
}
