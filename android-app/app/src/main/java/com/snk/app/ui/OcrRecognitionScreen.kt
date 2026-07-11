package com.snk.app.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.snk.app.data.food.OcrSearchQueryBuilder
import com.snk.app.data.ocr.OcrBoundingBox
import com.snk.app.data.ocr.OcrPoint
import com.snk.app.data.ocr.OcrRecognitionResult
import com.snk.app.data.ocr.OcrTextBlock
import com.snk.app.data.ocr.isLikelyOcrNoise
import com.snk.app.data.ocr.mapOcrBoxToFit
import java.io.File
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

@Composable
fun OcrRecognitionScreen(
    sessionState: SessionUiState,
    onFillSearchQuery: (String, List<String>) -> Unit,
    onOpenManualCreate: (String) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val selectedIds = remember { mutableStateListOf<String>() }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var result by remember { mutableStateOf<OcrRecognitionResult?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var showAll by remember { mutableStateOf(false) }
    var editorText by remember { mutableStateOf("") }
    var showEditor by remember { mutableStateOf(false) }
    var permanentDenial by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("请选择食品包装图片。OCR 只提取文字，搜索前由你确认。") }

    fun search(text: String) {
        val normalized = text.trim()
        if (normalized.isBlank()) return
        onFillSearchQuery(normalized, OcrSearchQueryBuilder.buildDisplayQueries(normalized))
    }

    suspend fun runOcr(uri: Uri) {
        isProcessing = true
        result = null
        selectedIds.clear()
        statusMessage = "正在本地识别文字…"
        try {
            val recognized = recognizeStructuredText(context, uri)
            result = recognized
            editorText = recognized.fullText
            when {
                recognized.fullText.isBlank() -> statusMessage = "没有识别到有效文字，可重新拍照、选择相册或手动输入。"
                recognized.blocks.none { it.boundingBox != null } -> {
                    statusMessage = "文字坐标不可用，已切换到全文编辑。"
                    showEditor = true
                }
                else -> statusMessage = "点击图片中的文字块，可多选并按点击顺序组合搜索词。"
            }
        } catch (_: Exception) {
            statusMessage = "本地识别失败，可重新选择图片或手动输入。"
            showEditor = true
        } finally {
            isProcessing = false
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingCameraUri
        if (!success || uri == null) {
            statusMessage = "已取消拍照；仍可从相册选择或手动输入。"
        } else {
            selectedImageUri = uri
            scope.launch { runOcr(uri) }
        }
    }
    fun launchCamera() {
        val uri = createTempOcrCameraImageUri(context)
        pendingCameraUri = uri
        cameraLauncher.launch(uri)
    }
    val cameraPermission = rememberCameraPermissionController(
        onGranted = ::launchCamera,
        onDenied = { kind ->
            permanentDenial = kind == CameraDenialKind.PERMANENT
            statusMessage = if (permanentDenial) {
                "相机权限已被永久拒绝，可去系统设置开启，或继续从相册选择/手动输入。"
            } else {
                "未获得相机权限，可以从相册选择或手动输入。"
            }
        },
    )
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) {
            statusMessage = "已取消选择图片；仍可手动输入。"
        } else {
            selectedImageUri = uri
            scope.launch { runOcr(uri) }
        }
    }

    val selectedText = selectedIds.mapNotNull { id -> result?.blocks?.firstOrNull { it.id == id }?.text }
        .joinToString(" ")

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("选择包装文字", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(statusMessage, style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = cameraPermission.request, enabled = !isProcessing) { Text("重新拍照") }
            OutlinedButton(
                onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                enabled = !isProcessing,
            ) { Text("从相册选择") }
            OutlinedButton(onClick = {
                editorText = result?.fullText.orEmpty()
                showEditor = true
            }) { Text("手动输入") }
        }
        if (permanentDenial) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = cameraPermission.openSettings) { Text("去系统设置") }
                TextButton(onClick = { showEditor = true }) { Text("继续手动输入") }
            }
        }

        val currentResult = result
        val imageUri = selectedImageUri
        if (currentResult != null && imageUri != null && currentResult.fullText.isNotBlank()) {
            OcrImageBlockPicker(
                imageUri = imageUri,
                result = currentResult,
                selectedIds = selectedIds,
                showAll = showAll,
                onToggle = { block ->
                    if (block.id in selectedIds) selectedIds.remove(block.id) else selectedIds.add(block.id)
                },
            )
            TextButton(onClick = { showAll = !showAll }) {
                Text(if (showAll) "隐藏低相关文字" else "显示全部文字")
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                selectedIds.forEach { id ->
                    currentResult.blocks.firstOrNull { it.id == id }?.let { block ->
                        AssistChip(
                            onClick = { selectedIds.remove(id) },
                            modifier = Modifier.semantics {
                                selected = true
                                stateDescription = "已选中，点击删除"
                            },
                            label = { Text("✓ ${block.text}  ×") },
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F2E8)),
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("待搜索文字", fontWeight = FontWeight.SemiBold)
                Text(selectedText.ifBlank { "尚未选择文字块" })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { search(selectedText) }, enabled = selectedText.isNotBlank()) {
                        Text("搜索所选文字")
                    }
                    TextButton(onClick = {
                        editorText = selectedText.ifBlank { currentResult?.fullText.orEmpty() }
                        showEditor = true
                    }) { Text("直接编辑") }
                }
            }
        }
        if (currentResult?.fullText?.isBlank() == true) {
            Button(onClick = { onOpenManualCreate("") }) { Text("手动输入并记录") }
        }
        OutlinedButton(onClick = onBack) { Text("返回") }
    }

    if (showEditor) {
        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text("编辑要搜索的核心名称") },
            text = {
                OutlinedTextField(
                    value = editorText,
                    onValueChange = { editorText = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 6,
                    label = { Text("OCR 原文") },
                )
            },
            confirmButton = {
                Button(onClick = { showEditor = false; search(editorText) }, enabled = editorText.isNotBlank()) {
                    Text("搜索")
                }
            },
            dismissButton = { TextButton(onClick = { showEditor = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun OcrImageBlockPicker(
    imageUri: Uri,
    result: OcrRecognitionResult,
    selectedIds: List<String>,
    showAll: Boolean,
    onToggle: (OcrTextBlock) -> Unit,
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val visibleBlocks = result.blocks.filter { showAll || !it.isLikelyNoise }
    Box(
        modifier = Modifier.fillMaxWidth().height(360.dp).onSizeChanged { canvasSize = it },
    ) {
        AsyncImage(
            model = imageUri,
            contentDescription = "OCR 原图，点击边框选择文字",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
        Canvas(
            modifier = Modifier.fillMaxSize().pointerInput(visibleBlocks, canvasSize) {
                detectTapGestures { tap ->
                    visibleBlocks.asReversed().firstOrNull { block ->
                        block.boundingBox?.let {
                            mapOcrBoxToFit(
                                it,
                                result.imageWidth.toFloat(),
                                result.imageHeight.toFloat(),
                                canvasSize.width.toFloat(),
                                canvasSize.height.toFloat(),
                            ).contains(tap.x, tap.y)
                        } == true
                    }?.let(onToggle)
                }
            },
        ) {
            visibleBlocks.forEach { block ->
                val box = block.boundingBox ?: return@forEach
                val mapped = mapOcrBoxToFit(
                    box,
                    result.imageWidth.toFloat(),
                    result.imageHeight.toFloat(),
                    size.width,
                    size.height,
                )
                val selected = block.id in selectedIds
                if (selected) {
                    drawRect(
                        color = Color(0x6657D38C),
                        topLeft = Offset(mapped.left, mapped.top),
                        size = Size(mapped.right - mapped.left, mapped.bottom - mapped.top),
                    )
                }
                drawRect(
                    color = when {
                        selected -> Color(0xFF087F5B)
                        block.isLikelyNoise -> Color(0xFF9E9E9E)
                        else -> Color(0xFFFF6B35)
                    },
                    topLeft = Offset(mapped.left, mapped.top),
                    size = Size(mapped.right - mapped.left, mapped.bottom - mapped.top),
                    style = Stroke(width = if (selected) 6f else 3f),
                )
            }
        }
    }
}

private fun createTempOcrCameraImageUri(context: Context): Uri {
    val directory = File(context.cacheDir, "ocr-camera").apply { mkdirs() }
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        File(directory, "camera-${UUID.randomUUID()}.jpg"),
    )
}

private suspend fun recognizeStructuredText(context: Context, imageUri: Uri): OcrRecognitionResult =
    suspendCancellableCoroutine { continuation ->
        val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
        try {
            val image = InputImage.fromFilePath(context, imageUri)
            recognizer.process(image)
                .addOnSuccessListener { text ->
                    val rotated = image.rotationDegrees == 90 || image.rotationDegrees == 270
                    val width = if (rotated) image.height else image.width
                    val height = if (rotated) image.width else image.height
                    continuation.resume(
                        OcrRecognitionResult(
                            fullText = text.text,
                            imageWidth = width,
                            imageHeight = height,
                            rotationDegrees = image.rotationDegrees,
                            blocks = text.textBlocks.mapIndexed { index, block ->
                                OcrTextBlock(
                                    id = "block-$index",
                                    text = block.text.trim(),
                                    boundingBox = block.boundingBox?.let {
                                        OcrBoundingBox(it.left.toFloat(), it.top.toFloat(), it.right.toFloat(), it.bottom.toFloat())
                                    },
                                    cornerPoints = block.cornerPoints?.map { OcrPoint(it.x.toFloat(), it.y.toFloat()) }.orEmpty(),
                                    isLikelyNoise = isLikelyOcrNoise(block.text),
                                )
                            }.filter { it.text.isNotBlank() },
                        ),
                    )
                }
                .addOnFailureListener(continuation::resumeWithException)
                .addOnCompleteListener { recognizer.close() }
        } catch (exception: Exception) {
            recognizer.close()
            continuation.resumeWithException(exception)
        }
    }
