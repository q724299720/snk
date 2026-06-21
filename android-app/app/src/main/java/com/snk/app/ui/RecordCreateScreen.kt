package com.snk.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import com.snk.app.SnkApplication
import com.snk.app.data.food.FoodSearchItem
import com.snk.app.data.food.FoodSearchResult
import com.snk.app.data.record.FoodRecordLikeResult
import com.snk.app.data.record.FoodRecordImageAttachment
import com.snk.app.data.record.FoodRecordSubmissionCoordinator
import com.snk.app.data.record.FoodRecordSubmissionResult
import com.snk.app.data.record.RecordImageUploadResult
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

@Composable
fun RecordCreateScreen(
    selectedFood: FoodSearchItem,
    sourceType: String,
    sessionState: SessionUiState,
    submissionCoordinator: FoodRecordSubmissionCoordinator,
    onSwitchRecommendedFood: (FoodSearchItem) -> Unit,
    onBackToSearch: () -> Unit,
    onOpenDrafts: () -> Unit,
) {
    val application = LocalContext.current.applicationContext as SnkApplication
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var rating by remember { mutableIntStateOf(DEFAULT_RECORD_RATING) }
    var comment by remember { mutableStateOf("") }
    var submitState by remember { mutableStateOf<FoodRecordSubmissionResult?>(null) }
    var likeCount by remember { mutableIntStateOf(0) }
    var interactionMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var isLiking by remember { mutableStateOf(false) }
    var isPublic by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var uploadedRecordImage by remember { mutableStateOf<FoodRecordImageAttachment?>(null) }
    var imageUploadMessage by remember { mutableStateOf<String?>(null) }
    var isUploadingImage by remember { mutableStateOf(false) }
    val commentValidation = validateRecordCommentForUi(comment)
    val imageSaveValidation = validateRecordImageForSave(
        hasSelectedImage = selectedImageUri != null,
        hasUploadedImage = uploadedRecordImage != null,
        isUploadingImage = isUploadingImage,
    )
    val submitFeedbackMessage = buildRecordSubmitFeedback(
        result = submitState,
        hasUploadedImage = uploadedRecordImage != null,
    )
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) {
            return@rememberLauncherForActivityResult
        }
        selectedImageUri = uri
        uploadedRecordImage = null
        imageUploadMessage = "图片上传中..."
        coroutineScope.launch {
            isUploadingImage = true
            imageUploadMessage = try {
                val payload = readRecordImagePayload(context, uri)
                when (
                    val result = application.container.foodRecordRepository.uploadRecordImage(
                        imageBytes = payload.bytes,
                        fileName = payload.fileName,
                        contentType = payload.contentType,
                    )
                ) {
                    is RecordImageUploadResult.Success -> {
                        uploadedRecordImage = result.image
                        "图片已上传，保存记录时会一起保存。"
                    }

                    is RecordImageUploadResult.Failure -> result.message
                }
            } catch (exception: Exception) {
                "图片读取失败，请重新选择。"
            }
            isUploadingImage = false
        }
    }
    LaunchedEffect(selectedFood.id) {
        val reset = RecordCreateTransientState(
            rating = rating,
            comment = comment,
            submitState = submitState,
            likeCount = likeCount,
            interactionMessage = interactionMessage,
            isPublic = isPublic,
            imageUploadMessage = imageUploadMessage,
            isUploadingImage = isUploadingImage,
        ).resetForFoodSwitch()
        rating = reset.rating
        comment = reset.comment
        submitState = reset.submitState
        likeCount = reset.likeCount
        interactionMessage = reset.interactionMessage
        isPublic = reset.isPublic
        imageUploadMessage = reset.imageUploadMessage
        isUploadingImage = reset.isUploadingImage
        selectedImageUri = null
        uploadedRecordImage = null
    }
    val relatedFoodState by produceState<FoodSearchResult?>(initialValue = null, key1 = selectedFood.id) {
        value = application.container.foodSearchRepository.recommendRelatedFoods(selectedFood.id)
    }
    LaunchedEffect(submitState) {
        if (submitState != null) {
            scrollState.animateScrollTo(scrollState.maxValue)
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
            text = "新建记录",
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
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = selectedFood.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (selectedFood.auditStatus != "approved") {
                    Text(
                        text = "该条目仍在审核中，当前记录会先绑定到待审核条目。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFB53A1A),
                    )
                }
                Text(
                    text = buildString {
                        append(selectedFood.category)
                        selectedFood.subcategory?.takeIf { it.isNotBlank() }?.let {
                            append(" / ")
                            append(it)
                        }
                        selectedFood.brand?.takeIf { it.isNotBlank() }?.let {
                            append(" / ")
                            append(it)
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF5B4A42),
                )
                selectedFood.barcode?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = "条码: $it",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF8A5A44),
                    )
                }
            }
        }
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
                    text = "相似推荐",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                when (val related = relatedFoodState) {
                    null -> Text(
                        text = "正在加载同类食物推荐...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF5B4A42),
                    )

                    is FoodSearchResult.Failure -> Text(
                        text = related.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF8A2E1C),
                    )

                    is FoodSearchResult.Success -> {
                        Text(
                            text = if (related.items.isEmpty()) "暂时没有找到相似条目。" else "可以快速切换到以下相似条目：",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF5B4A42),
                        )
                        related.items.take(3).forEach { item ->
                            Button(
                                onClick = { onSwitchRecommendedFood(item) },
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text(item.name)
                            }
                        }
                    }
                }
            }
        }
        Text(
            text = "评分",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2B1E18),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            (1..5).forEach { value ->
                Button(
                    onClick = { rating = value },
                    shape = RoundedCornerShape(18.dp),
                    enabled = !isSubmitting,
                ) {
                    Text(if (rating == value) "$value 分" else value.toString())
                }
            }
        }
        OutlinedTextField(
            value = comment,
            onValueChange = { comment = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("备注，可选") },
            supportingText = {
                Text(commentValidation.message ?: "${comment.trim().length}/$MAX_RECORD_COMMENT_LENGTH")
            },
            isError = commentValidation.hasError,
            minLines = 3,
            maxLines = 5,
            shape = RoundedCornerShape(20.dp),
            enabled = !isSubmitting,
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
                        text = "Share to public feed",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Off by default. Turn on only when you want other users to see this record.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF5B4A42),
                    )
                }
                Switch(
                    checked = isPublic,
                    onCheckedChange = { isPublic = it },
                    enabled = !isSubmitting,
                )
            }
        }
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
                Text(
                    text = "可选。图片上传成功后会随记录保存，并显示在最近记录里。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF5B4A42),
                )
                selectedImageUri?.let { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = "Selected record photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(18.dp)),
                        contentScale = ContentScale.Crop,
                    )
                } ?: Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFFF2E3D3)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No photo selected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF8A5A44),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            imagePickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        enabled = !isSubmitting && !isUploadingImage,
                    ) {
                        Text(if (isUploadingImage) "上传中..." else "选择图片")
                    }
                    if (selectedImageUri != null) {
                        Button(
                            onClick = {
                                selectedImageUri = null
                                uploadedRecordImage = null
                                imageUploadMessage = null
                            },
                            shape = RoundedCornerShape(16.dp),
                            enabled = !isSubmitting && !isUploadingImage,
                        ) {
                            Text("移除")
                        }
                    }
                }
                imageUploadMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (uploadedRecordImage != null) Color(0xFF3D6B35) else Color(0xFF8A5A44),
                    )
                }
                imageSaveValidation.message?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF8A2E1C),
                    )
                }
            }
        }
        Text(
            text = when (sessionState) {
                SessionUiState.Loading -> "游客身份初始化中，暂时不能提交。"
                is SessionUiState.Remote -> "当前将记录到游客 user_id ${sessionState.session.userId}"
                is SessionUiState.Cached -> "当前离线沿用缓存游客 user_id ${sessionState.session.userId}"
                is SessionUiState.Failure -> sessionState.reason
            },
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF5B4A42),
        )
        submitFeedbackMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = when (submitState) {
                    is FoodRecordSubmissionResult.Failure -> Color(0xFF8A2E1C)
                    else -> Color(0xFF3D6B35)
                },
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
                    val result = submissionCoordinator.submit(
                        userId = userId,
                        selectedFood = selectedFood,
                        rating = rating,
                        comment = comment,
                        sourceType = sourceType,
                        isPublic = isPublic,
                        images = uploadedRecordImage?.let { listOf(it) }.orEmpty(),
                    )
                    submitState = result
                    interactionMessage = null
                    if (result is FoodRecordSubmissionResult.Submitted) {
                        likeCount = result.likeCount
                    }
                    isSubmitting = false
                }
            },
            enabled = !isSubmitting &&
                !isUploadingImage &&
                imageSaveValidation.canSave &&
                !commentValidation.hasError &&
                sessionState !is SessionUiState.Loading &&
                sessionState !is SessionUiState.Failure,
            shape = RoundedCornerShape(18.dp),
        ) {
            Text(if (isSubmitting) "保存中..." else "保存记录")
        }
        when (val result = submitState) {
            null -> Unit
            is FoodRecordSubmissionResult.Failure -> {
                Text(
                    text = result.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF8A2E1C),
                )
            }

            is FoodRecordSubmissionResult.Submitted -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFCF1E6)),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "记录已保存",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "record_id: ${result.recordId}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF5B4A42),
                        )
                        Text(
                            text = "已点赞 $likeCount 次",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF5B4A42),
                        )
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isLiking = true
                                    when (val likeResult = application.container.foodRecordRepository.likeRecord(result.recordId)) {
                                        is FoodRecordLikeResult.Success -> {
                                            likeCount = likeResult.likeCount
                                            interactionMessage = "已更新点赞数"
                                        }

                                        is FoodRecordLikeResult.Failure -> {
                                            interactionMessage = likeResult.message
                                        }
                                    }
                                    isLiking = false
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            enabled = !isLiking,
                        ) {
                            Text(if (isLiking) "点赞中..." else "点赞这条记录")
                        }
                        Button(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "SNK 记录分享")
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        buildRecordShareText(
                                            foodName = selectedFood.name,
                                            rating = rating,
                                            comment = comment,
                                            recordId = result.recordId,
                                            recordTime = result.recordTime,
                                        ),
                                    )
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "分享这条记录"))
                            },
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Text("分享记录")
                        }
                        Button(
                            onClick = onBackToSearch,
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Text("返回搜索")
                        }
                    }
                }
                interactionMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF8A2E1C),
                    )
                }
            }

            is FoodRecordSubmissionResult.SavedToDraft -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F2E8)),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "已转存草稿",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "draft_id: ${result.draft.id}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF5B4A42),
                        )
                        Text(
                            text = "当前无法连接服务端，网络恢复后会自动补传。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF5B4A42),
                        )
                        Button(
                            onClick = onOpenDrafts,
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Text("查看草稿")
                        }
                    }
                }
            }
        }
    }
}

internal const val DEFAULT_RECORD_RATING = 4
internal const val MAX_RECORD_COMMENT_LENGTH = 500

internal data class RecordCommentUiValidation(
    val hasError: Boolean,
    val message: String?,
)

internal data class RecordImageSaveValidation(
    val canSave: Boolean,
    val message: String?,
)

internal fun validateRecordCommentForUi(comment: String): RecordCommentUiValidation {
    val length = comment.trim().length
    return if (length > MAX_RECORD_COMMENT_LENGTH) {
        RecordCommentUiValidation(
            hasError = true,
            message = "备注最长支持 500 个字符，当前 $length 个。",
        )
    } else {
        RecordCommentUiValidation(
            hasError = false,
            message = null,
        )
    }
}

internal fun validateRecordImageForSave(
    hasSelectedImage: Boolean,
    hasUploadedImage: Boolean,
    isUploadingImage: Boolean,
): RecordImageSaveValidation {
    return when {
        isUploadingImage -> RecordImageSaveValidation(
            canSave = false,
            message = "图片正在上传，上传完成后再保存。",
        )

        hasSelectedImage && !hasUploadedImage -> RecordImageSaveValidation(
            canSave = false,
            message = "图片上传未完成或失败，请重新选择图片，或移除图片后保存。",
        )

        else -> RecordImageSaveValidation(canSave = true, message = null)
    }
}

internal fun buildRecordSubmitFeedback(
    result: FoodRecordSubmissionResult?,
    hasUploadedImage: Boolean,
): String? {
    return when (result) {
        null -> null
        is FoodRecordSubmissionResult.Submitted -> {
            if (hasUploadedImage) {
                "记录已保存，图片已保存。"
            } else {
                "记录已保存。"
            }
        }

        is FoodRecordSubmissionResult.SavedToDraft -> "已转存草稿，网络恢复后会自动补传。"
        is FoodRecordSubmissionResult.Failure -> result.message
    }
}

internal data class RecordCreateTransientState(
    val rating: Int = DEFAULT_RECORD_RATING,
    val comment: String = "",
    val submitState: FoodRecordSubmissionResult? = null,
    val likeCount: Int = 0,
    val interactionMessage: String? = null,
    val isPublic: Boolean = false,
    val imageUploadMessage: String? = null,
    val isUploadingImage: Boolean = false,
)

internal fun RecordCreateTransientState.resetForFoodSwitch(): RecordCreateTransientState = RecordCreateTransientState()

internal fun buildRecordShareText(
    foodName: String,
    rating: Int,
    comment: String,
    recordId: Long,
    recordTime: String,
): String {
    val normalizedComment = comment.trim()
    return buildString {
        append("SNK 记录分享\n")
        append("食物：")
        append(foodName)
        append('\n')
        append("评分：")
        append(rating)
        append(" 分\n")
        if (normalizedComment.isNotBlank()) {
            append("备注：")
            append(normalizedComment)
            append('\n')
        }
        append("record_id：")
        append(recordId)
        append('\n')
        append("record_time：")
        append(recordTime)
    }
}

private data class RecordImagePayload(
    val bytes: ByteArray,
    val fileName: String,
    val contentType: String,
)

private fun readRecordImagePayload(context: Context, imageUri: Uri): RecordImagePayload {
    val bitmap = decodeRecordBitmap(context, imageUri)
    val bytes = ByteArrayOutputStream().use { output ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
        output.toByteArray()
    }
    return RecordImagePayload(
        bytes = bytes,
        fileName = "record-${System.currentTimeMillis()}.jpg",
        contentType = "image/jpeg",
    )
}

private fun decodeRecordBitmap(context: Context, imageUri: Uri): Bitmap {
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
