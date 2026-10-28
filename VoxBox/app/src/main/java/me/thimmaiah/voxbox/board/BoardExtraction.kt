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

/**
 * Joins regions that are pieces of one drawing.
 *
 * The vision model often boxes a labelled diagram part by part — the nucleus, then the arrow,
 * then the emitted particle — so one figure became three crops that each made no sense alone.
 * Boxes that overlap or sit within [gap] of each other (normalised frame units) are unioned until
 * nothing more merges; genuinely separate drawings on the same board stay separate.
 */
internal fun mergeAdjacentRegions(regions: List<DiagramRegion>, gap: Double = 0.06): List<DiagramRegion> {
    val merged = regions.toMutableList()
    var changed = true
    while (changed) {
        changed = false
        loop@ for (i in merged.indices) {
            for (j in i + 1 until merged.size) {
                val a = merged[i]
                val b = merged[j]
                val apart = a.left > b.left + b.width + gap || b.left > a.left + a.width + gap ||
                    a.top > b.top + b.height + gap || b.top > a.top + a.height + gap
                if (apart) continue
                val left = minOf(a.left, b.left)
                val top = minOf(a.top, b.top)
                val right = maxOf(a.left + a.width, b.left + b.width)
                val bottom = maxOf(a.top + a.height, b.top + b.height)
                val caption = listOf(a.caption, b.caption).filter(String::isNotBlank).distinct().joinToString("; ")
                merged[i] = DiagramRegion(left, top, right - left, bottom - top, caption)
                merged.removeAt(j)
                changed = true
                break@loop
            }
        }
    }
    return merged
}
