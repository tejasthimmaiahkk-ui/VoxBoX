package me.thimmaiah.voxbox.board

enum class BoardExtractionSource {
    REMOTE_VISION,
    MOCK_PROXY,
    OFFLINE_OCR,
}

data class BoardExtraction(
    val title: String,
    val summary: String,
    val visibleText: String,
    val concepts: List<String>,
    val confidence: Double,
    val warnings: List<String>,
    val source: BoardExtractionSource,
    val equations: List<String> = emptyList(),
    val diagramRegions: List<DiagramRegion> = emptyList(),
)

data class DiagramRegion(
    val left: Double,
    val top: Double,
    val width: Double,
    val height: Double,
    val caption: String,
) {
    fun isValid(): Boolean =
        left in 0.0..1.0 && top in 0.0..1.0 &&
            width > 0.0 && height > 0.0 &&
            left + width <= 1.000001 && top + height <= 1.000001
}

interface BoardExtractionClient {
    suspend fun extract(jpegBytes: ByteArray): BoardExtraction
}
