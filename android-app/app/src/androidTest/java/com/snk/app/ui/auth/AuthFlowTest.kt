package com.snk.app.ui.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class AuthFlowTest {
    @get:Rule val compose = createComposeRule()

    @Test fun signedOutScreenOnlyOffersLoginAndRegistration() {
        compose.setContent { LoginScreen(onLogin = { _, _ -> }, onOpenRegistration = {}) }
        compose.onNodeWithText("登录").assertIsDisplayed()
        compose.onNodeWithText("注册新账号").assertIsDisplayed()
        compose.onNodeWithText("游客").assertDoesNotExist()
    }

    @Test fun pendingScreenExplainsApprovalAndProvidesManualRefresh() {
        compose.setContent { PendingApprovalScreen(onCheckNow = {}, onBackToLogin = {}) }
        compose.onNodeWithText("等待主账户审核").assertIsDisplayed()
        compose.onNodeWithText("重新检查").assertIsDisplayed()
    }
}
