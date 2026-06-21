package com.snk.app.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.snk.app.data.record.FoodRecordHistoryItem
import com.snk.app.data.record.FoodRecordImageAttachment
import com.snk.app.data.record.FoodRecordUpdateResult
import com.snk.app.data.record.RecordImageUploadResult
import kotlinx.coroutines.launch

@Composable
fun RecordEditScreen(
    record: FoodRecordHistoryItem,
    onBack: () -> Unit,
    onUpdated: (FoodRecordHistoryItem) -> Unit,
) {
    val application = LocalContext.current.applicationContext as SnkApplication
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var rating by remember(record.id) { mutableIntStateOf(record.rating) }
    var comment by remember(record.id) { mutableStateOf(record.comment.orEmpty()) }
    var isPublic by remember(record.id) { mutableStateOf(record.isPublic) }
    var isSaving by remember(record.id) { mutableStateOf(false) }
    var updateResult by remember(record.id) { mutableStateOf<FoodRecordUpdateResult?>(null) }
    var currentImages by remember(record.id) { mutableStateOf(record.images) }
    var selectedImageUri by remember(record.id) { mutableStateOf<Uri?>(null) }
    var imageUploadMessage by remember(record.id) { mutableStateOf<String?>(null) }
    var isUploadingImage by remember(record.id) { mutableStateOf(false) }
    val commentValidation = validateRecordCommentForUi(comment)
    val feedback = buildRecordEditFeedback(updateResult)
    val imageSaveValidation = validateRecordImageForSave(
        hasSelectedImage = selectedImageUri != null,
        hasUploadedImage = selectedImageUri == null || currentImages.isNotEmpty(),
        isUploadingImage = isUploadingImage,
    )
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) {
            return@rememberLauncherForActivityResult
        }
        selectedImageUri = uri
        currentImages = emptyList()
        imageUploadMessage = "图片上传中..."
        updateResult = null
        coroutineScope.launch {
            isUploadingImage = true
            imageUploadMessage = try {
                val payload = readEditableRecordImagePayload(context, uri)
                when (
                    val result = application.container.foodRecordRepository.uploadRecordImage(
                        imageBytes = payload.bytes,
                        fileName = payload.fileName,
                        contentType = payload.contentType,
                    )
                ) {
                    is RecordImageUploadResult.Success -> {
                        currentImages = listOf(result.image)
                        "图片已上传，保存修改后会替换原图片。"
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
            .verticalScroll(scrollState)
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "编辑记录",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = Color(0xFF2B1E18),
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEFBF7)),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = record.foodName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = formatRecordEditMeta(record),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF5B4A42),
                )
                Text(
                    text = "record_id: ${record.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF7A6A61),
                )
            }
        }
        EditableRecordImagesCard(
            record = record,
            selectedImageUri = selectedImageUri,
            currentImages = currentImages,
            imageUploadMessage = imageUploadMessage,
            isUploadingImage = isUploadingImage,
            onPickImage = {
                imagePickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            onRemoveImage = {
                selectedImageUri = null
                currentImages = emptyList()
                imageUploadMessage = "图片已移除，保存修改后记录将不再显示图片。"
                updateResult = null
            },
        )
        imageSaveValidation.message?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF8A2E1C),
            )
        }
        Text(
            text = "评分",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2B1E18),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..5).forEach { value ->
                Button(
                    onClick = { rating = value },
                    shape = RoundedCornerShape(18.dp),
                    enabled = !isSaving,
                ) {
                    Text(if (rating == value) "$value 分" else value.toString())
                }
            }
        }
        OutlinedTextField(
            value = comment,
            onValueChange = {
                comment = it
                updateResult = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("备注，可选") },
            supportingText = {
                Text(commentValidation.message ?: "${comment.trim().length}/$MAX_RECORD_COMMENT_LENGTH")
            },
            isError = commentValidation.hasError,
            minLines = 3,
            maxLines = 5,
            shape = RoundedCornerShape(20.dp),
            enabled = !isSaving,
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF6EA)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "公开到分享区",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "关闭时仅自己最近记录可见。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF5B4A42),
                    )
                }
                Switch(
                    checked = isPublic,
                    onCheckedChange = {
                        isPublic = it
                        updateResult = null
                    },
                    enabled = !isSaving,
                )
            }
        }
        feedback?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (updateResult is FoodRecordUpdateResult.Failure) Color(0xFF8A2E1C) else Color(0xFF3D6B35),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = onBack,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
                enabled = !isSaving,
            ) {
                Text("返回")
            }
            Button(
                onClick = {
                    coroutineScope.launch {
                        isSaving = true
                        val result = application.container.foodRecordRepository.updateRecord(
                            recordId = record.id,
                            userId = record.userId,
                            rating = rating,
                            comment = comment,
                            isPublic = isPublic,
                            images = currentImages,
                        )
                        updateResult = result
                        if (result is FoodRecordUpdateResult.Success) {
                            onUpdated(
                                record.copy(
                                    rating = result.rating,
                                    comment = result.comment,
                                    isPublic = result.isPublic,
                                    images = currentImages,
                                ),
                            )
                        }
                        isSaving = false
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
                enabled = !isSaving && !isUploadingImage && imageSaveValidation.canSave && !commentValidation.hasError,
            ) {
                Text(if (isSaving) "保存中..." else "保存修改")
            }
        }
    }
}

@Composable
private fun EditableRecordImagesCard(
    record: FoodRecordHistoryItem,
    selectedImageUri: Uri?,
    currentImages: List<FoodRecordImageAttachment>,
    imageUploadMessage: String?,
    isUploadingImage: Boolean,
    onPickImage: () -> Unit,
    onRemoveImage: () -> Unit,
) {
    val previewImages = currentImages

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F1E7)),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "记录图片",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            selectedImageUri?.let { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = "Selected replacement record image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Crop,
                )
            } ?: if (previewImages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFFF2E3D3)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "暂无图片",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF8A5A44),
                    )
                }
            } else {
                previewImages.forEachIndexed { index, image ->
                    AsyncImage(
                        model = image.thumbnailUrl ?: image.imageUrl,
                        contentDescription = "Record image ${index + 1}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(18.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }
                Text(
                    text = "可选择新图片替换当前图片，也可以移除图片后保存。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF5B4A42),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onPickImage,
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isUploadingImage,
                ) {
                    Text(if (isUploadingImage) "上传中..." else "选择新图片")
                }
                if (previewImages.isNotEmpty() || selectedImageUri != null) {
                    Button(
                        onClick = onRemoveImage,
                        shape = RoundedCornerShape(16.dp),
                        enabled = !isUploadingImage,
                    ) {
                        Text("移除图片")
                    }
                }
            }
            imageUploadMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (currentImages.isNotEmpty()) Color(0xFF3D6B35) else Color(0xFF8A5A44),
                )
            }
        }
    }
}

internal fun buildRecordEditFeedback(result: FoodRecordUpdateResult?): String? = when (result) {
    null -> null
    is FoodRecordUpdateResult.Success -> "记录已更新。"
    is FoodRecordUpdateResult.Failure -> result.message
}

private fun formatRecordEditMeta(record: FoodRecordHistoryItem): String = buildString {
    append(record.foodCategory)
    record.foodSubcategory?.takeIf { it.isNotBlank() }?.let {
        append(" / ")
        append(it)
    }
    record.foodBrand?.takeIf { it.isNotBlank() }?.let {
        append(" / ")
        append(it)
    }
}

private data class EditableRecordImagePayload(
    val bytes: ByteArray,
    val fileName: String,
    val contentType: String,
)

private fun readEditableRecordImagePayload(context: Context, imageUri: Uri): EditableRecordImagePayload {
    val resolver = context.contentResolver
    val bytes = resolver.openInputStream(imageUri)?.use { it.readBytes() }
        ?: error("Unable to read selected image.")
    val contentType = resolver.getType(imageUri) ?: "image/jpeg"
    return EditableRecordImagePayload(
        bytes = bytes,
        fileName = "record-edit-${System.currentTimeMillis()}.jpg",
        contentType = contentType,
    )
}
