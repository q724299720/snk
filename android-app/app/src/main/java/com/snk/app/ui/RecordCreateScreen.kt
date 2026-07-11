package com.snk.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.TextButton
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
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.snk.app.SnkApplication
import com.snk.app.data.food.FoodSearchItem
import com.snk.app.data.food.FoodSearchResult
import com.snk.app.data.record.FoodRecordLikeResult
import com.snk.app.data.record.FoodRecordImageAttachment
import com.snk.app.data.record.FoodRecordSubmissionCoordinator
import com.snk.app.data.record.FoodRecordSubmissionResult
import com.snk.app.data.record.RecordImageUploadResult
import coil.compose.AsyncImage
import java.io.File
import java.util.UUID
import kotlinx.coroutines.launch
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
    val clientRequestId = remember { UUID.randomUUID().toString() }
    var rating by remember { mutableStateOf<Int?>(null) }
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
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var cameraPermanentlyDenied by remember { mutableStateOf(false) }

    suspend fun uploadSelectedImage(uri: Uri): String = try {
        val compressed = com.snk.app.util.ImageCompressor.compress(context, uri)
        when (val result = application.container.foodRecordRepository.uploadRecordImage(
                imageBytes = compressed.bytes,
                fileName = compressed.fileName,
                contentType = compressed.contentType,
            )) {
            is RecordImageUploadResult.Success -> {
                uploadedRecordImage = result.image
                "图片已上传，保存记录时会一起保存。"
            }
            is RecordImageUploadResult.Failure -> result.message
        }
    } catch (exception: Exception) {
        "图片读取失败，请重新选择。"
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val imageUri = pendingCameraUri
        if (!success || imageUri == null) return@rememberLauncherForActivityResult
        selectedImageUri = imageUri
        uploadedRecordImage = null
        imageUploadMessage = "图片上传中..."
        coroutineScope.launch {
            isUploadingImage = true
            imageUploadMessage = uploadSelectedImage(uri = imageUri)
            isUploadingImage = false
        }
    }

    fun launchCamera() {
        val imageUri = createTempRecordCameraImageUri(context)
        pendingCameraUri = imageUri
        cameraLauncher.launch(imageUri)
    }
    val cameraPermission = rememberCameraPermissionController(
        onGranted = ::launchCamera,
        onDenied = { kind ->
            cameraPermanentlyDenied = kind == CameraDenialKind.PERMANENT
            imageUploadMessage = if (cameraPermanentlyDenied) {
                "相机权限已被永久拒绝，可去系统设置，或从相册选择/跳过图片。"
            } else {
                "未获得相机权限，可以从相册选择或跳过图片。"
            }
        },
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
            imageUploadMessage = uploadSelectedImage(uri = uri)
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
        scrollState.scrollTo(0)
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEFBF7)),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = selectedFood.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (selectedFood.auditStatus != "approved") {
                    Text(
                        text = "该条目仍在审核中，当前记录会先绑定到待审核条目。",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB53A1A),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = selectedFood.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF5B4A42),
                    )
                    selectedFood.subcategory?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = "/ $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF5B4A42),
                        )
                    }
                    selectedFood.brand?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = "/ $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF5B4A42),
                        )
                    }
                }
            }
        }
        Text(
            text = "评分",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2B1E18),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (1..5).forEach { value ->
                Button(
                    onClick = { rating = value },
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isSubmitting,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = if (rating == value) "$value 分" else value.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        OutlinedTextField(
            value = comment,
            onValueChange = { comment = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("备注（可选）") },
            supportingText = {
                Text(commentValidation.message ?: "${comment.trim().length}/$MAX_RECORD_COMMENT_LENGTH")
            },
            isError = commentValidation.hasError,
            minLines = 2,
            maxLines = 4,
            shape = RoundedCornerShape(16.dp),
            enabled = !isSubmitting,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "公开分享",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "开启后其他用户可在公开分享区看到",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(
                checked = isPublic,
                onCheckedChange = { isPublic = it },
                enabled = !isSubmitting,
            )
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F1E7)),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "记录图片",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                selectedImageUri?.let { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = "Selected record photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop,
                    )
                } ?: Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFF2E3D3)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "暂无图片",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = cameraPermission.request,
                        shape = RoundedCornerShape(14.dp),
                        enabled = !isSubmitting && !isUploadingImage,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    ) {
                        Text(if (isUploadingImage) "上传中..." else "拍照", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (cameraPermanentlyDenied) {
                        TextButton(onClick = cameraPermission.openSettings) { Text("去系统设置") }
                    }
                    Button(
                        onClick = {
                            imagePickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        enabled = !isSubmitting && !isUploadingImage,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    ) {
                        Text(if (isUploadingImage) "上传中..." else "相册", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (selectedImageUri != null) {
                        Button(
                            onClick = {
                                selectedImageUri = null
                                uploadedRecordImage = null
                                imageUploadMessage = null
                            },
                            shape = RoundedCornerShape(14.dp),
                            enabled = !isSubmitting && !isUploadingImage,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        ) {
                            Text("移除", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                imageUploadMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (uploadedRecordImage != null) Color(0xFF3D6B35) else Color(0xFF8A5A44),
                    )
                }
                imageSaveValidation.message?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8A2E1C),
                    )
                }
            }
        }
        Button(
            onClick = {
                val userId = when (sessionState) {
                    is SessionUiState.Remote -> sessionState.session.userId
                    is SessionUiState.Cached -> sessionState.session.userId
                    else -> null
                } ?: return@Button
                val selectedRating = rating ?: return@Button

                coroutineScope.launch {
                    isSubmitting = true
                    val result = submissionCoordinator.submit(
                        clientRequestId = clientRequestId,
                        userId = userId,
                        selectedFood = selectedFood,
                        rating = selectedRating,
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
                rating != null &&
                !commentValidation.hasError &&
                sessionState !is SessionUiState.Loading &&
                sessionState !is SessionUiState.Failure,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 10.dp),
        ) {
            Text(if (isSubmitting) "保存中..." else "保存记录")
        }
        when (val result = submitState) {
            null -> Unit
            is FoodRecordSubmissionResult.Failure -> {
                Text(
                    text = result.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8A2E1C),
                )
            }

            is FoodRecordSubmissionResult.Submitted -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFCF1E6)),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "记录已保存",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isLiking,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            ) {
                                Text(if (isLiking) "点赞中..." else "点赞 ($likeCount)", style = MaterialTheme.typography.bodySmall)
                            }
                            Button(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "SNK 记录分享")
                                        putExtra(Intent.EXTRA_TEXT, buildRecordShareText(foodName = selectedFood.name, rating = rating ?: DEFAULT_RECORD_RATING, comment = comment, recordId = result.recordId, recordTime = result.recordTime))
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "分享"))
                                },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            ) {
                                Text("分享", style = MaterialTheme.typography.bodySmall)
                            }
                            Button(
                                onClick = onBackToSearch,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            ) {
                                Text("返回", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                interactionMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8A2E1C),
                    )
                }
            }

            is FoodRecordSubmissionResult.SavedToDraft -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F2E8)),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "已转存草稿",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "当前无法连接服务端，网络恢复后会自动补传。",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF5B4A42),
                        )
                        Button(
                            onClick = onOpenDrafts,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        ) {
                            Text("查看草稿", style = MaterialTheme.typography.bodySmall)
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
    val rating: Int? = null,
    val comment: String = "",
    val submitState: FoodRecordSubmissionResult? = null,
    val likeCount: Int = 0,
    val interactionMessage: String? = null,
    val isPublic: Boolean = false,
    val imageUploadMessage: String? = null,
    val isUploadingImage: Boolean = false,
)

internal fun RecordCreateTransientState.resetForFoodSwitch(): RecordCreateTransientState = RecordCreateTransientState()

private fun createTempRecordCameraImageUri(context: android.content.Context): Uri {
    val imageDirectory = File(context.cacheDir, "record-camera").apply { mkdirs() }
    val imageFile = File(imageDirectory, "camera-${UUID.randomUUID()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
}

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


