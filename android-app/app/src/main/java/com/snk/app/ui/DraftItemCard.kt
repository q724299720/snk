package com.snk.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.snk.app.data.draft.DraftSyncStatus
import com.snk.app.data.draft.FoodRecordDraft
import com.snk.app.data.draft.toUserFacingText

/**
 * 单条离线草稿卡片，首页「待上传 / 失败重试」分区与「草稿」Tab 共用，避免两处分叉。
 */
@Composable
fun DraftItemCard(
    draft: FoodRecordDraft,
    onRetry: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF8F2)),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = draft.foodName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterStart),
                )
                Text(
                    text = draft.statusLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF8A5A44),
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
            Text(
                text = buildString {
                    append("评分 ${draft.rating} / 5")
                    draft.barcode?.takeIf { it.isNotBlank() }?.let {
                        append(" · 条码 ")
                        append(it)
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF5B4A42),
            )
            if (draft.comment.isNotBlank()) {
                Text(
                    text = draft.comment,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF5B4A42),
                )
            }
            Text(
                text = draft.failureMessage ?: draft.failureReason?.toUserFacingText() ?: "等待同步状态更新。",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF5B4A42),
            )
            if (draft.syncStatus == DraftSyncStatus.SYNCED && draft.remoteRecordId != null) {
                Text(
                    text = "remote record_id: ${draft.remoteRecordId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF7A6A61),
                )
            }
            if (draft.syncStatus == DraftSyncStatus.FAILED) {
                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("手动重试")
                }
            }
        }
    }
}
