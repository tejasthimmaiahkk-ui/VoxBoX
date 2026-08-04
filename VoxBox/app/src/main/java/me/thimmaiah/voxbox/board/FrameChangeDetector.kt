package me.thimmaiah.voxbox.board

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.security.MessageDigest
import kotlin.math.abs
import kotlin.math.max

data class FrameChangeMetrics(
    val score: Double,
    val changedFraction: Double,
    val exposureShift: Double,
)

data class FrameChangeDecision(
    val accepted: Boolean,
    val score: Double,
    val fingerprint: String,
    val reason: String,
    internal val candidateToken: Long? = null,
)

/**
 * Compares a tiny luminance sample instead of uploading every scheduled camera frame.
 * The score compensates for a global exposure shift so projector flicker or auto-exposure
 * is less likely to be mistaken for new board content.
 */
class FrameChangeDetector(
    private val sampleSide: Int = 32,
    private val centeredPixelThreshold: Int = 22,
) {
    private var lastAcceptedLuma: IntArray? = null
    private var pendingCandidate: PendingFrameCandidate? = null
    private var nextCandidateToken = 1L

    fun evaluate(jpegBytes: ByteArray, threshold: Double): FrameChangeDecision {
        require(threshold in 0.0..1.0) { "threshold must be between 0 and 1" }
        val sample = decodeLumaSample(jpegBytes, sampleSide)
        return evaluateSample(sample, threshold)
    }

    internal fun evaluateSample(sample: IntArray, threshold: Double): FrameChangeDecision {
        require(sample.isNotEmpty())
        require(threshold in 0.0..1.0) { "threshold must be between 0 and 1" }
        val fingerprint = perceptualFingerprint(sample)
        val previous = lastAcceptedLuma
        if (previous == null) {
            return acceptedCandidate(
                sample = sample,
                accepted = true,
                score = 1.0,
                fingerprint = fingerprint,
                reason = "First frame establishes the board baseline.",
            )
        }
        val metrics = compareLuma(previous, sample, centeredPixelThreshold)
        val accepted = metrics.score >= threshold
        return if (accepted) {
            acceptedCandidate(
                sample = sample,
                accepted = true,
                score = metrics.score,
                fingerprint = fingerprint,
                reason = "Meaningful board change detected.",
            )
        } else {
            pendingCandidate = null
            FrameChangeDecision(
                accepted = false,
                score = metrics.score,
                fingerprint = fingerprint,
                reason = "Frame is similar to the last committed board state.",
            )
        }
    }

    /** Commits the comparison baseline only after extraction and note persistence succeed. */
    fun commit(decision: FrameChangeDecision): Boolean {
        val candidate = pendingCandidate ?: return false
        if (!decision.accepted || decision.candidateToken != candidate.token) return false
        lastAcceptedLuma = candidate.sample
        pendingCandidate = null
        return true
    }

    /** Leaves the last successful baseline unchanged so a failed frame can be retried. */
    fun discard(decision: FrameChangeDecision) {
        if (decision.candidateToken == pendingCandidate?.token) pendingCandidate = null
    }

    fun reset() {
        lastAcceptedLuma = null
        pendingCandidate = null
        nextCandidateToken = 1L
    }

    private fun acceptedCandidate(
        sample: IntArray,
        accepted: Boolean,
        score: Double,
        fingerprint: String,
        reason: String,
    ): FrameChangeDecision {
        val token = nextCandidateToken++
        pendingCandidate = PendingFrameCandidate(token, sample.copyOf())
        return FrameChangeDecision(
            accepted = accepted,
            score = score,
            fingerprint = fingerprint,
            reason = reason,
            candidateToken = token,
        )
    }
}

private data class PendingFrameCandidate(
    val token: Long,
    val sample: IntArray,
)

internal fun compareLuma(
    previous: IntArray,
    current: IntArray,
    centeredPixelThreshold: Int = 22,
): FrameChangeMetrics {
    require(previous.isNotEmpty() && previous.size == current.size)
    val previousMean = previous.average()
    val currentMean = current.average()
    val exposureShift = abs(currentMean - previousMean) / 255.0
    var centeredDifference = 0.0
    var changed = 0
    for (index in previous.indices) {
        val delta = abs((current[index] - currentMean) - (previous[index] - previousMean))
        centeredDifference += delta
        if (delta >= centeredPixelThreshold) changed += 1
    }
    val meanCenteredDifference = centeredDifference / previous.size / 255.0
    val changedFraction = changed.toDouble() / previous.size
    // Broad small edits and tight high-contrast writing both contribute to the result.
    val score = (0.65 * meanCenteredDifference + 0.35 * changedFraction).coerceIn(0.0, 1.0)
    return FrameChangeMetrics(
        score = score,
        changedFraction = changedFraction,
        exposureShift = exposureShift,
    )
}

internal fun perceptualFingerprint(luma: IntArray): String {
    require(luma.isNotEmpty())
    val mean = luma.average()
    val bits = ByteArray((luma.size + 7) / 8)
    luma.forEachIndexed { index, value ->
        if (value >= mean) {
            bits[index / 8] = (bits[index / 8].toInt() or (1 shl (7 - index % 8))).toByte()
        }
    }
    return MessageDigest.getInstance("SHA-256")
        .digest(bits)
        .take(8)
        .joinToString("") { byte -> "%02x".format(byte) }
}

private fun decodeLumaSample(jpegBytes: ByteArray, side: Int): IntArray {
    if (jpegBytes.isEmpty()) throw BoardExtractionException("The captured frame was empty.")
    require(side in 8..128) { "sampleSide must be between 8 and 128" }
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        throw BoardExtractionException("The captured frame could not be decoded for change detection.")
    }
    var sampleSize = 1
    while (max(bounds.outWidth, bounds.outHeight) / sampleSize > side * 4) sampleSize *= 2
    val decoded = BitmapFactory.decodeByteArray(
        jpegBytes,
        0,
        jpegBytes.size,
        BitmapFactory.Options().apply { inSampleSize = sampleSize },
    ) ?: throw BoardExtractionException("The captured frame could not be decoded for change detection.")
    val scaled = Bitmap.createScaledBitmap(decoded, side, side, true)
    if (scaled !== decoded) decoded.recycle()
    return try {
        val pixels = IntArray(side * side)
        scaled.getPixels(pixels, 0, side, 0, 0, side, side)
        IntArray(pixels.size) { index ->
            val color = pixels[index]
            val red = color shr 16 and 0xFF
            val green = color shr 8 and 0xFF
            val blue = color and 0xFF
            (red * 299 + green * 587 + blue * 114) / 1_000
        }
    } finally {
        scaled.recycle()
    }
}
