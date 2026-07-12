package com.snk.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.snk.app.data.auth.AuthenticatedAccount
import com.snk.app.data.record.FoodRecordHistoryItem
import com.snk.app.data.record.FoodRecordHistoryResult
import com.snk.app.ui.theme.ChiliRed
import com.snk.app.ui.theme.Divider
import com.snk.app.ui.theme.MutedText
import com.snk.app.ui.theme.Paper
import com.snk.app.ui.theme.PeachSurface

private sealed interface ProfileHistoryUiState {
    data object Loading : ProfileHistoryUiState
    data class Content(val records: List<FoodRecordHistoryItem>) : ProfileHistoryUiState
    data class Failure(val message: String) : ProfileHistoryUiState
}

@Composable
fun ProfileScreen(
    account: AuthenticatedAccount,
    sessionState: SessionUiState,
    onChangePassword: () -> Unit,
    onClaimLegacyHistory: () -> Unit,
    onLogout: () -> Unit,
    recordHistoryLoader: (suspend (Long, Int, Int) -> FoodRecordHistoryResult)? = null,
) {
    var showDiagnostics by remember { mutableStateOf(false) }
    var historyRefreshToken by remember { mutableIntStateOf(0) }
    val historyState by produceState<ProfileHistoryUiState>(
        initialValue = ProfileHistoryUiState.Loading,
        key1 = account.userId,
        key2 = recordHistoryLoader,
        key3 = historyRefreshToken,
    ) {
        val loader = recordHistoryLoader
        if (loader == null) {
            value = ProfileHistoryUiState.Content(emptyList())
            return@produceState
        }
        val allRecords = mutableListOf<FoodRecordHistoryItem>()
        var page = 0
        val pageSize = 40
        while (true) {
            when (val result = loader(account.userId, page, pageSize)) {
                is FoodRecordHistoryResult.Success -> {
                    allRecords += result.items
                    if (result.items.size < pageSize) break
                    page++
                }
                is FoodRecordHistoryResult.Failure -> {
                    value = ProfileHistoryUiState.Failure(result.message)
                    return@produceState
                }
            }
        }
        value = ProfileHistoryUiState.Content(allRecords)
    }
    val records = (historyState as? ProfileHistoryUiState.Content)?.records.orEmpty()
    val statsAvailable = historyState is ProfileHistoryUiState.Content

    Column(
        modifier = Modifier.fillMaxSize().background(Paper).verticalScroll(rememberScrollState()),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().background(PeachSurface).padding(horizontal = 18.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(72.dp).clip(CircleShape).background(ChiliRed),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(account.username.take(1).uppercase(), color = Color.White, style = MaterialTheme.typography.headlineMedium)
                }
                Column(Modifier.weight(1f).padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(account.username, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            if (account.role == "OWNER") "主账户" else "普通账号",
                            modifier = Modifier.padding(start = 8.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = .7f)).padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = ChiliRed,
                        )
                    }
                    Text("记录每一餐，遇见更好的自己", style = MaterialTheme.typography.bodySmall, color = MutedText)
                }
                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MutedText)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ProfileStat(if (statsAvailable) records.size.toString() else "—", "记录")
                ProfileStat(if (statsAvailable) records.count { it.isPublic }.toString() else "—", "公开")
                ProfileStat(if (statsAvailable) records.sumOf { it.likeCount }.toString() else "—", "获赞")
            }
        }

        Column(Modifier.padding(horizontal = 16.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            when (val state = historyState) {
                ProfileHistoryUiState.Loading -> Text("正在加载个人记录…", color = MutedText)
                is ProfileHistoryUiState.Failure -> Card(colors = CardDefaults.cardColors(containerColor = PeachSurface)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(state.message, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { historyRefreshToken++ }) { Text("重试") }
                    }
                }
                is ProfileHistoryUiState.Content -> Unit
            }
            if (records.isNotEmpty()) {
                Text("我的美食", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                records.take(6).chunked(3).forEach { rowRecords ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowRecords.forEach { record -> ProfileFoodImage(record, Modifier.weight(1f)) }
                        repeat(3 - rowRecords.size) { Box(Modifier.weight(1f)) }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Paper),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column {
                    ProfileSettingRow(Icons.Outlined.Sync, "同步状态", sessionLabel(sessionState), onClaimLegacyHistory)
                    ProfileSettingRow(Icons.Outlined.Lock, "隐私设置", "新记录默认公开", null)
                    ProfileSettingRow(Icons.Outlined.Password, "账号与安全", "修改密码", onChangePassword)
                    ProfileSettingRow(Icons.Outlined.Info, "关于 SNK", "问题反馈与版本信息", { showDiagnostics = !showDiagnostics })
                }
            }
            if (showDiagnostics) {
                Card(colors = CardDefaults.cardColors(containerColor = PeachSurface)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("诊断信息", fontWeight = FontWeight.Bold)
                        Text("user_id: ${sessionState.userIdOrNull() ?: "尚未连接"}", style = MaterialTheme.typography.bodySmall)
                        Text("installationId 保存在应用私有存储中。", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.AutoMirrored.Outlined.Logout, null, modifier = Modifier.padding(end = 8.dp))
                Text("退出登录")
            }
        }
    }
}

@Composable
private fun ProfileStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MutedText)
    }
}

@Composable
private fun ProfileFoodImage(record: FoodRecordHistoryItem, modifier: Modifier) {
    ProductImageFill(
        imageUrl = record.preferredDiscoverImageUrl(),
        productName = record.foodName,
        imageKind = ProductImageKind.RECORD,
        modifier = modifier.aspectRatio(1f).clip(RoundedCornerShape(14.dp)).background(PeachSurface),
    )
}

@Composable
private fun ProfileSettingRow(icon: ImageVector, title: String, value: String, onClick: (() -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(enabled = onClick != null) { onClick?.invoke() }.padding(horizontal = 14.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MutedText, modifier = Modifier.size(21.dp))
        Text(title, modifier = Modifier.weight(1f).padding(start = 12.dp), fontWeight = FontWeight.Medium)
        Text(value, style = MaterialTheme.typography.bodySmall, color = MutedText)
        Icon(Icons.Outlined.ChevronRight, null, tint = Divider, modifier = Modifier.size(20.dp))
    }
}

private fun sessionLabel(sessionState: SessionUiState): String = when (sessionState) {
    is SessionUiState.Authenticated, is SessionUiState.Remote -> "已登录，自动同步"
    is SessionUiState.Cached -> "离线，等待补传"
    SessionUiState.Loading -> "正在连接"
    is SessionUiState.Failure -> "连接异常"
}
