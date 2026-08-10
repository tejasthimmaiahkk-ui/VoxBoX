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

        // The change has to settle before it is ever offered for upload.
        assertTrue(!detector.evaluateSample(writing, threshold = 0.05).accepted)
        val failedChange = detector.evaluateSample(writing.copyOf(), threshold = 0.05)
        assertTrue(failedChange.accepted)
        detector.discard(failedChange)

        // The baseline is still the blank board, so the same writing is offered again.
        assertTrue(!detector.evaluateSample(writing.copyOf(), threshold = 0.05).accepted)
        val retry = detector.evaluateSample(writing.copyOf(), threshold = 0.05)
        assertTrue(retry.accepted)
        assertEquals(failedChange.fingerprint, retry.fingerprint)
    }

    // --- settle gate: movement in front of the board must never reach the API ---

    /** A person occupying a different part of the frame on each sample. */
    private fun personAt(baseline: IntArray, start: Int): IntArray =
        baseline.copyOf().also { values ->
            for (index in start until minOf(start + 20, values.size)) values[index] = 30
        }

    @Test
    fun writingThatStaysPutIsAcceptedOnTheSecondFrame() {
        val detector = FrameChangeDetector()
        val baseline = IntArray(256) { 235 }
        val writing = baseline.copyOf().also { values ->
            for (index in 60 until 130) values[index] = 20
        }
        assertTrue(detector.commit(detector.evaluateSample(baseline, threshold = 0.05)))

        val first = detector.evaluateSample(writing, threshold = 0.05)
        val second = detector.evaluateSample(writing.copyOf(), threshold = 0.05)

        assertTrue(!first.accepted)
        assertTrue(first.reason.contains("settled"))
        assertTrue(second.accepted)
    }

    @Test
    fun personMovingThroughFrameIsNeverAccepted() {
        val detector = FrameChangeDetector()
        val baseline = IntArray(256) { 235 }
        assertTrue(detector.commit(detector.evaluateSample(baseline, threshold = 0.05)))

        // Walks across the board over four samples, then leaves.
        val walk = listOf(40, 80, 120, 160).map { personAt(baseline, it) }
        walk.forEach { frame ->
            assertTrue(!detector.evaluateSample(frame, threshold = 0.05).accepted)
        }
        val afterLeaving = detector.evaluateSample(baseline.copyOf(), threshold = 0.05)

        assertTrue(!afterLeaving.accepted)
        assertEquals("Frame is similar to the last committed board state.", afterLeaving.reason)
    }

    @Test
    fun writingRevealedAfterSomeoneWalksAwayIsStillCaptured() {
        val detector = FrameChangeDetector()
        val baseline = IntArray(256) { 235 }
        val writing = baseline.copyOf().also { values ->
            for (index in 60 until 130) values[index] = 20
        }
        assertTrue(detector.commit(detector.evaluateSample(baseline, threshold = 0.05)))

        // Someone stands in front of the board while writing, then steps away.
        detector.evaluateSample(personAt(writing, 150), threshold = 0.05)
        detector.evaluateSample(personAt(writing, 190), threshold = 0.05)
        val revealed = detector.evaluateSample(writing.copyOf(), threshold = 0.05)
        val confirmed = detector.evaluateSample(writing.copyOf(), threshold = 0.05)

        assertTrue(!revealed.accepted)
        assertTrue(confirmed.accepted)
        assertTrue(detector.commit(confirmed))
    }

    @Test
    fun projectorSlideChangeSettlesAndIsAccepted() {
        val detector = FrameChangeDetector()
        val slideOne = IntArray(256) { if (it % 3 == 0) 30 else 220 }
        val slideTwo = IntArray(256) { if (it % 5 == 0) 25 else 215 }
        assertTrue(detector.commit(detector.evaluateSample(slideOne, threshold = 0.05)))

        assertTrue(!detector.evaluateSample(slideTwo, threshold = 0.05).accepted)
        val settled = detector.evaluateSample(slideTwo.copyOf(), threshold = 0.05)

        assertTrue(settled.accepted)
    }

    @Test
    fun resetClearsAnUnsettledChange() {
        val detector = FrameChangeDetector()
        val baseline = IntArray(256) { 235 }
        val writing = baseline.copyOf().also { values ->
            for (index in 60 until 130) values[index] = 20
        }
        assertTrue(detector.commit(detector.evaluateSample(baseline, threshold = 0.05)))
        detector.evaluateSample(writing, threshold = 0.05)

        detector.reset()

        // After a reset the next frame is a fresh baseline, not a settled change.
        val afterReset = detector.evaluateSample(writing.copyOf(), threshold = 0.05)
        assertEquals("First frame establishes the board baseline.", afterReset.reason)
    }
}
