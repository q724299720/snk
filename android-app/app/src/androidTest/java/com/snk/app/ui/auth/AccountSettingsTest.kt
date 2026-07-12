package com.snk.app.ui.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.snk.app.data.auth.AuthenticatedAccount
import com.snk.app.ui.ProfileScreen
import com.snk.app.ui.SessionUiState
import org.junit.Rule
import org.junit.Test

class AccountSettingsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun profileShowsAccountAndSessionActions() {
        compose.setContent {
            ProfileScreen(
                account = AuthenticatedAccount(7, "alice", "USER", false),
                sessionState = SessionUiState.Authenticated(7),
                onChangePassword = {}, onClaimLegacyHistory = {}, onLogout = {},
            )
        }
        compose.onNodeWithText("alice").assertIsDisplayed()
        compose.onNodeWithText("普通账号").assertIsDisplayed()
        compose.onNodeWithText("修改密码").assertIsDisplayed()
        compose.onNodeWithText("退出登录").assertIsDisplayed()
    }
}
