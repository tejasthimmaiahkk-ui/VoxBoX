package me.thimmaiah.voxbox.board

import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Adapts board sampling to how fast the speaker is going.
 *
 * A lecturer who has sped up is covering more ground per minute, so the board changes more often
 * and each state is on screen for less time. Fixed settings chosen before the lecture are wrong
 * within minutes: too slow and whole board states are never sampled, too fast and the same
 * unchanged board is compared over and over.
 *
 * Pace is measured in words per minute from the transcript, which the app already produces — no
 * extra model call, no extra permission, and it works for any language whose words are
 * whitespace-separated.
 *
 * Two knobs move together:
 *
 *  - **Interval** shortens as pace rises, so a fast speaker is sampled more often.
 *  - **Threshold** falls as pace rises, so smaller board changes still register — a speaker in a
 *    hurry adds a line at a time rather than rewriting the board.
 *
 * Both are clamped and smoothed. A single fast sentence must not swing the settings, so the pace
 * is an exponential moving average over recent chunks.
 */
class SpeechPaceTracker(
    private val smoothing: Double = 0.35,
    private val baselineWpm: Int = 130,
) {
    private var averageWpm: Double = 0.0
    private var samples = 0

    /** Words per minute after smoothing, or 0 before the first chunk. */
    val wordsPerMinute: Int get() = averageWpm.roundToInt()

    /** Feeds one transcribed chunk. Returns the smoothed pace. */
    fun observe(text: String, durationMs: Long): Int {
        if (durationMs <= 0) return wordsPerMinute
        val words = text.trim().split(Regex("\\s+")).count { it.isNotBlank() }
        if (words == 0) return wordsPerMinute
        val instant = words * 60_000.0 / durationMs
        averageWpm = if (samples == 0) instant else averageWpm + smoothing * (instant - averageWpm)
        samples += 1
        return wordsPerMinute
    }

    fun reset() {
        averageWpm = 0.0
        samples = 0
    }

    /**
     * Suggested settings for the current pace, or null before there is enough evidence.
     *
     * Scaled from the settings the session *started* with, never from the last suggestion. An
     * earlier version divided the current value each time, so every chunk compounded the last
     * adjustment: on a real device the interval walked to its ceiling while the threshold walked
     * to its floor, and the two knobs ended up describing opposite paces. Anchoring both to the
     * session baseline means a given pace always maps to the same settings, and a speaker who
     * slows back down gets the original values back.
     *
     * Returns null for the first chunk: acting on a single sample would swing the settings on the
     * strength of one sentence, which is exactly the instability this is meant to remove.
     */
    fun suggest(baseIntervalMs: Long, baseThreshold: Double): PaceSuggestion? {
        if (samples < 2 || averageWpm <= 0) return null
        // 1.0 at the baseline pace, above 1 when faster. Clamped so an outlier cannot dominate.
        val ratio = (averageWpm / baselineWpm).coerceIn(0.6, 1.8)
        val interval = (baseIntervalMs / ratio)
            .roundToLong()
            .coerceIn(MIN_INTERVAL_MS, MAX_INTERVAL_MS)
        val threshold = (baseThreshold / ratio).coerceIn(MIN_THRESHOLD, MAX_THRESHOLD)
        return PaceSuggestion(
            wordsPerMinute = wordsPerMinute,
            intervalMs = interval,
            threshold = threshold,
        )
    }

    companion object {
        const val MIN_INTERVAL_MS = 3_000L
        const val MAX_INTERVAL_MS = 20_000L
        const val MIN_THRESHOLD = 0.08
        const val MAX_THRESHOLD = 0.30
    }
}

data class PaceSuggestion(
    val wordsPerMinute: Int,
    val intervalMs: Long,
    val threshold: Double,
)
