package com.snk.app.data.draft

interface DraftRecordSaver {
    suspend fun createDraft(request: FoodRecordDraftCreateRequest): FoodRecordDraft
}
