package com.snk.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.snk.app.BuildConfig
import com.snk.app.data.food.FoodSearchResult
import kotlinx.coroutines.launch

private val recentQueries = listOf("乐事黄瓜味", "抹茶蛋糕", "拿铁", "牛肉汉堡")

@Composable
fun SearchScreen(sessionState: SessionUiState) {
    val application = LocalContext.current.applicationContext as SnkApplication
    val coroutineScope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var searchState by remember { mutableStateOf<FoodSearchResult?>(null) }
    var isSearching by remember { mutableStateOf(false) }

    fun submitSearch(value: String) {
        query = value
        coroutineScope.launch {
            isSearching = true
            searchState = application.container.foodSearchRepository.search(value)
            isSearching = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "文本搜索",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = Color(0xFF2B1E18),
        )
        Text(
            text = "当前基线版本先打通搜索入口和游客闭环，后续接入服务端真实搜索。",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF5B4A42),
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("输入食品名、品牌或口味") },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            recentQueries.forEach { item ->
                AssistChip(
                    onClick = { submitSearch(item) },
                    label = { Text(item) },
                )
            }
        }
        Button(
            onClick = { submitSearch(query) },
            shape = RoundedCornerShape(18.dp),
        ) {
            Text(if (isSearching) "搜索中..." else "搜索")
        }
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFCF1E6)),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "服务端占位配置",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Base URL: ${BuildConfig.API_BASE_URL}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF5B4A42),
                )
                Text(
                    text = "下一增量会在这里接匿名用户初始化、搜索接口和最近搜索本地缓存。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF5B4A42),
                )
                Text(
                    text = when (sessionState) {
                        SessionUiState.Loading -> "游客身份初始化中..."
                        is SessionUiState.Remote -> "当前游客 user_id: ${sessionState.session.userId}"
                        is SessionUiState.Cached -> "离线沿用缓存游客 user_id: ${sessionState.session.userId}"
                        is SessionUiState.Failure -> sessionState.reason
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF5B4A42),
                )
            }
        }
        SearchResultsCard(
            searchState = searchState,
            isSearching = isSearching,
        )
    }
}

@Composable
private fun SearchResultsCard(
    searchState: FoodSearchResult?,
    isSearching: Boolean,
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
                        text = "输入名称后即可查询已审核的基础食物条目。",
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
                        text = "没有命中结果，后续会在这里补手动创建入口。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF8A5A44),
                    )
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
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
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
                                item.barcode?.takeIf { it.isNotBlank() }?.let { barcode ->
                                    Text(
                                        text = "条码: $barcode",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF8A5A44),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
