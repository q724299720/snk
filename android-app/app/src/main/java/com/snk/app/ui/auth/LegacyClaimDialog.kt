package com.snk.app.ui.auth

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.snk.app.data.auth.LegacyClaimCoordinator
import com.snk.app.data.auth.LegacyClaimResult
import kotlinx.coroutines.launch

@Composable
fun LegacyClaimDialog(coordinator: LegacyClaimCoordinator, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("认领本机历史记录") },
        text = { Text(message ?: "本机旧版记录将归入当前账号；完成后不可更改。不会显示或探测其他账号的数据。") },
        confirmButton = {
            Button(onClick = {
                scope.launch {
                    when (coordinator.claim()) {
                        LegacyClaimResult.CLAIMED, LegacyClaimResult.ALREADY_CLAIMED, LegacyClaimResult.NOT_AVAILABLE -> onDismiss()
                        LegacyClaimResult.RETRYABLE_FAILURE -> message = "暂时无法完成认领，请稍后重试。"
                    }
                }
            }) { Text("认领") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("暂不") } },
    )
}
