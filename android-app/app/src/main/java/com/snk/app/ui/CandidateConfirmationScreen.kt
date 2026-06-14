package com.snk.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
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

data class CandidateConfirmationState(
    val sourceLabel: String,
    val title: String,
    val description: String,
    val items: List<FoodSearchItem>,
    val qualitySignal: String,
    val sourceType: String,
    val recognizedText: String? = null,
    val matchedQuery: String? = null,
    val attemptedQueries: List<String> = emptyList(),
    val manualCreateSeedName: String? = null,
)

@Composable
fun CandidateConfirmationScreen(
    state: CandidateConfirmationState,
    onSelectCandidate: (FoodSearchItem) -> Unit,
    onReportCandidate: (FoodSearchItem) -> Unit,
    onOpenManualCreate: (String) -> Unit,
    onBack: () -> Unit,
    reportMessage: String? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = state.sourceLabel,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = Color(0xFF2B1E18),
        )
        Text(
            text = state.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF3A291F),
        )
        Text(
            text = state.description,
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF5B4A42),
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFCF1E6)),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "候选质量",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = when (state.qualitySignal.lowercase()) {
                        "strong" -> "当前为高置信候选，可以确认后继续记录。"
                        else -> "当前候选置信度偏弱，建议先确认名称和分类；如果不对，可以改为手动创建。"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF5B4A42),
                )
                Text(
                    text = "qualitySignal = ${state.qualitySignal}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8A5A44),
                )
                state.recognizedText?.takeIf { it.isNotBlank() }?.let { text ->
                    Text(
                        text = "识别文本：$text",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF5B4A42),
                    )
                }
                state.matchedQuery?.takeIf { it.isNotBlank() }?.let { query ->
                    Text(
                        text = "命中查询：$query",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF5B4A42),
                    )
                }
                if (state.attemptedQueries.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.attemptedQueries.forEach { query ->
                            AssistChip(
                                onClick = {},
                                enabled = false,
                                label = { Text(query) },
                            )
                        }
                    }
                }
            }
        }
        state.items.forEach { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF8F2)),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
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
                            text = "条码：$barcode",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF8A5A44),
                        )
                    }
                    Button(
                        onClick = { onSelectCandidate(item) },
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text("确认并记一笔")
                    }
                    Button(
                        onClick = { onReportCandidate(item) },
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text("报错 / 纠错")
                    }
                }
            }
        }
        if (
            state.manualCreateSeedName != null &&
            (state.qualitySignal.equals("weak", ignoreCase = true) || state.items.isEmpty())
        ) {
            Button(
                onClick = { onOpenManualCreate(state.manualCreateSeedName) },
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("候选不对，手动创建")
            }
        }
        reportMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF8A5A44),
            )
        }
        Button(
            onClick = onBack,
            shape = RoundedCornerShape(16.dp),
        ) {
            Text("返回上一页")
        }
    }
}
