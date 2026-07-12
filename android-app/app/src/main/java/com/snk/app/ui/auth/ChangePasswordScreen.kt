package com.snk.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

object PasswordChangePolicy {
    fun isValid(oldPassword: String, newPassword: String, confirmation: String) =
        oldPassword.isNotBlank() && newPassword.length >= 12 && newPassword == confirmation
}

@Composable
fun ChangePasswordScreen(onSubmit: (String, String) -> Unit, onBack: (() -> Unit)? = null) {
    var old by remember { mutableStateOf("") }; var new by remember { mutableStateOf("") }; var confirm by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center) {
        Text("修改密码", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(old, { old = it }, Modifier.fillMaxWidth(), label={ Text("旧密码") }, visualTransformation=PasswordVisualTransformation())
        OutlinedTextField(new, { new = it }, Modifier.fillMaxWidth(), label={ Text("新密码（至少 12 位）") }, visualTransformation=PasswordVisualTransformation())
        OutlinedTextField(confirm, { confirm = it }, Modifier.fillMaxWidth(), label={ Text("确认新密码") }, visualTransformation=PasswordVisualTransformation())
        Button({ onSubmit(old, new) }, enabled=PasswordChangePolicy.isValid(old,new,confirm), modifier=Modifier.fillMaxWidth()) { Text("确认修改") }
        if (onBack != null) OutlinedButton(onBack, Modifier.fillMaxWidth()) { Text("返回") }
    }
}
