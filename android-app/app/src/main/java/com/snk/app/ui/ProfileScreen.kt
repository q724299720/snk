package com.snk.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ProfileScreen(
    sessionState: SessionUiState,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "游客身份",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = Color(0xFF2B1E18),
        )
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B1E18)),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "installationId -> anonymous user_id",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFFFE6D1),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "已接匿名初始化接口，并在本地保存安装级身份，保证同一安装周期内记录可关联。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFFFF4EA),
                )
            }
        }
        SessionStatusCard(
            sessionState = sessionState,
            onRetry = onRetry,
        )
    }
}

@Composable
private fun SessionStatusCard(
    sessionState: SessionUiState,
    onRetry: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7EF)),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "会话状态",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2B1E18),
            )
            when (sessionState) {
                SessionUiState.Loading -> {
                    Text(
                        text = "正在向服务端申请或复用匿名 user_id。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF5B4A42),
                    )
                }

                is SessionUiState.Remote -> {
                    Text(
                        text = "远程初始化成功，当前游客 user_id: ${sessionState.session.userId}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF5B4A42),
                    )
                }

                is SessionUiState.Cached -> {
                    Text(
                        text = "网络不可用，已回退到本地缓存游客身份: ${sessionState.session.userId}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF5B4A42),
                    )
                    Text(
                        text = sessionState.reason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF8A5A44),
                    )
                }

                is SessionUiState.Failure -> {
                    Text(
                        text = sessionState.reason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF8A2E1C),
                    )
                    Button(onClick = onRetry) {
                        Text("重试")
                    }
                }
            }
        }
    }
}
