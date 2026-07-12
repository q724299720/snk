package com.snk.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.semantics.semantics
import com.snk.app.SnkApplication
import com.snk.app.data.record.FoodRecordHistoryItem
import com.snk.app.data.record.FoodRecordHistoryResult
import com.snk.app.data.record.FoodRecordLikeResult
import com.snk.app.ui.theme.ChiliRed
import com.snk.app.ui.theme.MutedText
import com.snk.app.ui.theme.Paper
import com.snk.app.ui.theme.PeachSurface
import kotlinx.coroutines.launch

private enum class DiscoverFilter(val label: String) { RECOMMENDED("推荐"), LATEST("最新"), TOP_RATED("高分") }

@Composable
fun DiscoverScreen() {
    val application = LocalContext.current.applicationContext as SnkApplication
    val scope = rememberCoroutineScope()
    var records by remember { mutableStateOf<List<FoodRecordHistoryItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var filter by remember { mutableStateOf(DiscoverFilter.RECOMMENDED) }

    fun load() {
        scope.launch {
            loading = true
            error = null
            when (val result = application.container.foodRecordRepository.listPublicRecords(20)) {
                is FoodRecordHistoryResult.Success -> {
                    records = result.items
                }
                is FoodRecordHistoryResult.Failure -> error = result.message
            }
            loading = false
        }
    }
    LaunchedEffect(Unit) { load() }
    val visibleRecords = remember(records, filter) {
        when (filter) {
            DiscoverFilter.RECOMMENDED -> records.sortedByDescending { it.likeCount + it.rating }
            DiscoverFilter.LATEST -> records.sortedByDescending { it.recordTime }
            DiscoverFilter.TOP_RATED -> records.sortedByDescending { it.rating }
        }
    }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().background(Paper),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalItemSpacing = 12.dp,
    ) {
        item(span = StaggeredGridItemSpan.FullLine) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("发现美食", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("分享美味，遇见同好", style = MaterialTheme.typography.bodyMedium, color = MutedText)
            }
        }
        item(span = StaggeredGridItemSpan.FullLine) {
            Row(modifier = Modifier.semantics { selectableGroup() }, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                DiscoverFilter.entries.forEach { item ->
                    Column(
                        modifier = Modifier
                            .width(58.dp)
                            .heightIn(min = 48.dp)
                            .selectable(selected = filter == item, role = Role.Tab) { filter = item }
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            item.label,
                            color = if (filter == item) ChiliRed else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (filter == item) FontWeight.Bold else FontWeight.Medium,
                        )
                        if (filter == item) Box(Modifier.padding(top = 5.dp).size(width = 24.dp, height = 2.dp).background(ChiliRed))
                    }
                }
            }
        }
        if (loading && records.isEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine) { Text("正在加载公开记录…", color = MutedText) }
        } else if (error != null && records.isEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine) {
                Card(colors = CardDefaults.cardColors(containerColor = PeachSurface)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(error.orEmpty())
                        Button(onClick = { load() }) { Icon(Icons.Outlined.Refresh, null); Text("重试") }
                    }
                }
            }
        } else if (records.isEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine) { Text("暂无公开记录，先分享第一份美味吧。", color = MutedText) }
        } else {
            items(visibleRecords, key = { it.id }) { record ->
                DiscoverRecordCard(record) { newCount ->
                    records = records.map { if (it.id == record.id) it.copy(likeCount = newCount) else it }
                }
            }
            item(span = StaggeredGridItemSpan.FullLine) {
                Text("已展示最新公开记录", modifier = Modifier.fillMaxWidth(), color = MutedText)
            }
        }
    }
}

@Composable
private fun DiscoverRecordCard(record: FoodRecordHistoryItem, onLiked: (Int) -> Unit) {
    val application = LocalContext.current.applicationContext as SnkApplication
    val scope = rememberCoroutineScope()
    var liking by remember(record.id) { mutableStateOf(false) }
    var message by remember(record.id) { mutableStateOf<String?>(null) }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Paper),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column {
            ProductImageFill(
                imageUrl = record.preferredDiscoverImageUrl(),
                productName = record.foodName,
                imageKind = ProductImageKind.RECORD,
                modifier = Modifier.fillMaxWidth().aspectRatio(if (record.id % 2L == 0L) 1.05f else 0.88f).clip(RoundedCornerShape(16.dp)).background(PeachSurface),
            )
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(record.foodName, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                record.comment?.takeIf(String::isNotBlank)?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MutedText, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.clip(CircleShape).background(PeachSurface).padding(5.dp))
                    Text(" 美食记录", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MutedText)
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).heightIn(min = 48.dp).widthIn(min = 48.dp).clickable(enabled = !liking) {
                            scope.launch {
                                liking = true
                                when (val result = application.container.foodRecordRepository.likeRecord(record.id)) {
                                    is FoodRecordLikeResult.Success -> onLiked(result.likeCount)
                                    is FoodRecordLikeResult.Failure -> message = result.message
                                }
                                liking = false
                            }
                        }.padding(horizontal = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.FavoriteBorder, "点赞", tint = ChiliRed, modifier = Modifier.padding(end = 3.dp))
                        Text(record.likeCount.toString(), style = MaterialTheme.typography.labelSmall, color = MutedText)
                    }
                    Icon(
                        Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "评论",
                        tint = MutedText,
                        modifier = Modifier.size(48.dp).clickable {
                            message = "评论功能将在记录详情中打开"
                        }.padding(12.dp),
                    )
                }
                message?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MutedText) }
            }
        }
    }
}

internal fun FoodRecordHistoryItem.preferredDiscoverImageUrl(): String? =
    images.firstOrNull()?.thumbnailUrl ?: images.firstOrNull()?.imageUrl ?: foodCoverImageUrl
