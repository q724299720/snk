package com.snk.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage

enum class ProductImageKind(private val suffix: String) {
    PRODUCT("图片"),
    RECORD("记录图片"),
    ;

    fun descriptionFor(productName: String) = "$productName$suffix"
}

@Composable
fun ProductImage(
    imageUrl: String?,
    productName: String,
    imageKind: ProductImageKind,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val imageModifier = modifier
        .size(size)
        .clip(RoundedCornerShape(16.dp))
    if (imageUrl.isNullOrBlank()) {
        ProductImagePlaceholder(imageModifier)
        return
    }

    SubcomposeAsyncImage(
        model = imageUrl,
        contentDescription = null,
        modifier = imageModifier.semantics {
            contentDescription = imageKind.descriptionFor(productName)
        },
        contentScale = ContentScale.Crop,
        loading = { ProductImagePlaceholder(Modifier.fillMaxSize()) },
        error = { ProductImagePlaceholder(Modifier.fillMaxSize()) },
    )
}

@Composable
private fun ProductImagePlaceholder(modifier: Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "暂无图片", style = MaterialTheme.typography.bodySmall)
    }
}
