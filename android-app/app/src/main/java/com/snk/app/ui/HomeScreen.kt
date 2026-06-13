package com.snk.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        HeroCard()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FeatureCard(
                modifier = Modifier.weight(1f),
                title = "扫码优先",
                description = "零食先扫条形码，再走 OCR 与识别兜底。",
                icon = Icons.Outlined.QrCodeScanner,
                accent = Color(0xFFFF7A1A),
            )
            FeatureCard(
                modifier = Modifier.weight(1f),
                title = "拍照记录",
                description = "支持图片、评分、标签和口味备注。",
                icon = Icons.Outlined.PhotoCamera,
                accent = Color(0xFFD94D2E),
            )
        }
        FeatureCard(
            modifier = Modifier.fillMaxWidth(),
            title = "当前开发阶段",
            description = "Phase 2 正在落地安卓端基础记录闭环，先完成游客身份、搜索入口、草稿与补传能力。",
            icon = Icons.Outlined.AutoAwesome,
            accent = Color(0xFFB53A1A),
        )
        FeatureCard(
            modifier = Modifier.fillMaxWidth(),
            title = "冷启动底座",
            description = "服务端已准备基础种子数据、匿名用户初始化和图片上传能力，下一步接文本搜索与记录创建。",
            icon = Icons.Outlined.Inventory2,
            accent = Color(0xFF6F4E37),
        )
    }
}

@Composable
private fun HeroCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(28.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFD9481F),
                            Color(0xFFF07B3F),
                            Color(0xFFFFC15E),
                        ),
                    ),
                    shape = RoundedCornerShape(28.dp),
                )
                .padding(24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "SNK",
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = "把今天吃到的零食和美食，做成一份可以检索、打分、回看的个人味觉档案。",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFFFF6EE),
                )
                Text(
                    text = "识别策略：条形码 > 本地 OCR > 服务端 OCR > 图像识别",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFFFF0D9),
                )
            }
        }
    }
}

@Composable
private fun FeatureCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    icon: ImageVector,
    accent: Color,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEFBF7)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(accent.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accent,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2B1E18),
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF5B4A42),
            )
        }
    }
}
