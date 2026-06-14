package com.snk.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.snk.app.SnkApplication
import com.snk.app.data.draft.DraftSyncStatus
import com.snk.app.data.draft.FoodRecordDraft
import com.snk.app.data.draft.toUserFacingText
import kotlinx.coroutines.launch

@Composable
fun DraftsScreen() {
    val application = LocalContext.current.applicationContext as SnkApplication
    val coroutineScope = rememberCoroutineScope()
    val drafts by application.container.draftRecordRepository.observeDrafts().collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "离线草稿",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = Color(0xFF2B1E18),
        )
        Text(
            text = "这里会承载 Room 草稿、本地图片副本和 WorkManager 补传状态。",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF5B4A42),
        )
        if (drafts.isEmpty()) {
            DraftStatusCard(
                title = "草稿箱为空",
                description = "弱网提交失败时，会自动把记录转存到这里，并在网络恢复后补传。",
            )
        } else {
            drafts.forEach { draft ->
                DraftItemCard(
                    draft = draft,
                    onRetry = {
                        coroutineScope.launch {
                            application.container.draftRecordRepository.requestRetry(draft.id)
                            application.container.scheduleDraftRetry(draft.id)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun DraftStatusCard(
    title: String,
    description: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF8F2)),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF5B4A42),
            )
        }
    }
}

@Composable
private fun DraftItemCard(
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
