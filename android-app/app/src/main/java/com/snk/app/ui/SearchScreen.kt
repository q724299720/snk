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
import com.snk.app.BuildConfig
import com.snk.app.SnkApplication
import com.snk.app.data.food.FoodSearchItem
import com.snk.app.data.food.FoodReportResult
import com.snk.app.data.food.FoodSearchResult
import kotlinx.coroutines.launch

private val recentQueries = listOf("乐事黄瓜味", "抹茶蛋糕", "拿铁", "牛肉汉堡")

@Composable
fun SearchScreen(
    sessionState: SessionUiState,
    onCreateRecord: (FoodSearchItem) -> Unit,
    onOpenBarcodeScanner: () -> Unit,
    onOpenOcrRecognition: () -> Unit,
    onOpenManualCreate: (String) -> Unit,
) {
    val application = LocalContext.current.applicationContext as SnkApplication
    val coroutineScope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var searchState by remember { mutableStateOf<FoodSearchResult?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    var reportMessage by remember { mutableStateOf<String?>(null) }

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
            text = "当前已打通游客身份和远程搜索，接下来从命中结果直接创建记录。",
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
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = { submitSearch(query) },
                shape = RoundedCornerShape(18.dp),
            ) {
                Text(if (isSearching) "搜索中..." else "搜索")
            }
            Button(
                onClick = onOpenBarcodeScanner,
                shape = RoundedCornerShape(18.dp),
            ) {
                Text("扫码录入")
            }
            Button(
                onClick = onOpenOcrRecognition,
                shape = RoundedCornerShape(18.dp),
            ) {
                Text("图片识别")
            }
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
                    text = "服务端连接",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Base URL: ${BuildConfig.API_BASE_URL}",
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
        FoodSearchResultsCard(
            searchState = searchState,
            isSearching = isSearching,
            emptyHint = "输入名称后即可查询已审核的基础食物条目。",
            onCreateRecord = onCreateRecord,
            onReportItem = { item ->
                val userId = when (sessionState) {
                    is SessionUiState.Remote -> sessionState.session.userId
                    is SessionUiState.Cached -> sessionState.session.userId
                    else -> null
                }
                if (userId == null) {
                    reportMessage = "游客身份尚未初始化，暂时无法提交纠错。"
                } else {
                    coroutineScope.launch {
                        reportMessage = when (
                            val result = application.container.foodSearchRepository.reportFoodItem(
                                userId = userId,
                                foodItemId = item.id,
                            )
                        ) {
                            is FoodReportResult.Success -> "已提交纠错信号，当前 reportCount = ${result.reportCount}"
                            is FoodReportResult.Failure -> result.message
                        }
                    }
                }
            },
            noResultActionLabel = if (query.isNotBlank()) "没有找到？手动创建" else null,
            onNoResultAction = if (query.isNotBlank()) {
                { onOpenManualCreate(query) }
            } else {
                null
            },
        )
        reportMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF8A5A44),
            )
        }
    }
}
