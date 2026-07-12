package com.snk.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ProfileScreen(sessionState: SessionUiState, onRetry: () -> Unit) {
    var showDiagnostics by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("我的", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        ProfileInfoCard("账号模式", "当前使用正式账号，记录与图片将同步到服务器。")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF8F2)),
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("同步状态", fontWeight = FontWeight.SemiBold)
                when (sessionState) {
                    is SessionUiState.Authenticated -> Text("已登录，记录会自动同步。")
                    SessionUiState.Loading -> Text("正在连接服务端…")
                    is SessionUiState.Remote -> Text("已连接，记录会自动同步。")
                    is SessionUiState.Cached -> Text("当前离线，记录会先保存在本机并等待补传。")
                    is SessionUiState.Failure -> {
                        Text(sessionState.reason)
                        Button(onClick = onRetry) { Text("重试") }
                    }
                }
            }
        }
        ProfileInfoCard("隐私说明", "记录默认仅自己可见；只有你主动设为公开的内容才会出现在发现页。")
        ProfileInfoCard("问题反馈", "遇到识别、同步或内容问题时，请在反馈中附上发生时间和操作步骤。")
        TextButton(onClick = { showDiagnostics = !showDiagnostics }) {
            Text(if (showDiagnostics) "收起诊断信息" else "诊断信息")
        }
        if (showDiagnostics) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("仅用于排查问题", fontWeight = FontWeight.SemiBold)
                    Text("user_id: ${sessionState.userIdOrNull() ?: "尚未初始化"}")
                    Text("installationId 保存在应用私有存储中，不在普通页面展示。")
                }
            }
        }
    }
}

@Composable
private fun ProfileInfoCard(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF8F2)),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
