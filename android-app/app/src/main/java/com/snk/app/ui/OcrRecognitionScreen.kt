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
import com.snk.app.data.food.FoodImageRecognitionResult
import com.snk.app.data.food.FoodReportResult
import com.snk.app.data.food.FoodOcrSearchResult
import com.snk.app.data.food.FoodSearchResult
import java.io.IOException
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

@Composable
fun OcrRecognitionScreen(
    sessionUserId: Long?,
    onCandidatesMatched: (CandidateConfirmationState) -> Unit,
    onOpenManualCreate: (String) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val application = context.applicationContext as SnkApplication
    val coroutineScope = rememberCoroutineScope()
    val attemptedQueries = remember { mutableStateListOf<String>() }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var recognizedText by remember { mutableStateOf<String?>(null) }
    var searchState by remember { mutableStateOf<FoodSearchResult?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var canOpenManualCreate by remember { mutableStateOf(false) }
    var manualCreateSeedName by remember { mutableStateOf("") }
    var statusMessage by remember {
        mutableStateOf("先从相册选择一张零食或美食图片，系统会按 本地 OCR -> 服务端 OCR -> 图片识别 的顺序自动尝试。")
    }

    suspend fun runImageRecognitionFallback(
        imagePayload: ImagePayload,
        fallbackReason: String,
        manualSeedName: String,
    ) {
        val userId = sessionUserId
        if (userId == null) {
            searchState = FoodSearchResult.Failure("匿名身份尚未就绪，无法继续服务端图片识别。")
            canOpenManualCreate = true
            manualCreateSeedName = manualSeedName
            statusMessage = "匿名身份尚未就绪，当前请直接手动创建。"
            return
        }

        statusMessage = fallbackReason
        when (
            val result = application.container.foodSearchRepository.searchByImageRecognition(
                userId = userId,
                imageBytes = imagePayload.bytes,
                fileName = imagePayload.fileName,
                contentType = imagePayload.contentType,
            )
        ) {
            is FoodImageRecognitionResult.Success -> {
                searchState = result.result
                statusMessage = "图片识别已召回候选，正在进入确认页。"
                onCandidatesMatched(
                    CandidateConfirmationState(
                        sourceLabel = "图片识别候选确认",
                        title = "请确认图片识别到的食物条目",
                        description = "本地 OCR 和服务端 OCR 都未稳定命中后，已继续使用图片识别召回候选，请确认正确条目后再进入记录页面。",
                        items = result.result.items,
                        qualitySignal = result.result.qualitySignal,
                        sourceType = "image_search",
                        matchedQuery = result.imageUrl,
                        manualCreateSeedName = manualSeedName,
                    ),
                )
            }

            is FoodImageRecognitionResult.NoMatch -> {
                searchState = FoodSearchResult.Success(emptyList(), "weak")
                canOpenManualCreate = true
                manualCreateSeedName = manualSeedName
                statusMessage = "图片识别也没有返回可靠候选，当前可直接手动创建待审核条目。"
            }

            is FoodImageRecognitionResult.Failure -> {
                searchState = FoodSearchResult.Failure(result.message)
                canOpenManualCreate = true
                manualCreateSeedName = manualSeedName
                statusMessage = result.message
            }
        }
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
            searchState = FoodSearchResult.Failure("无法读取上传图片，当前请直接手动创建。")
            canOpenManualCreate = true
            manualCreateSeedName = clientRecognizedText.orEmpty()
            statusMessage = "读取图片失败，当前无法继续服务端识别，请直接手动创建。"
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
            is FoodOcrSearchResult.Success -> {
                recognizedText = result.recognizedText
                attemptedQueries.clear()
                attemptedQueries.addAll(result.attemptedQueries)
                searchState = result.result
                statusMessage = "服务端 OCR 已召回候选，正在进入确认页。"
                onCandidatesMatched(
                    CandidateConfirmationState(
                        sourceLabel = "服务端 OCR 候选确认",
                        title = "请确认识别到的食物条目",
                        description = "本地 OCR 未完成命中后，已继续走服务端 OCR 文本召回，确认正确后再进入记录页面。",
                        items = result.result.items,
                        qualitySignal = result.result.qualitySignal,
                        sourceType = "image_search",
                        recognizedText = result.recognizedText,
                        matchedQuery = result.matchedQuery,
                        attemptedQueries = result.attemptedQueries,
                        manualCreateSeedName = result.attemptedQueries.firstOrNull() ?: result.recognizedText,
                    ),
                )
            }

            is FoodOcrSearchResult.NoMatch -> {
                recognizedText = result.recognizedText.ifBlank { clientRecognizedText ?: "" }.ifBlank { null }
                attemptedQueries.clear()
                attemptedQueries.addAll(result.attemptedQueries)
                searchState = FoodSearchResult.Success(emptyList(), "weak")
                runImageRecognitionFallback(
                    imagePayload = imagePayload,
                    fallbackReason = "服务端 OCR 也没有命中条目，正在继续尝试图片识别。",
                    manualSeedName = result.attemptedQueries.firstOrNull()
                        ?: result.recognizedText.ifBlank { clientRecognizedText.orEmpty() },
                )
            }

            is FoodOcrSearchResult.Failure -> {
                recognizedText = result.recognizedText.ifBlank { clientRecognizedText ?: "" }.ifBlank { null }
                searchState = FoodSearchResult.Failure(result.message)
                runImageRecognitionFallback(
                    imagePayload = imagePayload,
                    fallbackReason = "服务端 OCR 失败，正在继续尝试图片识别。",
                    manualSeedName = clientRecognizedText.orEmpty(),
                )
            }
        }
    }

    suspend fun runLocalOcr(uri: Uri) {
        isProcessing = true
        searchState = null
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
                is FoodOcrSearchResult.Success -> {
                    recognizedText = result.recognizedText
                    attemptedQueries.addAll(result.attemptedQueries)
                    searchState = result.result
                    statusMessage = "本地 OCR 已召回候选，正在进入确认页。"
                    onCandidatesMatched(
                        CandidateConfirmationState(
                            sourceLabel = "OCR 候选确认",
                            title = "请确认识别到的食物条目",
                            description = "已先走本地 OCR 文本召回，确认正确后再进入记录页面。",
                            items = result.result.items,
                            qualitySignal = result.result.qualitySignal,
                            sourceType = "image_search",
                            recognizedText = result.recognizedText,
                            matchedQuery = result.matchedQuery,
                            attemptedQueries = result.attemptedQueries,
                            manualCreateSeedName = result.attemptedQueries.firstOrNull() ?: result.recognizedText,
                        ),
                    )
                }

                is FoodOcrSearchResult.NoMatch -> {
                    recognizedText = result.recognizedText
                    attemptedQueries.addAll(result.attemptedQueries)
                    searchState = FoodSearchResult.Success(emptyList(), "weak")
                    runServerOcrFallback(
                        uri = uri,
                        clientRecognizedText = result.recognizedText,
                        fallbackReason = "本地 OCR 已提取文字但没有命中条目，正在继续服务端 OCR 兜底。",
                    )
                }

                is FoodOcrSearchResult.Failure -> {
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
            searchState = FoodSearchResult.Failure("无法读取所选图片，请重新选择。")
            statusMessage = "读取图片失败，请重新选择一张清晰图片。"
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
            text = "图片识别",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = Color(0xFF2B1E18),
        )
        Text(
            text = "这一阶段先走本地 OCR 文本召回，不命中时再进入后续的服务端 OCR 和图片识别兜底。",
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
        FoodSearchResultsCard(
            searchState = searchState,
            isSearching = isProcessing,
            emptyHint = "选择图片并识别文字后，会在这里展示文本召回结果。",
            onCreateRecord = {},
            onReportItem = { item ->
                val userId = sessionUserId
                if (userId == null) {
                    statusMessage = "匿名身份尚未就绪，暂时无法提交纠错。"
                } else {
                    coroutineScope.launch {
                        statusMessage = when (
                            val result = application.container.foodSearchRepository.reportFoodItem(
                                userId = userId,
                                foodItemId = item.id,
                            )
                        ) {
                            is FoodReportResult.Success -> "已提交纠错信号，当前 reportCount = ${result.reportCount}"
                            is FoodReportResult.Failure -> result.message
                        }
                    }
                }
            },
            noResultActionLabel = if (canOpenManualCreate) "识别失败？手动创建" else null,
            onNoResultAction = if (canOpenManualCreate) {
                { onOpenManualCreate(manualCreateSeedName) }
            } else {
                null
            },
        )
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
