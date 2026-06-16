package com.snk.app.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.snk.app.SnkApplication
import java.io.IOException
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

@Composable
fun OcrRecognitionScreen(
    onFillSearchQuery: (String) -> Unit,
    onOpenManualCreate: (String) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val application = context.applicationContext as SnkApplication
    val coroutineScope = rememberCoroutineScope()
    val attemptedQueries = remember { mutableStateListOf<String>() }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var recognizedText by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var canOpenManualCreate by remember { mutableStateOf(false) }
    var manualCreateSeedName by remember { mutableStateOf("") }
    var statusMessage by remember {
        mutableStateOf("先从相册选择一张零食或美食图片，系统会优先在本地提取文字，必要时再切到服务端 OCR。")
    }

    fun returnToSearch(query: String, message: String) {
        onFillSearchQuery(query)
        statusMessage = message
        onBack()
    }

    suspend fun runServerOcrFallback(
        uri: Uri,
        clientRecognizedText: String?,
        fallbackReason: String,
    ) {
        statusMessage = fallbackReason
        val imagePayload = try {
            readImagePayload(context, uri)
        } catch (exception: IOException) {
            canOpenManualCreate = true
            manualCreateSeedName = clientRecognizedText.orEmpty()
            statusMessage = "读取图片失败，当前请直接手动创建。"
            return
        }

        when (
            val result = application.container.foodSearchRepository.searchByServerOcr(
                imageBytes = imagePayload.bytes,
                fileName = imagePayload.fileName,
                contentType = imagePayload.contentType,
                clientRecognizedText = clientRecognizedText,
            )
        ) {
            is com.snk.app.data.food.FoodOcrSearchResult.Success -> {
                recognizedText = result.recognizedText
                attemptedQueries.clear()
                attemptedQueries.addAll(result.attemptedQueries)
                returnToSearch(
                    query = result.matchedQuery.ifBlank {
                        result.attemptedQueries.firstOrNull().orEmpty()
                    },
                    message = "服务端 OCR 已提取到可搜索文字，正在回填搜索框。",
                )
            }

            is com.snk.app.data.food.FoodOcrSearchResult.NoMatch -> {
                recognizedText = result.recognizedText.ifBlank { clientRecognizedText ?: "" }.ifBlank { null }
                attemptedQueries.clear()
                attemptedQueries.addAll(result.attemptedQueries)
                val fallbackQuery = result.attemptedQueries.firstOrNull()
                    ?: result.recognizedText.ifBlank { clientRecognizedText.orEmpty() }
                if (fallbackQuery.isNotBlank()) {
                    returnToSearch(
                        query = fallbackQuery,
                        message = "OCR 已提取文字，但还未命中结果，已回填到搜索框继续确认。",
                    )
                } else {
                    canOpenManualCreate = true
                    manualCreateSeedName = clientRecognizedText.orEmpty()
                    statusMessage = "服务端 OCR 没有提取到可用文字，请直接手动创建。"
                }
            }

            is com.snk.app.data.food.FoodOcrSearchResult.Failure -> {
                recognizedText = result.recognizedText.ifBlank { clientRecognizedText ?: "" }.ifBlank { null }
                attemptedQueries.clear()
                attemptedQueries.addAll(result.attemptedQueries)
                val fallbackQuery = result.attemptedQueries.firstOrNull()
                    ?: result.recognizedText.ifBlank { clientRecognizedText.orEmpty() }
                if (fallbackQuery.isNotBlank()) {
                    returnToSearch(
                        query = fallbackQuery,
                        message = "服务端 OCR 不稳定，但已提取到文字，已回填到搜索框继续确认。",
                    )
                } else {
                    canOpenManualCreate = true
                    manualCreateSeedName = clientRecognizedText.orEmpty()
                    statusMessage = result.message
                }
            }
        }
    }

    suspend fun runLocalOcr(uri: Uri) {
        isProcessing = true
        recognizedText = null
        attemptedQueries.clear()
        canOpenManualCreate = false
        manualCreateSeedName = ""
        statusMessage = "正在本地识别图片文字..."

        try {
            val rawText = recognizeText(context, uri)
            val normalizedText = rawText.trim()
            if (normalizedText.isBlank()) {
                runServerOcrFallback(
                    uri = uri,
                    clientRecognizedText = null,
                    fallbackReason = "本地 OCR 没有识别到清晰文字，正在切到服务端 OCR 兜底。",
                )
                return
            }

            when (val result = application.container.foodSearchRepository.searchByRecognizedText(normalizedText)) {
                is com.snk.app.data.food.FoodOcrSearchResult.Success -> {
                    recognizedText = result.recognizedText
                    attemptedQueries.addAll(result.attemptedQueries)
                    returnToSearch(
                        query = result.matchedQuery,
                        message = "本地 OCR 已提取到可搜索文字，正在回填搜索框。",
                    )
                }

                is com.snk.app.data.food.FoodOcrSearchResult.NoMatch -> {
                    recognizedText = result.recognizedText
                    attemptedQueries.addAll(result.attemptedQueries)
                    val fallbackQuery = result.attemptedQueries.firstOrNull() ?: result.recognizedText
                    if (fallbackQuery.isNotBlank()) {
                        returnToSearch(
                            query = fallbackQuery,
                            message = "本地 OCR 已提取文字，但还未命中结果，已回填到搜索框继续确认。",
                        )
                    } else {
                        runServerOcrFallback(
                            uri = uri,
                            clientRecognizedText = result.recognizedText,
                            fallbackReason = "本地 OCR 没有整理出可用搜索词，正在切到服务端 OCR 兜底。",
                        )
                    }
                }

                is com.snk.app.data.food.FoodOcrSearchResult.Failure -> {
                    recognizedText = result.recognizedText.ifBlank { null }
                    attemptedQueries.addAll(result.attemptedQueries)
                    runServerOcrFallback(
                        uri = uri,
                        clientRecognizedText = result.recognizedText,
                        fallbackReason = "本地 OCR 识别失败，正在切到服务端 OCR 兜底。",
                    )
                }
            }
        } catch (exception: IOException) {
            canOpenManualCreate = true
            statusMessage = "无法读取所选图片，请重新选择。"
        } catch (exception: Exception) {
            runServerOcrFallback(
                uri = uri,
                clientRecognizedText = null,
                fallbackReason = "本地 OCR 识别失败，正在切到服务端 OCR 兜底。",
            )
        } finally {
            isProcessing = false
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        selectedImageUri = uri
        if (uri == null) {
            statusMessage = "已取消选择图片。"
            return@rememberLauncherForActivityResult
        }
        coroutineScope.launch {
            runLocalOcr(uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "OCR 文字提取",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = Color(0xFF2B1E18),
        )
        Text(
            text = "这里不直接做独立识别确认，只负责把图片里的文字提取出来并回填到主搜索框。",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF5B4A42),
        )
        Button(
            onClick = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            enabled = !isProcessing,
            shape = RoundedCornerShape(18.dp),
        ) {
            Text(if (isProcessing) "识别中..." else "选择图片")
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F2E8)),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "状态",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF5B4A42),
                )
                selectedImageUri?.let { uri ->
                    Text(
                        text = "图片 URI: $uri",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8A5A44),
                    )
                }
            }
        }
        recognizedText?.let { text ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFCF1E6)),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "识别文本",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF5B4A42),
                    )
                    if (attemptedQueries.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            attemptedQueries.forEach { query ->
                                AssistChip(
                                    onClick = {},
                                    enabled = false,
                                    label = { Text(query) },
                                )
                            }
                        }
                    }
                }
            }
        }
        if (canOpenManualCreate) {
            Button(
                onClick = { onOpenManualCreate(manualCreateSeedName) },
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("识别失败？手动创建")
            }
        }
        Button(
            onClick = onBack,
            shape = RoundedCornerShape(16.dp),
        ) {
            Text("返回搜索")
        }
    }
}

private data class ImagePayload(
    val bytes: ByteArray,
    val fileName: String,
    val contentType: String,
)

private fun readImagePayload(context: Context, imageUri: Uri): ImagePayload {
    val resolver = context.contentResolver
    val bytes = resolver.openInputStream(imageUri)?.use { it.readBytes() }
        ?: throw IOException("无法读取图片数据")
    val contentType = resolver.getType(imageUri) ?: "image/jpeg"
    val fileName = "ocr-${UUID.randomUUID()}.${contentType.substringAfter('/', "jpg")}"
    return ImagePayload(
        bytes = bytes,
        fileName = fileName,
        contentType = contentType,
    )
}

private suspend fun recognizeText(
    context: Context,
    imageUri: Uri,
): String = suspendCancellableCoroutine { continuation ->
    val recognizer = TextRecognition.getClient(
        ChineseTextRecognizerOptions.Builder().build(),
    )

    try {
        val image = InputImage.fromFilePath(context, imageUri)
        recognizer.process(image)
            .addOnSuccessListener { result ->
                continuation.resume(result.text)
            }
            .addOnFailureListener { error ->
                continuation.resumeWithException(error)
            }
            .addOnCompleteListener {
                recognizer.close()
            }
    } catch (exception: Exception) {
        recognizer.close()
        continuation.resumeWithException(exception)
    }
}
