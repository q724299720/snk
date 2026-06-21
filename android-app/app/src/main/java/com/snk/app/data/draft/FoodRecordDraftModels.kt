package com.snk.app.data.draft

data class FoodRecordDraft(
    val id: Long,
    val userId: Long,
    val foodItemId: Long,
    val foodName: String,
    val category: String,
    val subcategory: String?,
    val brand: String?,
    val barcode: String?,
    val rating: Int,
    val comment: String,
    val sourceType: String,
    val isPublic: Boolean,
    val syncStatus: DraftSyncStatus,
    val retryCount: Int,
    val failureReason: DraftFailureReason?,
    val failureMessage: String?,
    val remoteRecordId: Long?,
    val remoteRecordTime: String?,
    val createdAt: Long,
    val updatedAt: Long,
) {
    val statusLabel: String
        get() = when (syncStatus) {
            DraftSyncStatus.DRAFT -> "待上传"
            DraftSyncStatus.SYNCING -> "补传中"
            DraftSyncStatus.SYNCED -> "已同步"
            DraftSyncStatus.FAILED -> "补传失败"
        }
}

data class FoodRecordDraftCreateRequest(
    val userId: Long,
    val foodItemId: Long,
    val foodName: String,
    val category: String,
    val subcategory: String?,
    val brand: String?,
    val barcode: String?,
    val rating: Int,
    val comment: String,
    val sourceType: String,
    val isPublic: Boolean,
)

data class FoodRecordDraftPayload(
    val id: Long,
    val userId: Long,
    val foodItemId: Long,
    val rating: Int,
    val comment: String,
    val sourceType: String,
    val isPublic: Boolean,
    val retryCount: Int,
)

enum class DraftSyncStatus {
    DRAFT,
    SYNCING,
    SYNCED,
    FAILED,
}

enum class DraftFailureReason {
    NETWORK,
    IMAGE,
    SERVICE,
    UNKNOWN,
}

fun DraftFailureReason.toUserFacingText(): String = when (this) {
    DraftFailureReason.NETWORK -> "网络不稳定，等待自动补传。"
    DraftFailureReason.IMAGE -> "图片资源不可用，需要重新选择。"
    DraftFailureReason.SERVICE -> "服务端暂时拒绝了这条草稿。"
    DraftFailureReason.UNKNOWN -> "出现了未知错误，可稍后手动重试。"
}
