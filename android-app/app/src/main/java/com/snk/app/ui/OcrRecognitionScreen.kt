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
import com.snk.app.data.food.FoodOcrSearchResult
import com.snk.app.data.food.FoodSearchItem
import com.snk.app.data.food.FoodSearchResult
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

@Composable
fun OcrRecognitionScreen(
    onFoodMatched: (FoodSearchItem) -> Unit,
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
    var statusMessage by remember { mutableStateOf("先从相册选择一张零食或美食图片，本地识别出文字后会自动搜索。") }

    suspend fun runLocalOcr(uri: Uri) {
        isProcessing = true
        searchState = null
        recognizedText = null
        attemptedQueries.clear()
        statusMessage = "正在本地识别图片文字..."

        try {
            val rawText = recognizeText(context, uri)
            val normalizedText = rawText.trim()
            if (normalizedText.isBlank()) {
                statusMessage = "本地 OCR 没有识别到清晰文字，后续可继续接服务端 OCR 兜底。"
                isProcessing = false
                return
            }

            when (val result = application.container.foodSearchRepository.searchByRecognizedText(normalizedText)) {
                is FoodOcrSearchResult.Success -> {
                    recognizedText = result.recognizedText
                    attemptedQueries.addAll(result.attemptedQueries)
                    searchState = result.result
                    statusMessage = "本地 OCR 已命中结果，当前使用查询：${result.matchedQuery}"
                }

                is FoodOcrSearchResult.NoMatch -> {
                    recognizedText = result.recognizedText
                    attemptedQueries.addAll(result.attemptedQueries)
                    searchState = FoodSearchResult.Success(emptyList(), "weak")
                    statusMessage = "本地 OCR 已提取文字，但还没有命中条目，下一步可手动创建待审核条目。"
                }

                is FoodOcrSearchResult.Failure -> {
                    recognizedText = result.recognizedText.ifBlank { null }
                    attemptedQueries.addAll(result.attemptedQueries)
                    searchState = FoodSearchResult.Failure(result.message)
                    statusMessage = result.message
                }
            }
        } catch (exception: IOException) {
            searchState = FoodSearchResult.Failure("无法读取所选图片，请重新选择。")
            statusMessage = "读取图片失败，请重新选择一张清晰图片。"
        } catch (exception: Exception) {
            searchState = FoodSearchResult.Failure("本地 OCR 识别失败，请稍后重试。")
            statusMessage = "本地 OCR 识别失败，后续可接服务端 OCR 兜底。"
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
            text = "这一阶段先走本地 OCR 文本召回，不命中时再进入后续的服务端 OCR 和图像识别兜底。",
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
            onCreateRecord = onFoodMatched,
            noResultActionLabel = if (recognizedText != null) "识别失败？手动创建" else null,
            onNoResultAction = if (recognizedText != null) {
                { onOpenManualCreate(attemptedQueries.firstOrNull() ?: recognizedText.orEmpty()) }
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
