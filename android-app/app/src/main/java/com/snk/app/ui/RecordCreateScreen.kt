package com.snk.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.snk.app.SnkApplication
import com.snk.app.data.food.FoodSearchItem
import com.snk.app.data.food.FoodSearchResult
import com.snk.app.data.record.FoodRecordSubmissionCoordinator
import com.snk.app.data.record.FoodRecordSubmissionResult
import kotlinx.coroutines.launch

@Composable
fun RecordCreateScreen(
    selectedFood: FoodSearchItem,
    sourceType: String,
    sessionState: SessionUiState,
    submissionCoordinator: FoodRecordSubmissionCoordinator,
    onSwitchRecommendedFood: (FoodSearchItem) -> Unit,
    onBackToSearch: () -> Unit,
    onOpenDrafts: () -> Unit,
) {
    val application = LocalContext.current.applicationContext as SnkApplication
    val coroutineScope = rememberCoroutineScope()
    var rating by remember { mutableIntStateOf(4) }
    var comment by remember { mutableStateOf("") }
    var submitState by remember { mutableStateOf<FoodRecordSubmissionResult?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    val relatedFoodState by produceState<FoodSearchResult?>(initialValue = null, key1 = selectedFood.id) {
        value = application.container.foodSearchRepository.recommendRelatedFoods(selectedFood.id)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "新建记录",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = Color(0xFF2B1E18),
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEFBF7)),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = selectedFood.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (selectedFood.auditStatus != "approved") {
                    Text(
                        text = "该条目仍在审核中，当前记录会先绑定到待审核条目。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFB53A1A),
                    )
                }
                Text(
                    text = buildString {
                        append(selectedFood.category)
                        selectedFood.subcategory?.takeIf { it.isNotBlank() }?.let {
                            append(" / ")
                            append(it)
                        }
                        selectedFood.brand?.takeIf { it.isNotBlank() }?.let {
                            append(" / ")
                            append(it)
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF5B4A42),
                )
                selectedFood.barcode?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = "条码: $it",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF8A5A44),
                    )
                }
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F1E7)),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "相似推荐",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                when (val related = relatedFoodState) {
                    null -> Text(
                        text = "正在加载同类食物推荐...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF5B4A42),
                    )

                    is FoodSearchResult.Failure -> Text(
                        text = related.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF8A2E1C),
                    )

                    is FoodSearchResult.Success -> {
                        Text(
                            text = if (related.items.isEmpty()) "暂时没有找到相似条目。" else "可以快速切换到以下相似条目：",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF5B4A42),
                        )
                        related.items.take(3).forEach { item ->
                            Button(
                                onClick = { onSwitchRecommendedFood(item) },
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text(item.name)
                            }
                        }
                    }
                }
            }
        }
        Text(
            text = "评分",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2B1E18),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            (1..5).forEach { value ->
                Button(
                    onClick = { rating = value },
                    shape = RoundedCornerShape(18.dp),
                    enabled = !isSubmitting,
                ) {
                    Text(if (rating == value) "$value 分" else value.toString())
                }
            }
        }
        OutlinedTextField(
            value = comment,
            onValueChange = { comment = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("备注，可选") },
            minLines = 3,
            maxLines = 5,
            shape = RoundedCornerShape(20.dp),
            enabled = !isSubmitting,
        )
        Text(
            text = when (sessionState) {
                SessionUiState.Loading -> "游客身份初始化中，暂时不能提交。"
                is SessionUiState.Remote -> "当前将记录到游客 user_id ${sessionState.session.userId}"
                is SessionUiState.Cached -> "当前离线沿用缓存游客 user_id ${sessionState.session.userId}"
                is SessionUiState.Failure -> sessionState.reason
            },
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF5B4A42),
        )
        Button(
            onClick = {
                val userId = when (sessionState) {
                    is SessionUiState.Remote -> sessionState.session.userId
                    is SessionUiState.Cached -> sessionState.session.userId
                    else -> null
                } ?: return@Button

                coroutineScope.launch {
                    isSubmitting = true
                    submitState = submissionCoordinator.submit(
                        userId = userId,
                        selectedFood = selectedFood,
                        rating = rating,
                        comment = comment,
                        sourceType = sourceType,
                    )
                    isSubmitting = false
                }
            },
            enabled = !isSubmitting && sessionState !is SessionUiState.Loading && sessionState !is SessionUiState.Failure,
            shape = RoundedCornerShape(18.dp),
        ) {
            Text(if (isSubmitting) "保存中..." else "保存记录")
        }
        when (val result = submitState) {
            null -> Unit
            is FoodRecordSubmissionResult.Failure -> {
                Text(
                    text = result.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF8A2E1C),
                )
            }

            is FoodRecordSubmissionResult.Submitted -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFCF1E6)),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "记录已保存",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "record_id: ${result.recordId}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF5B4A42),
                        )
                        Button(
                            onClick = onBackToSearch,
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Text("返回搜索")
                        }
                    }
                }
            }

            is FoodRecordSubmissionResult.SavedToDraft -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F2E8)),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "已转存草稿",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "draft_id: ${result.draft.id}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF5B4A42),
                        )
                        Text(
                            text = "当前无法连接服务端，网络恢复后会自动补传。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF5B4A42),
                        )
                        Button(
                            onClick = onOpenDrafts,
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Text("查看草稿")
                        }
                    }
                }
            }
        }
    }
}
