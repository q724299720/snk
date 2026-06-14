package com.snk.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.snk.app.data.food.FoodSearchItem
import com.snk.app.data.food.FoodSearchResult

@Composable
fun FoodSearchResultsCard(
    searchState: FoodSearchResult?,
    isSearching: Boolean,
    emptyHint: String,
    onCreateRecord: (FoodSearchItem) -> Unit,
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
                                item.barcode?.takeIf { it.isNotBlank() }?.let { barcode ->
                                    Text(
                                        text = "条码: $barcode",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF8A5A44),
                                    )
                                }
                                Button(
                                    onClick = { onCreateRecord(item) },
                                    shape = RoundedCornerShape(14.dp),
                                ) {
                                    Text("记一笔")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
