package com.snk.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.snk.app.SnkApplication
import com.snk.app.data.record.FoodRecordHistoryItem
import com.snk.app.data.record.FoodRecordHistoryResult
import com.snk.app.data.record.FoodRecordLikeResult
import kotlinx.coroutines.launch

@Composable
fun DiscoverScreen() {
    val application = LocalContext.current.applicationContext as SnkApplication
    val scope = rememberCoroutineScope()
    var records by remember { mutableStateOf<List<FoodRecordHistoryItem>>(emptyList()) }
    var limit by remember { mutableIntStateOf(10) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var hasMore by remember { mutableStateOf(true) }

    fun load(requestedLimit: Int) {
        scope.launch {
            loading = true
            error = null
            when (val result = application.container.foodRecordRepository.listPublicRecords(requestedLimit)) {
                is FoodRecordHistoryResult.Success -> {
                    records = result.items
                    hasMore = result.items.size >= requestedLimit
                    limit = requestedLimit
                }
                is FoodRecordHistoryResult.Failure -> error = result.message
            }
            loading = false
        }
    }
    LaunchedEffect(Unit) { load(limit) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("发现", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("看看大家公开分享的美食记录", style = MaterialTheme.typography.bodyMedium)
        }
        if (loading && records.isEmpty()) {
            item { Text("正在加载公开记录…") }
        } else if (error != null && records.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFECE8))) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(error.orEmpty())
                        Button(onClick = { load(limit) }) { Text("重试") }
                    }
                }
            }
        } else if (records.isEmpty()) {
            item { Text("暂无公开记录，稍后再来看看。") }
        } else {
            items(records, key = { it.id }) { record ->
                DiscoverRecordCard(record) { newCount ->
                    records = records.map { if (it.id == record.id) it.copy(likeCount = newCount) else it }
                }
            }
            item {
                if (hasMore) {
                    OutlinedButton(
                        onClick = { load(limit + 10) },
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (loading) "加载中…" else "加载更多") }
                } else {
                    Text("已经到底了", modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun DiscoverRecordCard(record: FoodRecordHistoryItem, onLiked: (Int) -> Unit) {
    val application = LocalContext.current.applicationContext as SnkApplication
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF8F2)),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ProductImage(
                    imageUrl = record.preferredDiscoverImageUrl(),
                    productName = record.foodName,
                    imageKind = ProductImageKind.RECORD,
                    size = 96.dp,
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(record.foodName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${record.rating}/5 · ${record.foodBrand ?: "待补充品牌"}")
                    record.comment?.takeIf(String::isNotBlank)?.let { Text(it) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    scope.launch {
                        when (val result = application.container.foodRecordRepository.likeRecord(record.id)) {
                            is FoodRecordLikeResult.Success -> onLiked(result.likeCount)
                            is FoodRecordLikeResult.Failure -> message = result.message
                        }
                    }
                }) { Text("点赞 ${record.likeCount}") }
                OutlinedButton(onClick = { message = "评论入口已迁入发现页，详情评论将在下一层打开。" }) {
                    Text("评论")
                }
            }
            message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

internal fun FoodRecordHistoryItem.preferredDiscoverImageUrl(): String? =
    images.firstOrNull()?.thumbnailUrl
        ?: images.firstOrNull()?.imageUrl
        ?: foodCoverImageUrl
