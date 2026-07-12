package com.snk.app.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.snk.app.SnkApplication
import com.snk.app.data.record.FoodRecordCreateResult
import com.snk.app.data.record.FoodRecordCreateFailureReason
import com.snk.app.data.record.FoodRecordImageAttachment
import com.snk.app.data.record.RecordImageUploadResult
import com.snk.app.data.draft.FoodRecordDraftCreateRequest
import java.io.File
import java.util.UUID
import kotlinx.coroutines.launch

@Composable
fun QuickRecordScreen(
    sessionState: SessionUiState,
    initialName: String,
    onSaved: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val application = context.applicationContext as SnkApplication
    val scope = rememberCoroutineScope()
    val clientRequestId = remember { UUID.randomUUID().toString() }
    var name by remember(initialName) { mutableStateOf(initialName) }
    var rating by remember { mutableStateOf<Int?>(null) }
    var comment by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var cameraPermanentlyDenied by remember { mutableStateOf(false) }
    var localImagePath by remember { mutableStateOf<String?>(null) }
    var uploadedImage by remember { mutableStateOf<FoodRecordImageAttachment?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var isBusy by remember { mutableStateOf(false) }

    fun handleSelectedImage(uri: Uri) {
        val localFile = copyQuickRecordImageToPrivateStorage(context, uri)
        localImagePath = localFile?.absolutePath
        selectedImageUri = localFile?.let(Uri::fromFile) ?: uri
        uploadedImage = null
        scope.launch {
            isBusy = true
            message = "正在处理图片…"
            message = try {
                val compressed = com.snk.app.util.ImageCompressor.compress(context, selectedImageUri ?: uri)
                when (val result = application.container.foodRecordRepository.uploadRecordImage(
                    compressed.bytes,
                    compressed.fileName,
                    compressed.contentType,
                )) {
                    is RecordImageUploadResult.Success -> {
                        uploadedImage = result.image
                        "图片已准备好"
                    }
                    is RecordImageUploadResult.Failure -> result.message
                }
            } catch (_: Exception) {
                "图片读取失败；你仍可保存纯文字记录。"
            }
            isBusy = false
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) {
            message = "已取消选择图片；仍可保存纯文字记录。"
        } else {
            handleSelectedImage(uri)
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) handleSelectedImage(uri)
        else message = "拍照未完成；仍可保存纯文字记录。"
    }
    fun launchCamera() {
        val directory = File(context.cacheDir, "record-camera").apply { mkdirs() }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            File(directory, "camera-${UUID.randomUUID()}.jpg"),
        )
        pendingCameraUri = uri
        cameraLauncher.launch(uri)
    }
    val cameraPermission = rememberCameraPermissionController(
        onGranted = ::launchCamera,
        onDenied = { kind ->
            cameraPermanentlyDenied = kind == CameraDenialKind.PERMANENT
            message = if (cameraPermanentlyDenied) {
                "相机权限已被永久拒绝，可去系统设置，或从相册选择/跳过图片。"
            } else {
                "未获得相机权限，可以从相册选择或跳过图片。"
            }
        },
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("快速记录", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("只需名称和评分，记录默认公开。", style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("名称（必填）") },
            singleLine = true,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("评分（必选）", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..5).forEach { stars ->
                    OutlinedButton(
                        onClick = { rating = stars },
                        modifier = Modifier.weight(1f).semantics {
                            selected = rating == stars
                            stateDescription = if (rating == stars) "已选择 $stars 星" else "$stars 星未选择"
                        },
                    ) {
                        Text(if (rating == stars) "★$stars" else "☆$stars")
                    }
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("图片（选填）", fontWeight = FontWeight.SemiBold)
                selectedImageUri?.let {
                    AsyncImage(
                        model = it,
                        contentDescription = "待保存的记录图片",
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        contentScale = ContentScale.Crop,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = cameraPermission.request) {
                        Text("拍照")
                    }
                    OutlinedButton(onClick = {
                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Text("相册")
                    }
                }
                if (cameraPermanentlyDenied) {
                    TextButton(onClick = cameraPermission.openSettings) { Text("去系统设置") }
                }
            }
        }
        OutlinedTextField(
            value = comment,
            onValueChange = { comment = it.take(500) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("备注（选填）") },
            minLines = 3,
        )
        message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        Button(
            onClick = {
                val userId = sessionState.userIdOrNull() ?: return@Button
                val selectedRating = rating ?: return@Button
                scope.launch {
                    isBusy = true
                    when (val result = application.container.foodRecordRepository.createQuickRecord(
                        clientRequestId = clientRequestId,
                        userId = userId,
                        name = name,
                        rating = selectedRating,
                        comment = comment,
                        isPublic = true,
                        images = listOfNotNull(uploadedImage),
                    )) {
                        is FoodRecordCreateResult.Success -> {
                            localImagePath?.let { File(it).delete() }
                            onSaved()
                        }
                        is FoodRecordCreateResult.Failure -> {
                            if (result.reason == FoodRecordCreateFailureReason.NETWORK) {
                                val draft = application.container.draftRecordRepository.createDraft(
                                    FoodRecordDraftCreateRequest(
                                        userId = userId,
                                        foodItemId = null,
                                        foodName = name.trim(),
                                        category = "none",
                                        subcategory = null,
                                        brand = null,
                                        barcode = null,
                                        rating = selectedRating,
                                        comment = comment,
                                        sourceType = "quick_record",
                                        isPublic = true,
                                        clientRequestId = clientRequestId,
                                        localImagePath = localImagePath,
                                    ),
                                )
                                application.container.scheduleDraftRetry(draft.id)
                                message = "网络不可用，已保存到“需要处理”并等待补传。"
                                onSaved()
                            } else {
                                message = result.message
                            }
                        }
                    }
                    isBusy = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = name.isNotBlank() && rating != null && sessionState.userIdOrNull() != null && !isBusy,
        ) {
            Text(if (isBusy) "保存中…" else "保存公开记录")
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("返回") }
    }
}

private fun copyQuickRecordImageToPrivateStorage(context: Context, uri: Uri): File? = runCatching {
    val directory = File(context.filesDir, "record-drafts").apply { mkdirs() }
    val target = File(directory, "${UUID.randomUUID()}.jpg")
    context.contentResolver.openInputStream(uri).use { input ->
        requireNotNull(input)
        target.outputStream().use { output -> input.copyTo(output) }
    }
    target
}.getOrNull()
