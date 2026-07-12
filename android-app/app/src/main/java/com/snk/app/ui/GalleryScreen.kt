package com.snk.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.snk.app.SnkApplication
import com.snk.app.data.record.FoodRecordHistoryItem
import com.snk.app.data.record.FoodRecordHistoryResult
import com.snk.app.data.draft.DraftSyncStatus
import com.snk.app.ui.theme.ChiliRed
import com.snk.app.ui.theme.MutedText
import com.snk.app.ui.theme.Paper
import com.snk.app.ui.theme.PeachSurface
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 20

@Composable
fun GalleryScreen(
    sessionUserId: Long?,
    refreshToken: Int,
    onEditRecord: (FoodRecordHistoryItem) -> Unit,
) {
    val application = LocalContext.current.applicationContext as SnkApplication
    val coroutineScope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<FoodRecordHistoryItem>>(emptyList()) }
    var currentPage by remember { mutableStateOf(0) }
    var hasMore by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var selectedFilter by remember { mutableStateOf("全部") }
    val drafts by application.container.draftRecordRepository.observeDrafts().collectAsState(initial = emptyList())
    val pendingDrafts = remember(drafts) { drafts.filter { it.syncStatus != DraftSyncStatus.SYNCED } }

    fun loadPage(page: Int) {
        if (sessionUserId == null) return
        coroutineScope.launch {
            try {
                isLoading = true
                loadError = null
                when (val result = application.container.foodRecordRepository.listRecentRecords(sessionUserId, page, PAGE_SIZE)) {
                    is FoodRecordHistoryResult.Success -> {
                        if (page == 0) {
                            items = result.items
                        } else {
                            items = items + result.items
                        }
                        hasMore = result.items.size >= PAGE_SIZE
                        currentPage = page
                    }
                    is FoodRecordHistoryResult.Failure -> {
                        loadError = result.message
                    }
                }
            } finally {
                isLoading = false
            }
        }
    }

    if (sessionUserId == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("游客身份尚未初始化。", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF5B4A42))
        }
        return
    }

    LaunchedEffect(sessionUserId, refreshToken) {
        loadPage(0)
    }

    val gridState = rememberLazyStaggeredGridState()

    val shouldLoadMore = remember {
        derivedStateOf {
            if (items.isEmpty()) return@derivedStateOf false
            val lastVisibleItem = gridState.layoutInfo.visibleItemsInfo.lastOrNull()
            if (lastVisibleItem == null) false
            else lastVisibleItem.index >= items.size - 3
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && hasMore && !isLoading) {
            loadPage(currentPage + 1)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalStaggeredGrid(
            state = gridState,
            columns = StaggeredGridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalItemSpacing = 10.dp,
            modifier = Modifier.fillMaxSize(),
        ) {
            item(span = StaggeredGridItemSpan.FullLine) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("我的记录", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("每一餐，都是生活的注脚", style = MaterialTheme.typography.bodyMedium, color = MutedText)
                }
            }
            item(span = StaggeredGridItemSpan.FullLine) {
                androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("全部", "待处理").forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PeachSurface,
                                selectedLabelColor = ChiliRed,
                            ),
                        )
                    }
                }
            }
            if (selectedFilter == "待处理" || pendingDrafts.isNotEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Text("需要处理 · ${pendingDrafts.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                if (pendingDrafts.isEmpty()) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Card(colors = CardDefaults.cardColors(containerColor = PeachSurface), shape = RoundedCornerShape(16.dp)) {
                            Text("当前没有需要处理的记录", modifier = Modifier.padding(14.dp), color = MutedText)
                        }
                    }
                } else {
                    items(pendingDrafts, key = { "draft-${it.id}" }, span = { StaggeredGridItemSpan.FullLine }) { draft ->
                        DraftItemCard(
                            draft = draft,
                            onRetry = {
                                coroutineScope.launch {
                                    application.container.draftRecordRepository.requestRetry(draft.id)
                                    application.container.scheduleDraftRetry(draft.id)
                                }
                            },
                            onDelete = { coroutineScope.launch { application.container.draftRecordRepository.deleteDraft(draft.id) } },
                        )
                    }
                }
            }
            if (selectedFilter == "全部") {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Text("已保存 · ${items.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                if (!isLoading && loadError == null && items.isEmpty()) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Text("还没有已保存记录。", style = MaterialTheme.typography.bodyMedium, color = MutedText)
                    }
                }
                items(items, key = { it.id }) { record ->
                    GalleryItemCard(record = record, onEditRecord = { onEditRecord(record) })
                }
            }
            if (isLoading) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Text(
                        text = "加载中...",
                        modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8A5A44),
                    )
                }
            }
            if (loadError != null) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = loadError.orEmpty(),
                            modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8A2E1C),
                        )
                        Button(onClick = { loadPage(0) }) { Text("重试") }
                    }
                }
            }
            if (hasMore && !isLoading) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "上拉加载更多",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9E8E84),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun GalleryItemCard(record: FoodRecordHistoryItem, onEditRecord: () -> Unit) {
    val displayImageUrl = record.images.firstOrNull()?.thumbnailUrl
        ?: record.images.firstOrNull()?.imageUrl
        ?: record.foodCoverImageUrl

    Card(
        modifier = Modifier.clickable(role = Role.Button, onClickLabel = "编辑记录", onClick = onEditRecord),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Paper),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column {
            if (!displayImageUrl.isNullOrBlank()) {
                ProductImageFill(
                    imageUrl = displayImageUrl,
                    productName = record.foodName,
                    imageKind = ProductImageKind.RECORD,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(Color(0xFFF2E3D3)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("暂无图片", style = MaterialTheme.typography.bodySmall, color = MutedText)
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = record.foodName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${"★".repeat(record.rating.coerceIn(0, 5))}${"☆".repeat((5 - record.rating).coerceIn(0, 5))}  ${record.rating}/5",
                    style = MaterialTheme.typography.bodySmall,
                    color = ChiliRed,
                )
                Text(
                    text = if (record.isPublic) "已公开" else "仅自己",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (record.isPublic) Color(0xFF2E7D32) else Color(0xFF6D4C41),
                )
            }
        }
    }
}
