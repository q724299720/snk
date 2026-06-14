package com.snk.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.snk.app.SnkApplication
import com.snk.app.data.food.FoodBarcodeLookupResult
import com.snk.app.data.food.FoodSearchItem
import java.util.concurrent.Executors
import kotlinx.coroutines.launch

@Composable
fun BarcodeScanScreen(
    onFoodMatched: (FoodSearchItem) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val application = context.applicationContext as SnkApplication
    val coroutineScope = rememberCoroutineScope()
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var manualBarcode by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("优先使用摄像头扫描条码，也可以手动输入后查询。") }
    var isLookingUp by remember { mutableStateOf(false) }
    var lastHandledBarcode by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            statusMessage = "未授予相机权限，可以先手动输入条码继续。"
        }
    }

    suspend fun lookupBarcode(barcode: String) {
        val normalizedBarcode = barcode.trim()
        if (normalizedBarcode.isBlank() || isLookingUp) {
            return
        }
        isLookingUp = true
        statusMessage = "正在查询条码 $normalizedBarcode ..."
        when (val result = application.container.foodSearchRepository.lookupByBarcode(normalizedBarcode)) {
            is FoodBarcodeLookupResult.Success -> {
                statusMessage = "条码命中 ${result.item.name}，正在进入候选确认页。"
                onFoodMatched(result.item)
            }

            is FoodBarcodeLookupResult.NotFound -> {
                statusMessage = "未找到条码 ${result.barcode} 对应的服务端条目。"
                isLookingUp = false
            }

            is FoodBarcodeLookupResult.Failure -> {
                statusMessage = result.message
                isLookingUp = false
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "条码录入",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = Color(0xFF2B1E18),
        )
        Text(
            text = "零食优先走条码命中，命中后先确认候选结果，再进入记录创建页。",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF5B4A42),
        )
        if (hasCameraPermission) {
            CameraBarcodePreview(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                onBarcodeDetected = { barcode ->
                    if (barcode == lastHandledBarcode || isLookingUp) {
                        return@CameraBarcodePreview
                    }
                    lastHandledBarcode = barcode
                    manualBarcode = barcode
                    coroutineScope.launch {
                        lookupBarcode(barcode)
                    }
                },
            )
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF8F2)),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "相机权限未开启",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "可以重新申请权限，或者直接手动输入条码继续。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF5B4A42),
                    )
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text("重新申请相机权限")
                    }
                }
            }
        }
        OutlinedTextField(
            value = manualBarcode,
            onValueChange = { manualBarcode = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("手动输入条码") },
            singleLine = true,
            enabled = !isLookingUp,
            shape = RoundedCornerShape(20.dp),
        )
        Button(
            onClick = {
                lastHandledBarcode = manualBarcode.trim()
                coroutineScope.launch {
                    lookupBarcode(manualBarcode)
                }
            },
            enabled = !isLookingUp,
            shape = RoundedCornerShape(18.dp),
        ) {
            Text(if (isLookingUp) "查询中..." else "按条码查询")
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

@Composable
private fun CameraBarcodePreview(
    modifier: Modifier = Modifier,
    onBarcodeDetected: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(lifecycleOwner) {
        val scanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E,
                    Barcode.FORMAT_CODE_128,
                )
                .build(),
        )
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val listener = Runnable {
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { previewUseCase ->
                previewUseCase.surfaceProvider = previewView.surfaceProvider
            }
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage == null) {
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                barcodes.firstNotNullOfOrNull { it.rawValue?.trim()?.takeIf(String::isNotBlank) }
                                    ?.let(onBarcodeDetected)
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    }
                }

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis,
            )
        }

        cameraProviderFuture.addListener(listener, mainExecutor)

        onDispose {
            scanner.close()
            cameraProviderFuture.get().unbindAll()
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier,
    )
}
