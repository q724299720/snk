package com.snk.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import kotlinx.coroutines.launch

@Composable
fun DraftsScreen() {
    val application = LocalContext.current.applicationContext as SnkApplication
    val coroutineScope = rememberCoroutineScope()
    val drafts by application.container.draftRecordRepository.observeDrafts().collectAsState(initial = emptyList())
    val hasSynced = drafts.any { it.syncStatus == DraftSyncStatus.SYNCED }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "离线草稿",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2B1E18),
                )
                if (hasSynced) {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                application.container.draftRecordRepository.deleteAllSynced()
                            }
                        },
                    ) {
                        Text("清空已同步", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        if (drafts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF8F2)),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "草稿箱为空",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "弱网提交失败时，会自动把记录转存到这里，并在网络恢复后补传。",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF5B4A42),
                        )
                    }
                }
            }
        } else {
            items(drafts, key = { it.id }) { draft ->
                DraftItemCard(
                    draft = draft,
                    onRetry = {
                        coroutineScope.launch {
                            application.container.draftRecordRepository.requestRetry(draft.id)
                            application.container.scheduleDraftRetry(draft.id)
                        }
                    },
                    onDelete = {
                        coroutineScope.launch {
                            application.container.draftRecordRepository.deleteDraft(draft.id)
                        }
                    },
                )
            }
        }
    }
}
