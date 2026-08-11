package me.thimmaiah.voxbox.board

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechPaceTrackerTest {

    /** Roughly 130 wpm, the assumed baseline for an unhurried lecture. */
    private fun chunk(words: Int, seconds: Long = 20) = " word".repeat(words).trim() to seconds * 1_000

    @Test
    fun measuresPaceFromTheTranscriptItAlreadyHas() {
        val tracker = SpeechPaceTracker()
        val (text, duration) = chunk(words = 50, seconds = 20)

        // 50 words in 20 seconds is 150 a minute.
        assertEquals(150, tracker.observe(text, duration))
    }

    @Test
    fun oneFastSentenceDoesNotSwingTheSettings() {
        val tracker = SpeechPaceTracker()
        tracker.observe(chunk(40).first, chunk(40).second)
        val afterBurst = tracker.observe(chunk(90).first, chunk(90).second)

        // Smoothed, so nowhere near the 270 wpm of the burst on its own.
        assertTrue("pace jumped to $afterBurst", afterBurst < 200)
    }

    @Test
    fun oneChunkIsNotEnoughEvidenceToRetune() {
        val tracker = SpeechPaceTracker()
        tracker.observe(chunk(50).first, chunk(50).second)

        assertNull(tracker.suggest(baseIntervalMs = 8_000, baseThreshold = 0.21))
    }

    @Test
    fun aFasterSpeakerGetsShorterIntervalsAndFinerSensitivity() {
        val tracker = SpeechPaceTracker()
        repeat(4) { tracker.observe(chunk(words = 70).first, chunk(words = 70).second) }

        val suggestion = tracker.suggest(baseIntervalMs = 8_000, baseThreshold = 0.21)!!

        assertTrue("interval ${suggestion.intervalMs}", suggestion.intervalMs < 8_000)
        assertTrue("threshold ${suggestion.threshold}", suggestion.threshold < 0.21)
    }

    @Test
    fun aSlowerSpeakerGetsLongerIntervalsAndCoarserSensitivity() {
        val tracker = SpeechPaceTracker()
        repeat(4) { tracker.observe(chunk(words = 25).first, chunk(words = 25).second) }

        val suggestion = tracker.suggest(baseIntervalMs = 8_000, baseThreshold = 0.21)!!

        assertTrue("interval ${suggestion.intervalMs}", suggestion.intervalMs > 8_000)
        assertTrue("threshold ${suggestion.threshold}", suggestion.threshold > 0.21)
    }

    @Test
    fun suggestionsStayInsideUsableBounds() {
        val tracker = SpeechPaceTracker()
        // Absurdly fast, to prove an outlier cannot drive the interval to zero.
        repeat(6) { tracker.observe(chunk(words = 400).first, chunk(words = 400).second) }

        val suggestion = tracker.suggest(baseIntervalMs = 4_000, baseThreshold = 0.12)!!

        assertTrue(suggestion.intervalMs >= SpeechPaceTracker.MIN_INTERVAL_MS)
        assertTrue(suggestion.threshold >= SpeechPaceTracker.MIN_THRESHOLD)
    }

    @Test
    fun silenceLeavesThePaceAlone() {
        val tracker = SpeechPaceTracker()
        tracker.observe(chunk(50).first, chunk(50).second)
        val before = tracker.wordsPerMinute

        tracker.observe("   ", 20_000)

        assertEquals(before, tracker.wordsPerMinute)
    }

    @Test
    fun resetForgetsThePreviousLecture() {
        val tracker = SpeechPaceTracker()
        repeat(3) { tracker.observe(chunk(70).first, chunk(70).second) }

        tracker.reset()

        assertEquals(0, tracker.wordsPerMinute)
        assertNull(tracker.suggest(8_000, 0.21))
    }

    @Test
    fun repeatedSuggestionsDoNotCompound() {
        // A device run showed the interval walking to its ceiling while the threshold walked to
        // its floor, because each suggestion divided the previous one. Anchored to the session
        // baseline, the same pace always maps to the same settings however often it is asked.
        val tracker = SpeechPaceTracker()
        repeat(5) { tracker.observe(chunk(words = 25).first, chunk(words = 25).second) }

        val first = tracker.suggest(8_000, 0.21)!!
        val second = tracker.suggest(8_000, 0.21)!!
        val third = tracker.suggest(8_000, 0.21)!!

        assertEquals(first.intervalMs, second.intervalMs)
        assertEquals(first.intervalMs, third.intervalMs)
        assertEquals(first.threshold, third.threshold, 1e-9)
    }

    @Test
    fun bothKnobsAgreeAboutWhetherTheSpeakerIsFast() {
        val slow = SpeechPaceTracker()
        repeat(4) { slow.observe(chunk(words = 25).first, chunk(words = 25).second) }
        val slowSuggestion = slow.suggest(8_000, 0.21)!!

        val fast = SpeechPaceTracker()
        repeat(4) { fast.observe(chunk(words = 70).first, chunk(words = 70).second) }
        val fastSuggestion = fast.suggest(8_000, 0.21)!!

        // Slow: sample less often, need a bigger change. Fast: the opposite. Never a mix.
        assertTrue(slowSuggestion.intervalMs > fastSuggestion.intervalMs)
        assertTrue(slowSuggestion.threshold > fastSuggestion.threshold)
    }
}
