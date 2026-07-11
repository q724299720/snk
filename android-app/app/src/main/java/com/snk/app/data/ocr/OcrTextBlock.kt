package com.snk.app.data.ocr

data class OcrPoint(val x: Float, val y: Float)

data class OcrBoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    fun contains(x: Float, y: Float): Boolean = x in left..right && y in top..bottom
}

data class OcrTextBlock(
    val id: String,
    val text: String,
    val boundingBox: OcrBoundingBox?,
    val cornerPoints: List<OcrPoint>,
    val isLikelyNoise: Boolean,
)

data class OcrRecognitionResult(
    val fullText: String,
    val imageWidth: Int,
    val imageHeight: Int,
    val rotationDegrees: Int,
    val blocks: List<OcrTextBlock>,
)

fun isLikelyOcrNoise(text: String): Boolean {
    val normalized = text.trim().replace(" ", "")
    if (normalized.isBlank()) return true
    if (normalized.matches(Regex("[0-9\uFF10-\uFF19/.年月日:：-]{5,}"))) return true
    if (normalized.matches(Regex("[0-9]{8,}"))) return true
    return listOf("净含量", "配料表", "生产厂家", "制造商", "地址", "保质期", "执行标准", "条形码")
        .any(normalized::contains)
}

fun mapOcrBoxToFit(
    box: OcrBoundingBox,
    imageWidth: Float,
    imageHeight: Float,
    containerWidth: Float,
    containerHeight: Float,
): OcrBoundingBox {
    if (imageWidth <= 0f || imageHeight <= 0f || containerWidth <= 0f || containerHeight <= 0f) {
        return OcrBoundingBox(0f, 0f, 0f, 0f)
    }
    val scale = minOf(containerWidth / imageWidth, containerHeight / imageHeight)
    val offsetX = (containerWidth - imageWidth * scale) / 2f
    val offsetY = (containerHeight - imageHeight * scale) / 2f
    return OcrBoundingBox(
        left = offsetX + box.left * scale,
        top = offsetY + box.top * scale,
        right = offsetX + box.right * scale,
        bottom = offsetY + box.bottom * scale,
    )
}
