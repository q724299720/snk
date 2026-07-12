package com.snk.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.snk.app.SnkApplication
import java.io.File
import java.util.UUID
import com.snk.app.data.food.FoodSearchItem
import com.snk.app.data.food.ManualFoodCreateResult
import com.snk.app.data.record.RecordImageUploadResult
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

    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    suspend fun uploadCoverImage(uri: Uri) {
        isUploadingImage = true
        imageUploadMessage = "封面图片上传中..."
        imageUploadMessage = try {
            val compressed = com.snk.app.util.ImageCompressor.compress(context, uri)
            when (val result = application.container.foodRecordRepository.uploadRecordImage(
                    imageBytes = compressed.bytes,
                    fileName = compressed.fileName,
                    contentType = compressed.contentType,
                )) {
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

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val imageUri = pendingCameraUri
        if (!success || imageUri == null) return@rememberLauncherForActivityResult
        selectedImageUri = imageUri
        uploadedCoverImageUrl = null
        coroutineScope.launch { uploadCoverImage(imageUri) }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            val imageUri = createTempManualCameraImageUri(context)
            pendingCameraUri = imageUri
            cameraLauncher.launch(imageUri)
        } else {
            imageUploadMessage = "需要相机权限才能拍照。"
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        selectedImageUri = uri
        uploadedCoverImageUrl = null
        coroutineScope.launch { uploadCoverImage(uri) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "手动创建",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2B1E18),
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFCF1E6)),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("名称") },
                    singleLine = true,
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(14.dp),
                )
                Text(
                    text = "类型",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    manualItemTypeOptions.forEach { option ->
                        FilterChip(
                            selected = itemType == option.value,
                            onClick = { itemType = option.value },
                            enabled = !isSubmitting,
                            label = { Text(option.label, style = MaterialTheme.typography.bodySmall) },
                        )
                    }
                }
                Text(
                    text = "一级分类",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    manualCategoryOptions.forEach { option ->
                        FilterChip(
                            selected = category == option.value,
                            onClick = { category = option.value },
                            enabled = !isSubmitting,
                            label = { Text(option.label, style = MaterialTheme.typography.bodySmall) },
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
                    shape = RoundedCornerShape(14.dp),
                )
                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("品牌（可选）") },
                    singleLine = true,
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(14.dp),
                )
                if (itemType == "packaged_product") {
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("条码（可选）") },
                        singleLine = true,
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(14.dp),
                    )
                }
                Text(
                    text = "封面图片（可选）",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                selectedImageUri?.let { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = "封面图片预览",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(14.dp)),
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val hasCameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                            if (hasCameraPermission) {
                                val imageUri = createTempManualCameraImageUri(context)
                                pendingCameraUri = imageUri
                                cameraLauncher.launch(imageUri)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        enabled = !isSubmitting && !isUploadingImage,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    ) {
                        Text(if (isUploadingImage) "上传中..." else "拍照", style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        onClick = { imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        shape = RoundedCornerShape(14.dp),
                        enabled = !isSubmitting && !isUploadingImage,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    ) {
                        Text(if (isUploadingImage) "上传中..." else "相册", style = MaterialTheme.typography.bodySmall)
                    }
                    if (selectedImageUri != null) {
                        Button(
                            onClick = {
                                selectedImageUri = null
                                uploadedCoverImageUrl = null
                                imageUploadMessage = null
                            },
                            shape = RoundedCornerShape(14.dp),
                            enabled = !isSubmitting && !isUploadingImage,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        ) {
                            Text("移除", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        submitMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8A2E1C),
            )
        }
        Button(
            onClick = {
                val userId = when (sessionState) {
                    is SessionUiState.Authenticated -> sessionState.userId
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
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 10.dp),
        ) {
            Text(if (isSubmitting) "创建中..." else "创建并记一笔")
        }
        OutlinedButton(
            onClick = onBack,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            Text("返回")
        }
    }
}

private fun createTempManualCameraImageUri(context: android.content.Context): Uri {
    val imageDirectory = File(context.cacheDir, "manual-camera").apply { mkdirs() }
    val imageFile = File(imageDirectory, "camera-${UUID.randomUUID()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
}


