package me.thimmaiah.voxbox.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioPipelineTest {
    @Test
    fun wavEncoderWritesCanonicalPcmHeader() {
        val pcm = ByteArray(32_000)

        val wav = encodePcm16MonoWav(pcm, sampleRate = 16_000)

        assertEquals("RIFF", wav.copyOfRange(0, 4).toString(Charsets.US_ASCII))
        assertEquals("WAVE", wav.copyOfRange(8, 12).toString(Charsets.US_ASCII))
        assertEquals("data", wav.copyOfRange(36, 40).toString(Charsets.US_ASCII))
        assertEquals(32_000, ByteBuffer.wrap(wav, 40, 4).order(ByteOrder.LITTLE_ENDIAN).int)
        assertEquals(1_000, pcmDurationMs(pcm.size, 16_000))
    }

    @Test
    fun finalUsefulPartialPcmIsEmittedOnceWithContinuousOffsets() {
        var nextId = 0
        val accumulator = PcmChunkAccumulator(
            targetBytes = 8,
            minimumUsefulBytes = 4,
            sampleRate = 1_000,
            idFactory = { "chunk-${++nextId}" },
        )

        val full = accumulator.append(ByteArray(12) { it.toByte() }).single()
        val partial = accumulator.finish()

        assertEquals("chunk-1", full.id)
        assertEquals(0L, full.offsetMs)
        assertEquals(4L, full.durationMs)
        assertEquals(8, ByteBuffer.wrap(full.wavBytes, 40, 4).order(ByteOrder.LITTLE_ENDIAN).int)
        requireNotNull(partial)
        assertEquals("chunk-2", partial.id)
        assertEquals(4L, partial.offsetMs)
        assertEquals(2L, partial.durationMs)
        assertEquals(4, ByteBuffer.wrap(partial.wavBytes, 40, 4).order(ByteOrder.LITTLE_ENDIAN).int)
        assertNull(accumulator.finish())
    }

    @Test
    fun dominantSpeakerIsSelectedOnlyInsideCurrentChunk() {
        val tracker = PerChunkSpeakerTracker()
        val first = tracker.evaluate(
            "chunk-1",
            listOf(
                segment("A", 0, 21_000, "t1"),
                segment("B", 21_000, 27_000, "s1"),
            ),
        )

        assertEquals(SpeakerFocusStatus.FOCUSED, first.status)
        assertEquals("A", first.selectedSpeakerId)
        assertTrue(first.leadingShare > 0.7)

        val second = tracker.evaluate(
            "chunk-2",
            listOf(
                segment("A", 0, 2_000, "a2"),
                segment("B", 2_000, 20_000, "b2"),
            ),
        )
        assertEquals(SpeakerFocusStatus.FOCUSED, second.status)
        assertEquals("B", second.selectedSpeakerId)
        assertEquals(20_000L, second.observedVoicedMs)
    }

    @Test
    fun manualChoiceIsChunkLocalAndSegmentsAreIdempotent() {
        val tracker = PerChunkSpeakerTracker()
        val teacher = segment("A", 0, 100_000, "same-id")
        val ambiguous = tracker.evaluate(
            "chunk-1",
            listOf(teacher, teacher, segment("B", 100_000, 190_000, "b")),
        )

        assertEquals(SpeakerFocusStatus.AMBIGUOUS, ambiguous.status)
        assertEquals(190_000L, ambiguous.observedVoicedMs)

        val manual = tracker.selectManually("A")
        assertEquals(SpeakerFocusStatus.MANUAL, manual.status)
        assertEquals("A", manual.selectedSpeakerId)

        val next = tracker.evaluate("chunk-2", listOf(segment("B", 0, 10_000, "next")))
        assertEquals(SpeakerFocusStatus.FOCUSED, next.status)
        assertEquals("B", next.selectedSpeakerId)
    }

    private fun segment(speaker: String, start: Long, end: Long, id: String) = TranscribedSegment(
        id = id,
        speakerId = speaker,
        startMs = start,
        endMs = end,
        text = "text",
    )
}
