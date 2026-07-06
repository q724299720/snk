package com.snk.app.ui

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.snk.app.SnkApplication
import com.snk.app.data.food.FoodSearchItem
import com.snk.app.data.food.ManualFoodCreateResult
import com.snk.app.data.record.RecordImageUploadResult
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.launch

private data class ManualOption(
    val value: String,
    val label: String,
)

private val manualItemTypeOptions = listOf(
    ManualOption("packaged_product", "包装食品"),
    ManualOption("dish", "菜品/饮品"),
    ManualOption("fruit", "水果"),
)

private val manualCategoryOptions = listOf(
    ManualOption("snack", "零食"),
    ManualOption("dessert", "甜品"),
    ManualOption("drink", "饮品"),
    ManualOption("meal", "主食"),
    ManualOption("fruit", "水果"),
)

@Composable
fun ManualFoodCreateScreen(
    sessionState: SessionUiState,
    initialName: String,
    initialBarcode: String,
    onFoodCreated: (FoodSearchItem) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val application = context.applicationContext as SnkApplication
    val coroutineScope = rememberCoroutineScope()
    var name by remember(initialName) { mutableStateOf(initialName) }
    var barcode by remember(initialBarcode) { mutableStateOf(initialBarcode) }
    var itemType by remember { mutableStateOf("packaged_product") }
    var category by remember { mutableStateOf("snack") }
    var subcategory by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var submitMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var uploadedCoverImageUrl by remember { mutableStateOf<String?>(null) }
    var imageUploadMessage by remember { mutableStateOf<String?>(null) }
    var isUploadingImage by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        selectedImageUri = uri
        uploadedCoverImageUrl = null
        imageUploadMessage = "封面图片上传中..."
        coroutineScope.launch {
            isUploadingImage = true
            imageUploadMessage = try {
                val payload = readManualImagePayload(context, uri)
                when (
                    val result = application.container.foodRecordRepository.uploadRecordImage(
                        imageBytes = payload.bytes,
                        fileName = payload.fileName,
                        contentType = payload.contentType,
                    )
                ) {
                    is RecordImageUploadResult.Success -> {
                        uploadedCoverImageUrl = result.image.imageUrl
                        "封面图片已上传。"
                    }
                    is RecordImageUploadResult.Failure -> result.message
                }
            } catch (exception: Exception) {
                "图片读取失败，请重新选择。"
            }
            isUploadingImage = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "手动创建",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = Color(0xFF2B1E18),
        )
        Text(
            text = "没有找到匹配结果时，先创建一个待审核条目，再继续保存这次记录。",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF5B4A42),
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFCF1E6)),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("名称") },
                    singleLine = true,
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(18.dp),
                )
                Text(
                    text = "类型",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    manualItemTypeOptions.forEach { option ->
                        FilterChip(
                            selected = itemType == option.value,
                            onClick = { itemType = option.value },
                            enabled = !isSubmitting,
                            label = { Text(option.label) },
                        )
                    }
                }
                Text(
                    text = "一级分类",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    manualCategoryOptions.forEach { option ->
                        FilterChip(
                            selected = category == option.value,
                            onClick = { category = option.value },
                            enabled = !isSubmitting,
                            label = { Text(option.label) },
                        )
                    }
                }
                OutlinedTextField(
                    value = subcategory,
                    onValueChange = { subcategory = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("二级分类（可选）") },
                    singleLine = true,
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(18.dp),
                )
                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("品牌（可选）") },
                    singleLine = true,
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(18.dp),
                )
                if (itemType == "packaged_product") {
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("条码（包装食品可选填）") },
                        singleLine = true,
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(18.dp),
                    )
                }
                Text(
                    text = "封面图片（可选）",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                selectedImageUri?.let { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = "封面图片预览",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(18.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }
                imageUploadMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (uploadedCoverImageUrl != null) Color(0xFF2E7D32) else Color(0xFF8A2E1C),
                    )
                }
                OutlinedButton(
                    onClick = { imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    enabled = !isSubmitting && !isUploadingImage,
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text(if (isUploadingImage) "上传中..." else "选择图片")
                }
            }
        }
        Text(
            text = when (sessionState) {
                SessionUiState.Loading -> "游客身份初始化中，暂时不能创建待审核条目。"
                is SessionUiState.Remote -> "当前将以游客 user_id ${sessionState.session.userId} 创建待审核条目"
                is SessionUiState.Cached -> "当前离线沿用缓存游客 user_id ${sessionState.session.userId}"
                is SessionUiState.Failure -> sessionState.reason
            },
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF5B4A42),
        )
        submitMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF8A2E1C),
            )
        }
        Button(
            onClick = {
                val userId = when (sessionState) {
                    is SessionUiState.Remote -> sessionState.session.userId
                    is SessionUiState.Cached -> sessionState.session.userId
                    else -> null
                } ?: return@Button

                coroutineScope.launch {
                    isSubmitting = true
                    submitMessage = null
                    when (
                        val result = application.container.foodSearchRepository.createManualFoodItem(
                            userId = userId,
                            name = name,
                            itemType = itemType,
                            category = category,
                            subcategory = subcategory,
                            brand = brand,
                            barcode = barcode,
                            coverImageUrl = uploadedCoverImageUrl,
                        )
                    ) {
                        is ManualFoodCreateResult.Success -> onFoodCreated(result.item)
                        is ManualFoodCreateResult.Failure -> submitMessage = result.message
                    }
                    isSubmitting = false
                }
            },
            enabled = !isSubmitting && !isUploadingImage && sessionState !is SessionUiState.Loading && sessionState !is SessionUiState.Failure,
            shape = RoundedCornerShape(18.dp),
        ) {
            Text(if (isSubmitting) "创建中..." else "创建并记一笔")
        }
        Button(
            onClick = onBack,
            shape = RoundedCornerShape(16.dp),
        ) {
            Text("返回")
        }
    }
}

private data class ManualImagePayload(
    val bytes: ByteArray,
    val fileName: String,
    val contentType: String,
)

private fun readManualImagePayload(context: android.content.Context, imageUri: Uri): ManualImagePayload {
    val bitmap = decodeManualBitmap(context, imageUri)
    val bytes = ByteArrayOutputStream().use { output ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
        output.toByteArray()
    }
    return ManualImagePayload(
        bytes = bytes,
        fileName = "cover-${System.currentTimeMillis()}.jpg",
        contentType = "image/jpeg",
    )
}

private fun decodeManualBitmap(context: android.content.Context, imageUri: Uri): Bitmap {
    val resolver = context.contentResolver
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, imageUri)) { decoder, _, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    } else {
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.getBitmap(resolver, imageUri)
    }
}
