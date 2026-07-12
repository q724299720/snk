package com.snk.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

class ProductImageDisplayTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun productImageUsesReadableDescriptionAndDecorativePlaceholder() {
        compose.setContent {
            ProductImage(
                imageUrl = null,
                productName = "乐事黄瓜味薯片",
                imageKind = ProductImageKind.PRODUCT,
                size = 96.dp,
            )
        }

        compose.onNodeWithText("暂无图片").assertIsDisplayed()
        compose.onNodeWithContentDescription("乐事黄瓜味薯片图片").assertDoesNotExist()
    }
}
