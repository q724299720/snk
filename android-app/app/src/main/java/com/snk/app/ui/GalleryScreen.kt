package com.snk.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.snk.app.SnkApplication
import com.snk.app.data.record.FoodRecordHistoryItem
import com.snk.app.data.record.FoodRecordHistoryResult
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 20

@Composable
fun GalleryScreen(
    sessionUserId: Long?,
) {
    val application = LocalContext.current.applicationContext as SnkApplication
    val coroutineScope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<FoodRecordHistoryItem>>(emptyList()) }
    var currentPage by remember { mutableStateOf(0) }
    var hasMore by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }

    fun loadPage(page: Int) {
        if (sessionUserId == null) return
        coroutineScope.launch {
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
            isLoading = false
        }
    }

    if (sessionUserId == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("游客身份尚未初始化。", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF5B4A42))
        }
        return
    }

    if (items.isEmpty() && !isLoading) {
        loadPage(0)
    }

    val gridState = rememberLazyStaggeredGridState()

    val shouldLoadMore = remember {
        derivedStateOf {
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
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalItemSpacing = 10.dp,
            modifier = Modifier.fillMaxSize(),
        ) {
            items(items, key = { it.id }) { record ->
                GalleryItemCard(record = record)
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
                    Text(
                        text = loadError.orEmpty(),
                        modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8A2E1C),
                    )
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
private fun GalleryItemCard(record: FoodRecordHistoryItem) {
    val displayImageUrl = record.images.firstOrNull()?.thumbnailUrl
        ?: record.images.firstOrNull()?.imageUrl
        ?: record.foodCoverImageUrl

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF8F2)),
    ) {
        Column {
            if (!displayImageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = displayImageUrl,
                    contentDescription = record.foodName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(Color(0xFFF2E3D3)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("无图", style = MaterialTheme.typography.bodySmall, color = Color(0xFF8A5A44))
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
                    text = "评分 ${record.rating}/5",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8A5A44),
                )
            }
        }
    }
}
