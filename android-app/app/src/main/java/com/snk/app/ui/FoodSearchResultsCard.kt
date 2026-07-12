package com.snk.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.snk.app.data.food.FoodSearchItem
import com.snk.app.data.food.FoodSearchResult
import java.util.Locale
import com.snk.app.ui.theme.ChiliRed
import com.snk.app.ui.theme.MutedText
import com.snk.app.ui.theme.Paper
import com.snk.app.ui.theme.PeachSurface

@Composable
fun FoodSearchResultsCard(
    searchState: FoodSearchResult?,
    isSearching: Boolean,
    onCreateRecord: (FoodSearchItem) -> Unit,
    onReportItem: (FoodSearchItem) -> Unit,
    noResultActionLabel: String? = null,
    onNoResultAction: (() -> Unit)? = null,
) {
    if (searchState == null && !isSearching) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Paper),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
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
                    )
                }

                searchState is FoodSearchResult.Failure -> {
                    Text(
                        text = searchState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                searchState is FoodSearchResult.Success && searchState.items.isEmpty() -> {
                    Text(
                        text = "没有命中结果，可以手动创建一个条目继续记录。",
                        style = MaterialTheme.typography.bodyMedium,
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
                    searchState.items.chunked(2).forEach { rowItems ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            rowItems.forEach { item ->
                                ItemCard(
                                    item = item,
                                    modifier = Modifier.weight(1f),
                                    onRecord = { onCreateRecord(item) },
                                    onReport = { onReportItem(item) },
                                )
                            }
                            if (rowItems.size == 1) androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemCard(
    item: FoodSearchItem,
    modifier: Modifier,
    onRecord: () -> Unit,
    onReport: () -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Paper),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ProductImageFill(
                imageUrl = item.coverImageUrl,
                productName = item.name,
                imageKind = ProductImageKind.PRODUCT,
                modifier = Modifier.fillMaxWidth().aspectRatio(1.15f).clip(RoundedCornerShape(18.dp)),
            )
            Column(modifier = Modifier.padding(horizontal = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = item.averageRating.toRatingLabel(),
                        style = MaterialTheme.typography.bodySmall,
                        color = ChiliRed,
                    )
                    item.brand?.takeIf { it.isNotBlank() }?.let { brand ->
                        Text(text = brand, style = MaterialTheme.typography.bodySmall, color = MutedText)
                    }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onRecord,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("记一笔")
                }
                TextButton(
                    onClick = onReport,
                ) {
                    Text("纠错", style = MaterialTheme.typography.labelSmall, color = MutedText)
                }
            }
        }
    }
}

private fun Double?.toRatingLabel(): String {
    if (this == null) {
        return "全站评分: 暂无评分"
    }
    return "全站评分: ${String.format(Locale.US, "%.1f", this)} / 5"
}
