package com.snk.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class NoCategoryUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun manualCreationDoesNotExposeCategoryInputs() {
        compose.setContent {
            ManualFoodCreateScreen(
                sessionState = SessionUiState.Authenticated(userId = 7L),
                initialName = "测试产品",
                initialBarcode = "",
                onFoodCreated = {},
                onBack = {},
            )
        }

        compose.onNodeWithText("名称").assertIsDisplayed()
        compose.onNodeWithText("类型").assertIsDisplayed()
        compose.onNodeWithText("一级分类").assertDoesNotExist()
        compose.onNodeWithText("二级分类（可选）").assertDoesNotExist()
        compose.onNodeWithText("待分类", substring = true).assertDoesNotExist()
    }
}
