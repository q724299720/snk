package com.snk.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DraftItemCard(
    draft: FoodRecordDraft,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
) {
    val statusColor = when (draft.syncStatus) {
        DraftSyncStatus.EDITING -> Color(0xFF6D4C41)
        DraftSyncStatus.DRAFT, DraftSyncStatus.QUEUED -> Color(0xFFE65100)
        DraftSyncStatus.SYNCING -> Color(0xFF1565C0)
        DraftSyncStatus.SYNCED -> Color(0xFF2E7D32)
        DraftSyncStatus.FAILED -> Color(0xFFB71C1C)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF8F2)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = draft.foodName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2B1E18),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = when (draft.syncStatus) {
                        DraftSyncStatus.EDITING -> "✎ ${draft.statusLabel}"
                        DraftSyncStatus.DRAFT, DraftSyncStatus.QUEUED -> "◷ ${draft.statusLabel}"
                        DraftSyncStatus.SYNCING -> "↻ ${draft.statusLabel}"
                        DraftSyncStatus.SYNCED -> "✓ ${draft.statusLabel}"
                        DraftSyncStatus.FAILED -> "! ${draft.statusLabel}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${draft.category}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF7A6A61),
                )
                draft.brand?.takeIf { it.isNotBlank() }?.let {
                    Text(text = "·", style = MaterialTheme.typography.bodySmall, color = Color(0xFF7A6A61))
                    Text(text = it, style = MaterialTheme.typography.bodySmall, color = Color(0xFF7A6A61))
                }
                Text(text = "·", style = MaterialTheme.typography.bodySmall, color = Color(0xFF7A6A61))
                Text(
                    text = draft.rating?.let { "$it/5" } ?: "待评分",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF7A6A61),
                )
            }
            if (draft.comment.isNotBlank()) {
                Text(
                    text = draft.comment,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5B4A42),
                    maxLines = 2,
                )
            }
            val failText = draft.failureMessage ?: draft.failureReason?.toUserFacingText()
            if (failText != null && draft.syncStatus != DraftSyncStatus.SYNCED) {
                Text(
                    text = failText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8A2E1C),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatDraftTime(draft.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF9E8E84),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (draft.syncStatus == DraftSyncStatus.FAILED ||
                        draft.syncStatus == DraftSyncStatus.DRAFT ||
                        draft.syncStatus == DraftSyncStatus.QUEUED
                    ) {
                        OutlinedButton(
                            onClick = onRetry,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        ) {
                            Text("重试", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Button(
                        onClick = onDelete,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0D6CC)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    ) {
                        Text("删除", style = MaterialTheme.typography.labelSmall, color = Color(0xFF5B4A42))
                    }
                }
            }
        }
    }
}

private fun formatDraftTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
