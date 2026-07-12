package com.snk.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
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

                searchState == null -> {
                    Text(
                        text = emptyHint,
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
                    searchState.items.forEach { item ->
                        ItemCard(
                            item = item,
                            onRecord = { onCreateRecord(item) },
                            onReport = { onReportItem(item) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemCard(
    item: FoodSearchItem,
    onRecord: () -> Unit,
    onReport: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = item.averageRating.toRatingLabel(),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    item.brand?.takeIf { it.isNotBlank() }?.let { brand ->
                        Text(text = brand, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onRecord,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("记一笔")
                }
                Button(
                    onClick = onReport,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("报错 / 纠错")
                }
            }
        }
    }
}

@Composable
private fun FoodCover(item: FoodSearchItem) {
    ProductImage(
        imageUrl = item.coverImageUrl,
        productName = item.name,
        imageKind = ProductImageKind.PRODUCT,
        size = 96.dp,
    )
}

private fun Double?.toRatingLabel(): String {
    if (this == null) {
        return "全站评分: 暂无评分"
    }
    return "全站评分: ${String.format(Locale.US, "%.1f", this)} / 5"
}
