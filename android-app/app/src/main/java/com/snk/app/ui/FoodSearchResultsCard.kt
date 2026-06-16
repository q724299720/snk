package com.snk.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.snk.app.data.food.FoodSearchItem
import com.snk.app.data.food.FoodSearchResult
import java.util.Locale

@Composable
fun FoodSearchResultsCard(
    searchState: FoodSearchResult?,
    isSearching: Boolean,
    emptyHint: String,
    onCreateRecord: (FoodSearchItem) -> Unit,
    onReportItem: (FoodSearchItem) -> Unit,
    noResultActionLabel: String? = null,
    onNoResultAction: (() -> Unit)? = null,
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
            Text(
                text = "搜索结果",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            when {
                isSearching -> {
                    Text(
                        text = "正在请求服务端搜索...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF5B4A42),
                    )
                }

                searchState == null -> {
                    Text(
                        text = emptyHint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF5B4A42),
                    )
                }

                searchState is FoodSearchResult.Failure -> {
                    Text(
                        text = searchState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF8A2E1C),
                    )
                }

                searchState is FoodSearchResult.Success && searchState.items.isEmpty() -> {
                    Text(
                        text = "没有命中结果，可以手动创建一个待审核条目继续记录。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF8A5A44),
                    )
                    if (noResultActionLabel != null && onNoResultAction != null) {
                        Button(
                            onClick = onNoResultAction,
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text(noResultActionLabel)
                        }
                    }
                }

                searchState is FoodSearchResult.Success -> {
                    Text(
                        text = "质量信号: ${searchState.qualitySignal}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF5B4A42),
                    )
                    searchState.items.forEach { item ->
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    FoodCover(item = item)
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        if (item.auditStatus != "approved") {
                                            Text(
                                                text = "待审核条目",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color(0xFFB53A1A),
                                            )
                                        }
                                        Text(
                                            text = item.averageRating.toRatingLabel(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF8A5A44),
                                        )
                                        Text(
                                            text = buildString {
                                                append(item.category)
                                                item.subcategory?.takeIf { it.isNotBlank() }?.let {
                                                    append(" / ")
                                                    append(it)
                                                }
                                                item.brand?.takeIf { it.isNotBlank() }?.let {
                                                    append(" / ")
                                                    append(it)
                                                }
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF5B4A42),
                                        )
                                    }
                                }
                                Button(
                                    onClick = { onCreateRecord(item) },
                                    shape = RoundedCornerShape(14.dp),
                                ) {
                                    Text("记一笔")
                                }
                                Button(
                                    onClick = { onReportItem(item) },
                                    shape = RoundedCornerShape(14.dp),
                                ) {
                                    Text("报错 / 纠错")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FoodCover(item: FoodSearchItem) {
    val shape = RoundedCornerShape(16.dp)
    if (!item.coverImageUrl.isNullOrBlank()) {
        AsyncImage(
            model = item.coverImageUrl,
            contentDescription = item.name,
            modifier = Modifier
                .size(96.dp)
                .clip(shape),
            contentScale = ContentScale.Crop,
        )
        return
    }

    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(shape)
            .background(Color(0xFFF2E3D3)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "暂无图片",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF8A5A44),
        )
    }
}

private fun Double?.toRatingLabel(): String {
    if (this == null) {
        return "全站评分: 暂无评分"
    }
    return "全站评分: ${String.format(Locale.US, "%.1f", this)} / 5"
}
