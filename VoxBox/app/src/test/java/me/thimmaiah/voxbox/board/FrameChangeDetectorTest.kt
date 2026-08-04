package me.thimmaiah.voxbox.board

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameChangeDetectorTest {
    @Test
    fun identicalFramesHaveNoChange() {
        val frame = IntArray(1_024) { if (it % 8 == 0) 20 else 235 }

        val result = compareLuma(frame, frame.copyOf())

        assertEquals(0.0, result.score, 0.000001)
        assertEquals(0.0, result.changedFraction, 0.000001)
    }

    @Test
    fun globalExposureShiftIsDiscounted() {
        val previous = IntArray(1_024) { if (it % 10 == 0) 40 else 180 }
        val brighter = IntArray(previous.size) { previous[it] + 25 }

        val result = compareLuma(previous, brighter)

        assertTrue(result.exposureShift > 0.09)
        assertTrue(result.score < 0.01)
    }

    @Test
    fun localHighContrastWritingProducesMeaningfulChange() {
        val previous = IntArray(1_024) { 235 }
        val current = previous.copyOf().also { values ->
            for (index in 350 until 550) values[index] = 20
        }

        val result = compareLuma(previous, current)

        assertTrue(result.changedFraction > 0.15)
        assertTrue(result.score > 0.10)
    }

    @Test
    fun fingerprintIsStableAndChangesWithLayout() {
        val first = IntArray(64) { if (it < 32) 10 else 240 }
        val second = IntArray(64) { if (it % 2 == 0) 10 else 240 }

        assertEquals(perceptualFingerprint(first), perceptualFingerprint(first.copyOf()))
        assertTrue(perceptualFingerprint(first) != perceptualFingerprint(second))
        assertEquals(16, perceptualFingerprint(first).length)
    }

    @Test
    fun discardedFirstFrameCanBeEvaluatedAgain() {
        val detector = FrameChangeDetector()
        val sample = IntArray(64) { if (it < 32) 20 else 235 }

        val failed = detector.evaluateSample(sample, threshold = 0.05)
        detector.discard(failed)
        val retry = detector.evaluateSample(sample, threshold = 0.05)

        assertTrue(failed.accepted)
        assertTrue(retry.accepted)
        assertTrue(detector.commit(retry))
        assertTrue(!detector.evaluateSample(sample.copyOf(), threshold = 0.05).accepted)
    }

    @Test
    fun failedChangedFrameDoesNotAdvanceCommittedBaseline() {
        val detector = FrameChangeDetector()
        val baseline = IntArray(64) { 235 }
        val writing = baseline.copyOf().also { values ->
            for (index in 16 until 48) values[index] = 20
        }
        val first = detector.evaluateSample(baseline, threshold = 0.05)
        assertTrue(detector.commit(first))

        val failedChange = detector.evaluateSample(writing, threshold = 0.05)
        assertTrue(failedChange.accepted)
        detector.discard(failedChange)

        val retry = detector.evaluateSample(writing.copyOf(), threshold = 0.05)
        assertTrue(retry.accepted)
        assertEquals(failedChange.fingerprint, retry.fingerprint)
    }
}
